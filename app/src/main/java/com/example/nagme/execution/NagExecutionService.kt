package com.example.nagme.execution

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.nagme.data.Nag
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.NagUtterance
import com.example.nagme.data.NagUtteranceDao
import com.example.nagme.data.OccurrenceStatus
import com.example.nagme.data.RoomNagRepository
import com.example.nagme.data.UtteranceSource
import com.example.nagme.logic.NagEngine
import com.example.nagme.network.NagMeApiClient
import com.example.nagme.notification.NagNotificationManager
import com.example.nagme.scheduler.NagScheduler
import com.example.nagme.settings.UserSettingsRepository
import com.example.nagme.speech.SpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class NagExecutionService : Service() {

    private val serviceJob: Job =
        SupervisorJob()

    private val serviceScope =
        CoroutineScope(
            serviceJob + Dispatchers.IO
        )

    private lateinit var repository:
            RoomNagRepository

    private lateinit var engine:
            NagEngine

    private lateinit var scheduler:
            NagScheduler

    private lateinit var speechManager:
            SpeechManager

    private lateinit var settingsRepository:
            UserSettingsRepository

    private lateinit var nagNotificationManager:
            NagNotificationManager

    private lateinit var nagUtteranceDao:
            NagUtteranceDao

    private val speechQueue =
        ArrayDeque<SpeechItem>()

    private var processingSpeech =
        false

    private var executionInProgress =
        false

    private var currentStartId =
        0

    override fun onCreate() {
        super.onCreate()

        val database =
            NagDatabase.getInstance(
                applicationContext
            )

        nagUtteranceDao =
            database.nagUtteranceDao()

        repository =
            RoomNagRepository(
                database
            )

        engine =
            NagEngine(
                repository
            )

        scheduler =
            NagScheduler(
                context =
                    applicationContext,
                repository =
                    repository
            )

        settingsRepository =
            UserSettingsRepository(
                applicationContext
            )

        speechManager =
            SpeechManager(
                context =
                    applicationContext,
                settingsRepository =
                    settingsRepository
            )

        nagNotificationManager =
            NagNotificationManager(
                applicationContext
            )

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification(
                "Preparing reminder..."
            )
        )

        Log.d(
            TAG,
            "NagExecutionService created"
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        currentStartId =
            startId

        Log.d(
            TAG,
            "Service start received. " +
                    "startId=$startId flags=$flags"
        )

        if (!executionInProgress) {

            executionInProgress =
                true

            serviceScope.launch {

                try {

                    processDueOccurrences(
                        startId =
                            startId
                    )

                } catch (
                    e: Exception
                ) {

                    Log.e(
                        TAG,
                        "Nag execution failed",
                        e
                    )

                    try {

                        scheduler.reschedule()

                    } catch (
                        schedulerError:
                        Exception
                    ) {

                        Log.e(
                            TAG,
                            "Reschedule after failure failed",
                            schedulerError
                        )
                    }

                    stopServiceCleanly(
                        startId =
                            startId
                    )
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    private suspend fun processDueOccurrences(
        startId: Int
    ) {

        val now =
            System.currentTimeMillis()

        val dueOccurrences =
            repository
                .getAllDueOccurrences(
                    now
                )

        Log.d(
            TAG,
            "Found ${dueOccurrences.size} due occurrence(s)"
        )

        if (
            dueOccurrences.isEmpty()
        ) {

            scheduler.reschedule()

            stopServiceCleanly(
                startId =
                    startId
            )

            return
        }

        val showNagNotifications =
            settingsRepository
                .settings
                .first()
                .showNagNotifications

        for (
        occurrence in
        dueOccurrences
        ) {

            val nag =
                repository.getNag(
                    occurrence.nagId
                )

            if (
                nag == null
            ) {

                Log.w(
                    TAG,
                    "Nag ${occurrence.nagId} not found"
                )

                continue
            }

            if (
                !nag.enabled
            ) {

                Log.d(
                    TAG,
                    "Skipping disabled Nag ${nag.id}"
                )

                nagNotificationManager
                    .cancel(
                        nag.id
                    )

                continue
            }

            val activeOccurrence =
                if (
                    occurrence.startedAt ==
                    null
                ) {

                    engine
                        .startDueOccurrence(
                            nag = nag,
                            occurrence =
                                occurrence,
                            now = now
                        )

                } else {

                    occurrence
                }

            speechQueue.addLast(
                SpeechItem(
                    nag = nag,
                    occurrence =
                        activeOccurrence,
                    showNotification =
                        showNagNotifications
                )
            )
        }

        if (
            speechQueue.isEmpty()
        ) {

            scheduler.reschedule()

            stopServiceCleanly(
                startId =
                    startId
            )

            return
        }

        speakNext(
            startId =
                startId
        )
    }

    private fun speakNext(
        startId: Int
    ) {

        if (
            processingSpeech
        ) {
            return
        }

        val item =
            speechQueue
                .firstOrNull()
                ?: run {

                    finishExecution(
                        startId =
                            startId
                    )

                    return
                }

        processingSpeech =
            true

        /*
         * Resolve the text to speak.
         *
         * AI is opt-in per Nag.
         *
         * AI OFF:
         * - do not call the cloud API
         * - speak the locally stored original text
         * - record the utterance as ORIGINAL
         *
         * AI ON:
         * - use the existing cloud/AI path
         * - use the Nag's selected personality
         * - any failure, timeout or invalid response falls
         *   back to the original locally stored text
         */
        serviceScope.launch {

            var spokenText =
                item.nag.text

            var utteranceSource =
                UtteranceSource.ORIGINAL

            if (
                item.nag.aiEnabled
            ) {

                /*
                 * AI was explicitly requested.
                 *
                 * From this point until AI succeeds,
                 * original local text is the fallback.
                 */
                utteranceSource =
                    UtteranceSource.FALLBACK

                try {

                    val previousUtterances =
                        nagUtteranceDao
                            .getForOccurrence(
                                item.occurrence.id
                            )
                            .map {
                                it.spokenText
                            }
                            .takeLast(5)

                    val apiResult =
                        withTimeout(
                            AI_REQUEST_TIMEOUT_MS
                        ) {

                            NagMeApiClient.requestNagText(
                                nagId =
                                    item.nag.id,
                                occurrenceId =
                                    item.occurrence.id,
                                nagNumber =
                                    item.occurrence.nagCount + 1,
                                originalText =
                                    item.nag.text,
                                personality =
                                    item.nag.personality,
                                previousUtterances =
                                    previousUtterances
                            )
                        }

                    if (
                        apiResult.success &&
                        !apiResult.text.isNullOrBlank() &&
                        apiResult.source == "AI"
                    ) {

                        spokenText =
                            apiResult.text

                        utteranceSource =
                            UtteranceSource.AI

                        Log.d(
                            "NagMeApi",
                            "Nag ${item.nag.id} AI SUCCESS " +
                                    "personality=${item.nag.personality}: $spokenText"
                        )

                    } else {

                        Log.w(
                            "NagMeApi",
                            "Nag ${item.nag.id} returned no valid AI text; using fallback"
                        )
                    }

                } catch (
                    error: Exception
                ) {

                    Log.w(
                        "NagMeApi",
                        "Nag ${item.nag.id} AI FAILED; using original fallback",
                        error
                    )
                }

            } else {

                /*
                 * AI is disabled for this Nag.
                 *
                 * Deliberately do not call NagMeApiClient.
                 */
                Log.d(
                    "NagMeApi",
                    "Nag ${item.nag.id} AI disabled; using original text"
                )
            }

            /*
             * From this point on, spokenText is the exact text
             * NagMe will say and later store in nag_utterances.
             */

            Log.d(
                TAG,
                "Speaking Nag ${item.nag.id} " +
                        "source=$utteranceSource: $spokenText"
            )

            if (
                item.showNotification
            ) {

                /*
                 * Keep the Nag/occurrence identity unchanged.
                 *
                 * Use the actual spoken text for the notification
                 * while leaving the original Nag stored in Room.
                 */
                nagNotificationManager
                    .show(
                        nag =
                            item.nag.copy(
                                text =
                                    spokenText
                            ),
                        occurrence =
                            item.occurrence.copy(
                                nagCount =
                                    item.occurrence
                                        .nagCount + 1
                            )
                    )

            } else {

                nagNotificationManager
                    .cancel(
                        item.nag.id
                    )
            }

            speechManager.speakNag(
                text =
                    spokenText
            ) {

                serviceScope.launch {

                    try {

                        speechQueue
                            .removeFirstOrNull()

                        val spokenAt =
                            System.currentTimeMillis()

                        /*
                         * Store exactly what was spoken and where
                         * that text came from.
                         *
                         * ORIGINAL = AI was disabled and normal
                         *            local NagMe wording was used.
                         *
                         * AI       = Gemini-generated text.
                         *
                         * FALLBACK = AI was requested, but original
                         *            local text was used because
                         *            AI/cloud was unavailable or invalid.
                         */
                        try {

                            nagUtteranceDao.insert(
                                NagUtterance(
                                    occurrenceId =
                                        item.occurrence.id,
                                    nagNumber =
                                        item.occurrence.nagCount + 1,
                                    spokenText =
                                        spokenText,
                                    source =
                                        utteranceSource,
                                    spokenAt =
                                        spokenAt
                                )
                            )

                        } catch (
                            utteranceError: Exception
                        ) {

                            Log.e(
                                TAG,
                                "Failed to record Nag utterance history",
                                utteranceError
                            )
                        }

                        /*
                         * Re-read the Nag before calculating the
                         * next cycle because the user may have
                         * changed its settings while speech ran.
                         */
                        val latestNag =
                            repository.getNag(
                                item.nag.id
                            ) ?: item.nag

                        val updatedOccurrence =
                            engine
                                .recordNagSpoken(
                                    nag =
                                        latestNag,
                                    occurrence =
                                        item.occurrence,
                                    spokenAt =
                                        spokenAt
                                )

                        Log.d(
                            TAG,
                            "Recorded spoken Nag ${item.nag.id} " +
                                    "source=$utteranceSource at $spokenAt"
                        )

                        if (
                            updatedOccurrence.status ==
                            OccurrenceStatus.EXPIRED
                        ) {

                            nagNotificationManager
                                .cancel(
                                    item.nag.id
                                )
                        }

                        processingSpeech =
                            false

                        if (
                            speechQueue
                                .isNotEmpty()
                        ) {

                            speakNext(
                                startId =
                                    startId
                            )

                        } else {

                            scheduler
                                .reschedule()

                            Log.d(
                                TAG,
                                "Nag execution complete; next alarm rescheduled"
                            )

                            stopServiceCleanly(
                                startId =
                                    startId
                            )
                        }

                    } catch (
                        e: Exception
                    ) {

                        Log.e(
                            TAG,
                            "Failed after Nag speech",
                            e
                        )

                        processingSpeech =
                            false

                        try {

                            scheduler
                                .reschedule()

                        } catch (
                            schedulerError:
                            Exception
                        ) {

                            Log.e(
                                TAG,
                                "Recovery reschedule failed",
                                schedulerError
                            )
                        }

                        stopServiceCleanly(
                            startId =
                                startId
                        )
                    }
                }
            }
        }
    }

    private fun finishExecution(
        startId: Int
    ) {

        serviceScope.launch {

            try {

                scheduler.reschedule()

            } finally {

                stopServiceCleanly(
                    startId =
                        startId
                )
            }
        }
    }

    private fun stopServiceCleanly(
        startId: Int
    ) {

        Log.d(
            TAG,
            "Stopping NagExecutionService. startId=$startId"
        )

        executionInProgress =
            false

        processingSpeech =
            false

        speechManager.shutdown()

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        stopSelf(
            startId
        )
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "NagExecutionService destroyed"
        )

        serviceJob.cancel()

        if (
            ::speechManager
                .isInitialized
        ) {

            speechManager.shutdown()
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? =
        null

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "NagMe execution",
                    NotificationManager
                        .IMPORTANCE_LOW
                ).apply {

                    description =
                        "Keeps NagMe running while a scheduled reminder is prepared and spoken."

                    setSound(
                        null,
                        null
                    )

                    enableVibration(
                        false
                    )

                    setShowBadge(
                        false
                    )
                }

            getSystemService(
                NotificationManager::class.java
            )
                .createNotificationChannel(
                    channel
                )
        }
    }

    private fun createNotification(
        text: String
    ): Notification {

        val builder =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                Notification.Builder(
                    this,
                    CHANNEL_ID
                )

            } else {

                @Suppress(
                    "DEPRECATION"
                )
                Notification.Builder(
                    this
                )
            }

        return builder
            .setSmallIcon(
                android.R.drawable
                    .ic_lock_idle_alarm
            )
            .setContentTitle(
                "NagMe"
            )
            .setContentText(
                text
            )
            .setOngoing(
                true
            )
            .setOnlyAlertOnce(
                true
            )
            .setPriority(
                Notification.PRIORITY_LOW
            )
            .setSound(
                null
            )
            .setVibrate(
                null
            )
            .build()
    }

    private data class SpeechItem(
        val nag: Nag,
        val occurrence: NagOccurrence,
        val showNotification: Boolean
    )

    companion object {

        private const val TAG =
            "NagMe"

        private const val CHANNEL_ID =
            "nagme_execution"

        private const val NOTIFICATION_ID =
            2001

        /*
         * Don't allow cloud/AI latency to hold a Nag indefinitely.
         * On timeout, the existing local text is spoken.
         */
        private const val AI_REQUEST_TIMEOUT_MS =
            8_000L
    }
}
