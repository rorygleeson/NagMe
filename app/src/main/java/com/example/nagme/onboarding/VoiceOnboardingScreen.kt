package com.example.nagme.ui.onboarding

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun VoiceOnboardingScreen(
    userName: String,
    onComplete: (String?) -> Unit
) {

    val context = LocalContext.current

    var tts by remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var voices by remember {
        mutableStateOf<List<Voice>>(emptyList())
    }

    var selectedVoiceName by remember {
        mutableStateOf<String?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        tts = TextToSpeech(
            context.applicationContext,
            { status ->

                if (status == TextToSpeech.SUCCESS) {

                    val engine = tts

                    if (engine != null) {

                        voices =
                            curateVoices(
                                engine.voices
                                    ?: emptySet()
                            )

                        selectedVoiceName =
                            voices.firstOrNull()?.name

                        if (voices.isEmpty()) {
                            errorMessage =
                                "No suitable English voices were found."
                        }
                    }

                } else {

                    errorMessage =
                        "NagMe couldn't initialise text-to-speech."
                }

                loading = false
            },
            GOOGLE_TTS_ENGINE
        )
    }

    DisposableEffect(Unit) {

        onDispose {
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            text = "Choose your voice",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Pick how NagMe should sound when it nags you."
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        when {

            loading -> {

                CircularProgressIndicator()
            }

            errorMessage != null -> {

                Text(
                    text = errorMessage!!
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onComplete(null)
                    }
                ) {
                    Text("USE PHONE DEFAULT")
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        items = voices,
                        key = { it.name }
                    ) { voice ->

                        VoiceRow(
                            voice = voice,
                            selected =
                                selectedVoiceName ==
                                        voice.name,
                            onSelect = {
                                selectedVoiceName =
                                    voice.name
                            },
                            onPreview = {

                                selectedVoiceName =
                                    voice.name

                                tts?.voice = voice

                                tts?.speak(
                                    "$userName, this is how I'll sound when I nag you.",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "nagme_voice_preview"
                                )
                            }
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled =
                        selectedVoiceName != null,
                    onClick = {
                        onComplete(
                            selectedVoiceName
                        )
                    }
                ) {
                    Text("USE THIS VOICE")
                }
            }
        }
    }
}

@Composable
private fun VoiceRow(
    voice: Voice,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selected,
            onClick = onSelect
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {

            Text(
                text = friendlyVoiceName(voice),
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(
                text = localeLabel(voice.locale),
                style =
                    MaterialTheme.typography.bodySmall
            )
        }

        OutlinedButton(
            onClick = onPreview
        ) {
            Text("▶")
        }
    }
}

private fun curateVoices(
    allVoices: Set<Voice>
): List<Voice> {

    val preferredCountries =
        listOf(
            "AU",
            "GB",
            "US"
        )

    val englishVoices =
        allVoices
            .filter {
                it.locale.language ==
                        Locale.ENGLISH.language
            }
            .filter {
                !it.isNetworkConnectionRequired
            }

    val result =
        mutableListOf<Voice>()

    for (country in preferredCountries) {

        val countryVoices =
            englishVoices
                .filter {
                    it.locale.country == country
                }
                .sortedWith(
                    compareByDescending<Voice> {
                        it.quality
                    }.thenBy {
                        it.name
                    }
                )
                .take(3)

        result.addAll(countryVoices)
    }

    if (result.isEmpty()) {

        result.addAll(
            englishVoices
                .sortedWith(
                    compareByDescending<Voice> {
                        it.quality
                    }.thenBy {
                        it.name
                    }
                )
                .take(6)
        )
    }

    return result.distinctBy {
        it.name
    }
}

private fun friendlyVoiceName(
    voice: Voice
): String {

    val country =
        when (voice.locale.country) {
            "AU" -> "Australian"
            "GB" -> "British"
            "US" -> "American"
            else ->
                voice.locale.displayCountry
                    .ifBlank { "English" }
        }

    val shortId =
        voice.name
            .substringAfterLast("-")
            .uppercase()

    return "$country · Voice $shortId"
}

private fun localeLabel(
    locale: Locale
): String {

    return buildString {

        append(
            locale.getDisplayLanguage(
                Locale.getDefault()
            )
        )

        if (locale.country.isNotBlank()) {

            append(" · ")

            append(
                locale.getDisplayCountry(
                    Locale.getDefault()
                )
            )
        }
    }
}

private const val GOOGLE_TTS_ENGINE =
    "com.google.android.tts"