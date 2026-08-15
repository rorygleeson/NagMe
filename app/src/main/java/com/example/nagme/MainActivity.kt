package com.example.nagme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.nagme.notification.NagNotificationManager
import com.example.nagme.settings.UserSettings
import com.example.nagme.settings.UserSettingsRepository
import com.example.nagme.speech.VoicePickerScreen
import com.example.nagme.ui.add.AddNagScreen
import com.example.nagme.ui.current.CurrentNagScreen
import com.example.nagme.ui.edit.EditNagScreen
import com.example.nagme.ui.history.HistoryScreen
import com.example.nagme.ui.home.HomeScreen
import com.example.nagme.ui.onboarding.NameOnboardingScreen
import com.example.nagme.ui.onboarding.VoiceOnboardingScreen
import com.example.nagme.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var notificationNagId by
    mutableStateOf<Long?>(null)

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        readNotificationIntent(
            intent
        )

        setContent {

            MaterialTheme {

                val settingsRepository =
                    remember {
                        UserSettingsRepository(
                            applicationContext
                        )
                    }

                val scope =
                    rememberCoroutineScope()

                val settings by
                settingsRepository.settings
                    .collectAsState(
                        initial = UserSettings()
                    )

                var temporaryScreen by remember {
                    mutableStateOf<String?>(null)
                }

                var currentNagId by remember {
                    mutableStateOf<Long?>(null)
                }

                var editNagId by remember {
                    mutableStateOf<Long?>(null)
                }

                /*
                 * A notification tap takes priority over the
                 * normal temporary screen.
                 */
                val nagIdFromNotification =
                    notificationNagId

                if (
                    nagIdFromNotification != null
                ) {
                    currentNagId =
                        nagIdFromNotification

                    temporaryScreen = null
                    editNagId = null

                    notificationNagId = null
                }

                when {

                    !settings.onboardingComplete &&
                            settings.userName.isBlank() -> {

                        NameOnboardingScreen(
                            onNext = { name ->

                                scope.launch {
                                    settingsRepository
                                        .setUserName(name)
                                }
                            }
                        )
                    }

                    !settings.onboardingComplete -> {

                        VoiceOnboardingScreen(
                            userName =
                                settings.userName,
                            onComplete = { voiceName ->

                                scope.launch {

                                    settingsRepository
                                        .completeOnboarding(
                                            userName =
                                                settings.userName,
                                            voiceName =
                                                voiceName
                                        )
                                }
                            }
                        )
                    }

                    currentNagId != null -> {

                        CurrentNagScreen(
                            nagId = currentNagId!!,
                            onBack = {
                                currentNagId = null
                            },
                            onDone = {
                                currentNagId = null
                            }
                        )
                    }

                    editNagId != null -> {

                        EditNagScreen(
                            nagId = editNagId!!,
                            onSaved = {
                                editNagId = null
                            },
                            onDeleted = {
                                editNagId = null
                            },
                            onBack = {
                                editNagId = null
                            }
                        )
                    }

                    temporaryScreen == "Add Nag" -> {

                        AddNagScreen(
                            onSaved = {
                                temporaryScreen = null
                            },
                            onCancel = {
                                temporaryScreen = null
                            }
                        )
                    }

                    temporaryScreen == "History" -> {

                        HistoryScreen(
                            onBack = {
                                temporaryScreen = null
                            }
                        )
                    }

                    temporaryScreen == "Settings" -> {

                        SettingsScreen(
                            onBack = {
                                temporaryScreen = null
                            },
                            onChooseVoice = {
                                temporaryScreen =
                                    "Voice Picker"
                            }
                        )
                    }

                    temporaryScreen == "Voice Picker" -> {

                        VoicePickerScreen(
                            currentVoiceName =
                                settings.defaultVoiceName,
                            onSave = { voiceName ->

                                scope.launch {

                                    settingsRepository
                                        .setDefaultVoiceName(
                                            voiceName
                                        )

                                    temporaryScreen =
                                        "Settings"
                                }
                            },
                            onBack = {
                                temporaryScreen =
                                    "Settings"
                            }
                        )
                    }

                    else -> {

                        HomeScreen(
                            onAddNag = {
                                temporaryScreen =
                                    "Add Nag"
                            },
                            onHistory = {
                                temporaryScreen =
                                    "History"
                            },
                            onSettings = {
                                temporaryScreen =
                                    "Settings"
                            },
                            onOpenActiveNag = { nagId ->
                                currentNagId =
                                    nagId
                            },
                            onEditUpcomingNag = { nagId ->
                                editNagId =
                                    nagId
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(intent)

        setIntent(intent)

        readNotificationIntent(
            intent
        )
    }

    private fun readNotificationIntent(
        intent: Intent?
    ) {

        if (
            intent == null ||
            !intent.hasExtra(
                NagNotificationManager.EXTRA_NAG_ID
            )
        ) {
            return
        }

        val nagId =
            intent.getLongExtra(
                NagNotificationManager.EXTRA_NAG_ID,
                -1L
            )

        if (
            nagId > 0L
        ) {
            notificationNagId =
                nagId
        }

        /*
         * Consume the extra so a normal Activity recreation
         * doesn't reopen the Nag unexpectedly.
         */
        intent.removeExtra(
            NagNotificationManager.EXTRA_NAG_ID
        )
    }
}