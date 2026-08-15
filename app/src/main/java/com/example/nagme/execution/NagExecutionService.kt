package com.example.nagme.execution

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.nagme.data.Nag
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.NagOccurrence
import com.example.nagme.data.OccurrenceStatus
import com.example.nagme.data.RoomNagRepository
import com.example.nagme.logic.NagEngine
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
                "Processing due Nag..."
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

                    /*
                     * The occurrence remains in Room.
                     *
                     * Try to restore the next Android
                     * wake-up rather than leaving the
                     * database state stranded.
                     */
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

        /*
         * If Android kills this service before we call
         * stopSelf(startId), Android may recreate it and
         * redeliver the most recent Intent.
         */
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

        /*
         * Read the user's notification preference once for
         * this service execution.
         *
         * Speech is always processed regardless of this value.
         */
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

        Log.d(
            TAG,
            "Speaking Nag ${item.nag.id}: ${item.nag.text}"
        )

        /*
         * This is the existing foreground-service
         * notification required while speech is processed.
         */
        updateForegroundNotification(
            item.nag.text
        )

        /*
         * User-facing Nag notification.
         *
         * We show the count that this speech is about to become,
         * so the first spoken Nag displays as 1 rather than 0.
         */
        if (
            item.showNotification
        ) {

            nagNotificationManager
                .show(
                    nag =
                        item.nag,
                    occurrence =
                        item.occurrence.copy(
                            nagCount =
                                item.occurrence
                                    .nagCount + 1
                        )
                )

        } else {

            /*
             * If the user has since disabled visual
             * notifications, remove any existing one.
             */
            nagNotificationManager
                .cancel(
                    item.nag.id
                )
        }

        speechManager.speakNag(
            text =
                item.nag.text
        ) {

            serviceScope.launch {

                try {

                    speechQueue
                        .removeFirstOrNull()

                    val spokenAt =
                        System.currentTimeMillis()

                    /*
                     * Re-read the Nag from Room before calculating
                     * the next cycle.
                     *
                     * The user may have changed Max Nags while
                     * this speech was in progress, so item.nag
                     * may contain stale settings.
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
                        "Recorded spoken Nag ${item.nag.id} at $spokenAt"
                    )

                    /*
                     * Max nags, maximum duration, or a
                     * single-speech occurrence may finish here.
                     *
                     * In that case the user-facing notification
                     * must disappear immediately.
                     */
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

        /*
         * Important with START_REDELIVER_INTENT:
         * tell Android this particular start has
         * completed successfully.
         */
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
                    "NagMe active nags",
                    NotificationManager
                        .IMPORTANCE_HIGH
                ).apply {

                    description =
                        "Shown while NagMe is speaking a scheduled nag."
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
            .build()
    }

    private fun updateForegroundNotification(
        text: String
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat
                .checkSelfPermission(
                    this,
                    Manifest.permission
                        .POST_NOTIFICATIONS
                ) !=
            PackageManager
                .PERMISSION_GRANTED
        ) {

            return
        }

        getSystemService(
            NotificationManager::class.java
        )
            .notify(
                NOTIFICATION_ID,
                createNotification(
                    text
                )
            )
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
            "nagme_active_nag"

        private const val NOTIFICATION_ID =
            2001
    }
}