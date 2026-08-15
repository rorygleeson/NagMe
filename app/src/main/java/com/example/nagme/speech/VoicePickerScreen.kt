package com.example.nagme.speech

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun VoicePickerScreen(
    currentVoiceName: String?,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    var tts by remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    var initStatus by remember {
        mutableIntStateOf(Int.MIN_VALUE)
    }

    var voiceOptions by remember {
        mutableStateOf<List<VoiceOption>>(
            emptyList()
        )
    }

    var selectedVoiceName by remember {
        mutableStateOf(
            currentVoiceName
        )
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(Unit) {

        val engine =
            TextToSpeech(
                context.applicationContext,
                { status ->
                    initStatus = status
                },
                GOOGLE_TTS_ENGINE
            )

        tts = engine

        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }

    LaunchedEffect(
        initStatus,
        tts
    ) {

        val engine =
            tts ?: return@LaunchedEffect

        if (
            initStatus ==
            TextToSpeech.SUCCESS
        ) {

            val availableVoices =
                engine.voices
                    ?.filter { voice ->

                        voice.locale.language
                            .equals(
                                Locale.ENGLISH.language,
                                ignoreCase = true
                            )
                    }
                    ?.sortedWith(
                        compareBy<Voice>(
                            {
                                countryPriority(
                                    it.locale.country
                                )
                            },
                            {
                                it.locale.country
                            },
                            {
                                baseVoiceId(it)
                            },
                            {
                                it.isNetworkConnectionRequired
                            }
                        )
                    )
                    ?: emptyList()

            voiceOptions =
                buildVoiceOptions(
                    availableVoices
                )

            if (
                voiceOptions.isEmpty()
            ) {

                errorMessage =
                    "No English Google voices were found."

            } else {

                val currentExists =
                    voiceOptions.any {
                        it.voice.name ==
                                selectedVoiceName
                    }

                if (!currentExists) {

                    selectedVoiceName =
                        voiceOptions
                            .first()
                            .voice
                            .name
                }

                errorMessage = null
            }

            loading = false

        } else if (
            initStatus !=
            Int.MIN_VALUE
        ) {

            loading = false

            errorMessage =
                "NagMe couldn't initialise Google text-to-speech."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Choose voice",
                style =
                    MaterialTheme.typography
                        .headlineMedium,
                modifier =
                    Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = onBack
            ) {
                Text("‹ BACK")
            }
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Tap a voice to hear a preview."
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        when {

            loading -> {

                Text(
                    text =
                        "Loading voices..."
                )
            }

            errorMessage != null -> {

                Text(
                    text =
                        errorMessage!!
                )
            }

            else -> {

                LazyColumn(
                    modifier =
                        Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    items(
                        items =
                            voiceOptions,
                        key = { option ->
                            option.voice.name
                        }
                    ) { option ->

                        VoiceRow(
                            option = option,
                            selected =
                                option.voice.name ==
                                        selectedVoiceName,
                            onSelect = {

                                selectedVoiceName =
                                    option.voice.name

                                tts?.voice =
                                    option.voice

                                tts?.speak(
                                    "Hi. This is how I will nag you.",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "nagme_voice_preview"
                                )
                            }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Button(
                    modifier =
                        Modifier.fillMaxWidth(),
                    enabled =
                        selectedVoiceName !=
                                null,
                    onClick = {

                        selectedVoiceName
                            ?.let {
                                onSave(it)
                            }
                    }
                ) {
                    Text(
                        "USE THIS VOICE"
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceRow(
    option: VoiceOption,
    selected: Boolean,
    onSelect: () -> Unit
) {

    val voice =
        option.voice

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onSelect
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            RadioButton(
                selected = selected,
                onClick = onSelect
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        option.displayName,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )

                Text(
                    text =
                        friendlyLocale(
                            voice.locale
                        ),
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }
    }
}

private fun buildVoiceOptions(
    voices: List<Voice>
): List<VoiceOption> {

    /*
     * Google TTS can expose both a local/offline and a network
     * version of the same underlying voice.
     *
     * Collapse those variants so the user only sees each
     * actual voice once.
     *
     * The list is already sorted with local/offline before
     * network, so first() automatically prefers local.
     */
    val uniqueVoices =
        voices
            .groupBy { voice ->

                Pair(
                    voice.locale.country
                        .uppercase(),
                    baseVoiceId(
                        voice
                    )
                )
            }
            .values
            .map { variants ->
                variants.first()
            }

    val voiceNumbersByCountry =
        mutableMapOf<
                String,
                MutableMap<String, Int>
                >()

    uniqueVoices.forEach { voice ->

        val country =
            voice.locale.country
                .uppercase()

        val baseId =
            baseVoiceId(
                voice
            )

        val countryVoices =
            voiceNumbersByCountry
                .getOrPut(
                    country
                ) {
                    mutableMapOf()
                }

        if (
            baseId !in
            countryVoices
        ) {

            countryVoices[
                baseId
            ] =
                countryVoices.size + 1
        }
    }

    return uniqueVoices.map { voice ->

        val country =
            voice.locale.country
                .uppercase()

        val baseId =
            baseVoiceId(
                voice
            )

        val number =
            voiceNumbersByCountry[
                country
            ]?.get(
                baseId
            ) ?: 1

        VoiceOption(
            voice = voice,
            displayName =
                "${friendlyCountryName(country)} Voice $number"
        )
    }
}

private fun baseVoiceId(
    voice: Voice
): String {

    return voice.name
        .removeSuffix(
            "-local"
        )
        .removeSuffix(
            "-network"
        )
        .lowercase()
}

private fun countryPriority(
    country: String
): Int {

    return when (
        country.uppercase()
    ) {

        "AU" -> 0
        "GB" -> 1
        "IN" -> 2
        "US" -> 3
        else -> 4
    }
}

private fun friendlyCountryName(
    country: String
): String {

    return when (
        country.uppercase()
    ) {

        "AU" ->
            "Australian"

        "GB" ->
            "British"

        "IN" ->
            "Indian"

        "US" ->
            "US"

        "CA" ->
            "Canadian"

        "IE" ->
            "Irish"

        else ->
            Locale(
                "",
                country
            ).displayCountry
                .ifBlank {
                    "English"
                }
    }
}

private fun friendlyLocale(
    locale: Locale
): String {

    val country =
        locale.displayCountry

    return if (
        country.isNotBlank()
    ) {

        "${locale.displayLanguage} · $country"

    } else {

        locale.displayLanguage
    }
}

private data class VoiceOption(
    val voice: Voice,
    val displayName: String
)

private const val GOOGLE_TTS_ENGINE =
    "com.google.android.tts"