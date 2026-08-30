package com.example.nagme.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.nagme.settings.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpeechManager(
    context: Context,
    private val settingsRepository: UserSettingsRepository
) {

    private val appContext =
        context.applicationContext

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    private val audioManager =
        appContext.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

    /*
     * Treat NagMe speech like normal media speech.
     *
     * This lets Android use the active media route:
     * Bluetooth/headphones when active, otherwise the phone.
     */
    private val speechAudioAttributes =
        AudioAttributes.Builder()
            .setUsage(
                AudioAttributes.USAGE_MEDIA
            )
            .setContentType(
                AudioAttributes.CONTENT_TYPE_SPEECH
            )
            .build()

    private var audioFocusRequest:
            AudioFocusRequest? = null

    private val audioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener {
            // For V1 we don't need to react manually.
            // Android handles the temporary focus/ducking behaviour.
        }

    private var tts: TextToSpeech? = null

    private val queue =
        ArrayDeque<SpeechRequest>()

    private var speaking = false
    private var initialising = false

    /*
     * Diagnostic only.
     *
     * Used to measure how long the TTS engine takes
     * to initialise. Does not affect TTS behaviour.
     */
    private var ttsInitialisationStartedAt:
            Long? = null

    fun speakNag(
        text: String,
        onComplete: () -> Unit
    ) {

        Log.d(
            "NagMeTiming",
            "SPEAK_NAG_RECEIVED " +
                    "ttsExists=${tts != null} " +
                    "initialising=$initialising " +
                    "t=${SystemClock.elapsedRealtime()}"
        )

        scope.launch {

            val settings =
                settingsRepository.settings.first()

            val spokenText =
                if (
                    settings.userName.isBlank()
                ) {
                    text
                } else {
                    "${settings.userName}, $text."
                }

            synchronized(queue) {

                queue.addLast(
                    SpeechRequest(
                        spokenText =
                            spokenText,
                        preferredVoiceName =
                            settings.defaultVoiceName,
                        onComplete =
                            onComplete
                    )
                )
            }

            ensureTtsReady()
        }
    }

    private fun ensureTtsReady() {

        synchronized(this) {

            if (tts != null) {
                speakNext()
                return
            }

            if (initialising) {
                return
            }

            initialising = true

            ttsInitialisationStartedAt =
                SystemClock.elapsedRealtime()

            Log.d(
                "NagMeTiming",
                "TTS_INIT_START " +
                        "t=$ttsInitialisationStartedAt"
            )

            initialiseGoogleTts()
        }
    }

    private fun initialiseGoogleTts() {

        tts = TextToSpeech(
            appContext,
            { status ->

                initialising = false

                val ttsReadyAt =
                    SystemClock.elapsedRealtime()

                Log.d(
                    "NagMeTiming",
                    "TTS_INIT_RESULT status=$status " +
                            "durationMs=" +
                            (
                                    ttsInitialisationStartedAt
                                        ?.let {
                                            ttsReadyAt - it
                                        }
                                        ?: -1
                                    ) +
                            " t=$ttsReadyAt"
                )

                if (
                    status ==
                    TextToSpeech.SUCCESS
                ) {

                    configureTts()
                    speakNext()

                } else {

                    tts?.shutdown()
                    tts = null

                    initialiseFallbackTts()
                }
            },
            GOOGLE_TTS_ENGINE
        )
    }

    private fun initialiseFallbackTts() {

        initialising = true

        Log.d(
            "NagMeTiming",
            "TTS_FALLBACK_INIT_START " +
                    "t=${SystemClock.elapsedRealtime()}"
        )

        tts =
            TextToSpeech(
                appContext
            ) { status ->

                initialising = false

                Log.d(
                    "NagMeTiming",
                    "TTS_FALLBACK_INIT_RESULT " +
                            "status=$status " +
                            "t=${SystemClock.elapsedRealtime()}"
                )

                if (
                    status ==
                    TextToSpeech.SUCCESS
                ) {

                    configureTts()
                    speakNext()

                } else {

                    failPendingSpeech()
                }
            }
    }

    private fun configureTts() {

        val engine =
            tts ?: return

        engine.setAudioAttributes(
            speechAudioAttributes
        )

        engine.setOnUtteranceProgressListener(
            object :
                UtteranceProgressListener() {

                override fun onStart(
                    utteranceId: String?
                ) {

                    Log.d(
                        "NagMeTiming",
                        "TTS_AUDIO_STARTED " +
                                "utteranceId=$utteranceId " +
                                "t=${SystemClock.elapsedRealtime()}"
                    )
                }

                override fun onDone(
                    utteranceId: String?
                ) {

                    Log.d(
                        "NagMeTiming",
                        "TTS_AUDIO_DONE " +
                                "utteranceId=$utteranceId " +
                                "t=${SystemClock.elapsedRealtime()}"
                    )

                    finishCurrentSpeech()
                }

                @Deprecated(
                    "Deprecated by Android"
                )
                override fun onError(
                    utteranceId: String?
                ) {

                    Log.d(
                        "NagMeTiming",
                        "TTS_AUDIO_ERROR " +
                                "utteranceId=$utteranceId " +
                                "t=${SystemClock.elapsedRealtime()}"
                    )

                    finishCurrentSpeech()
                }
            }
        )
    }

    private fun speakNext() {

        synchronized(this) {

            if (speaking) return

            val request =
                synchronized(queue) {
                    queue.firstOrNull()
                } ?: return

            val engine =
                tts ?: return

            applyPreferredVoice(
                engine = engine,
                preferredVoiceName =
                    request.preferredVoiceName
            )

            /*
             * Ask Android for short-lived focus and explicitly
             * allow existing media to duck rather than requiring
             * it to stop completely.
             */
            Log.d(
                "NagMeTiming",
                "AUDIO_FOCUS_REQUEST " +
                        "t=${SystemClock.elapsedRealtime()}"
            )

            requestAudioFocus()

            speaking = true

            Log.d(
                "NagMeTiming",
                "TTS_SPEAK_CALL " +
                        "t=${SystemClock.elapsedRealtime()}"
            )

            engine.speak(
                request.spokenText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "nagme_${System.nanoTime()}"
            )
        }
    }

    private fun requestAudioFocus() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val request =
                AudioFocusRequest.Builder(
                    AudioManager
                        .AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                    .setAudioAttributes(
                        speechAudioAttributes
                    )
                    .setOnAudioFocusChangeListener(
                        audioFocusChangeListener
                    )
                    .build()

            audioFocusRequest =
                request

            audioManager.requestAudioFocus(
                request
            )

        } else {

            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager
                    .AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            audioFocusRequest?.let {

                audioManager
                    .abandonAudioFocusRequest(
                        it
                    )
            }

            audioFocusRequest = null

        } else {

            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(
                audioFocusChangeListener
            )
        }
    }

    private fun applyPreferredVoice(
        engine: TextToSpeech,
        preferredVoiceName: String?
    ) {

        if (
            preferredVoiceName
                .isNullOrBlank()
        ) {
            return
        }

        val preferredVoice =
            engine.voices
                ?.firstOrNull {
                    it.name ==
                            preferredVoiceName
                }

        if (
            preferredVoice != null
        ) {
            engine.voice =
                preferredVoice
        }
    }

    private fun finishCurrentSpeech() {

        abandonAudioFocus()

        val completed =
            synchronized(queue) {
                queue.removeFirstOrNull()
            }

        speaking = false

        completed
            ?.onComplete
            ?.invoke()

        speakNext()
    }

    private fun failPendingSpeech() {

        abandonAudioFocus()

        val pending =
            synchronized(queue) {

                val items =
                    queue.toList()

                queue.clear()

                items
            }

        speaking = false

        pending.forEach {
            it.onComplete.invoke()
        }
    }

    fun shutdown() {

        abandonAudioFocus()

        scope.coroutineContext[
            Job
        ]?.cancel()

        tts?.stop()
        tts?.shutdown()
        tts = null

        synchronized(queue) {
            queue.clear()
        }

        speaking = false
        initialising = false
    }

    private data class SpeechRequest(
        val spokenText: String,
        val preferredVoiceName: String?,
        val onComplete: () -> Unit
    )

    companion object {

        private const val
                GOOGLE_TTS_ENGINE =
            "com.google.android.tts"
    }
}