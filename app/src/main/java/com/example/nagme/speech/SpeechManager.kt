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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpeechManager(
    context: Context,
    private val settingsRepository: UserSettingsRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val speechAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { }

    private var tts: TextToSpeech? = null
    private val queue = ArrayDeque<SpeechRequest>()
    private var speaking = false
    private var initialising = false
    private var ttsInitialisationStartedAt: Long? = null

    fun speakNag(
        text: String,
        onComplete: () -> Unit
    ) {
        Log.d(
            TAG_TIMING,
            "SPEAK_NAG_RECEIVED ttsExists=${tts != null} initialising=$initialising t=${SystemClock.elapsedRealtime()}"
        )

        scope.launch {
            val settings = settingsRepository.settings.first()
            val spokenText = if (settings.userName.isBlank()) text else "${settings.userName}, $text."

            synchronized(queue) {
                queue.addLast(
                    SpeechRequest(
                        spokenText = spokenText,
                        preferredVoiceName = settings.defaultVoiceName,
                        onComplete = onComplete
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
            if (initialising) return

            initialising = true
            ttsInitialisationStartedAt = SystemClock.elapsedRealtime()
            Log.d(TAG_TIMING, "TTS_INIT_START t=$ttsInitialisationStartedAt")
            initialiseGoogleTts()
        }
    }

    private fun initialiseGoogleTts() {
        tts = TextToSpeech(
            appContext,
            { status ->
                initialising = false
                val readyAt = SystemClock.elapsedRealtime()
                Log.d(
                    TAG_TIMING,
                    "TTS_INIT_RESULT status=$status durationMs=${ttsInitialisationStartedAt?.let { readyAt - it } ?: -1} t=$readyAt"
                )

                if (status == TextToSpeech.SUCCESS) {
                    configureTts()
                    speakNext()
                } else {
                    shutdownEngineOnly()
                    initialiseFallbackTts()
                }
            },
            GOOGLE_TTS_ENGINE
        )
    }

    private fun initialiseFallbackTts() {
        initialising = true
        Log.d(TAG_TIMING, "TTS_FALLBACK_INIT_START t=${SystemClock.elapsedRealtime()}")

        tts = TextToSpeech(appContext) { status ->
            initialising = false
            Log.d(TAG_TIMING, "TTS_FALLBACK_INIT_RESULT status=$status t=${SystemClock.elapsedRealtime()}")

            if (status == TextToSpeech.SUCCESS) {
                configureTts()
                speakNext()
            } else {
                retryCurrentSpeech("TTS initialisation failed")
            }
        }
    }

    private fun configureTts() {
        val engine = tts ?: return
        engine.setAudioAttributes(speechAudioAttributes)
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG_TIMING, "TTS_AUDIO_STARTED utteranceId=$utteranceId t=${SystemClock.elapsedRealtime()}")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG_TIMING, "TTS_AUDIO_DONE utteranceId=$utteranceId t=${SystemClock.elapsedRealtime()}")
                    finishCurrentSpeechSuccess()
                }

                @Deprecated("Deprecated by Android")
                override fun onError(utteranceId: String?) {
                    Log.e(TAG_TIMING, "TTS_AUDIO_ERROR utteranceId=$utteranceId t=${SystemClock.elapsedRealtime()}")
                    retryCurrentSpeech("TTS onError callback")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(
                        TAG_TIMING,
                        "TTS_AUDIO_ERROR utteranceId=$utteranceId errorCode=$errorCode t=${SystemClock.elapsedRealtime()}"
                    )
                    retryCurrentSpeech("TTS onError code=$errorCode")
                }
            }
        )
    }

    private fun speakNext() {
        synchronized(this) {
            if (speaking) return

            val request = synchronized(queue) { queue.firstOrNull() } ?: return
            val engine = tts ?: return

            applyPreferredVoice(engine, request.preferredVoiceName)

            Log.d(TAG_TIMING, "AUDIO_FOCUS_REQUEST t=${SystemClock.elapsedRealtime()}")
            val focusResult = requestAudioFocus()
            Log.d(TAG_TIMING, "AUDIO_FOCUS_RESULT result=$focusResult t=${SystemClock.elapsedRealtime()}")

            speaking = true
            val utteranceId = "nagme_${System.nanoTime()}"
            Log.d(
                TAG_TIMING,
                "TTS_SPEAK_CALL utteranceId=$utteranceId attempt=${request.attempt + 1} t=${SystemClock.elapsedRealtime()}"
            )

            val speakResult = engine.speak(
                request.spokenText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            )

            Log.d(TAG_TIMING, "TTS_SPEAK_RESULT utteranceId=$utteranceId result=$speakResult")

            if (speakResult == TextToSpeech.ERROR) {
                retryCurrentSpeech("TextToSpeech.speak returned ERROR")
            }
        }
    }

    private fun retryCurrentSpeech(reason: String) {
        synchronized(this) {
            abandonAudioFocus()
            speaking = false

            val request = synchronized(queue) { queue.firstOrNull() } ?: return
            request.attempt += 1

            Log.w(
                TAG_TIMING,
                "TTS_RETRY reason=$reason attempt=${request.attempt} t=${SystemClock.elapsedRealtime()}"
            )

            shutdownEngineOnly()
            initialising = false

            scope.launch {
                delay(TTS_RETRY_DELAY_MS)
                ensureTtsReady()
            }
        }
    }

    private fun finishCurrentSpeechSuccess() {
        val completed: SpeechRequest?
        synchronized(this) {
            if (!speaking) return
            abandonAudioFocus()
            completed = synchronized(queue) { queue.removeFirstOrNull() }
            speaking = false
        }

        // This is deliberately the ONLY place that reports completion.
        // TTS errors never mark a Nag as spoken or advance its schedule.
        completed?.onComplete?.invoke()
        speakNext()
    }

    private fun requestAudioFocus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(speechAudioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun applyPreferredVoice(engine: TextToSpeech, preferredVoiceName: String?) {
        if (preferredVoiceName.isNullOrBlank()) return
        engine.voices?.firstOrNull { it.name == preferredVoiceName }?.let { engine.voice = it }
    }

    private fun shutdownEngineOnly() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    fun shutdown() {
        abandonAudioFocus()
        scope.coroutineContext[Job]?.cancel()
        shutdownEngineOnly()
        synchronized(queue) { queue.clear() }
        speaking = false
        initialising = false
    }

    private data class SpeechRequest(
        val spokenText: String,
        val preferredVoiceName: String?,
        val onComplete: () -> Unit,
        var attempt: Int = 0
    )

    companion object {
        private const val TAG_TIMING = "NagMeTiming"
        private const val GOOGLE_TTS_ENGINE = "com.google.android.tts"
        private const val TTS_RETRY_DELAY_MS = 1_500L
    }
}
