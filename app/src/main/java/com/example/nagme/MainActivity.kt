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
import androidx.lifecycle.lifecycleScope
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.RoomNagRepository
import com.example.nagme.notification.NagNotificationManager
import com.example.nagme.scheduler.NagScheduler
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var notificationNagId by
    mutableStateOf<Long?>(null)

    private var notificationOpenedFromNotification by
    mutableStateOf(false)

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        readNotificationIntent(
            intent
        )

        /*
         * Rebuild the next Android alarm whenever NagMe starts.
         *
         * This provides recovery after an app update if Android
         * has removed the previously registered AlarmManager alarm
         * while the Nag/occurrence data remains in Room.
         */
        rescheduleNagAlarms()

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

                var currentNagOpenedFromNotification by remember {
                    mutableStateOf(false)
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

                    currentNagOpenedFromNotification =
                        notificationOpenedFromNotification

                    temporaryScreen = null
                    editNagId = null

                    notificationNagId = null
                    notificationOpenedFromNotification = false
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
                            showDoneButton =
                                currentNagOpenedFromNotification,
                            onBack = {
                                currentNagId = null
                                currentNagOpenedFromNotification =
                                    false
                            },
                            onDone = {
                                currentNagId = null
                                currentNagOpenedFromNotification =
                                    false
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
                                currentNagOpenedFromNotification =
                                    false

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

    private fun rescheduleNagAlarms() {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            val database =
                NagDatabase.getInstance(
                    applicationContext
                )

            val repository =
                RoomNagRepository(
                    database
                )

            val scheduler =
                NagScheduler(
                    context =
                        applicationContext,
                    repository =
                        repository
                )

            scheduler.reschedule()
        }
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

            notificationOpenedFromNotification =
                intent.getBooleanExtra(
                    NagNotificationManager
                        .EXTRA_OPENED_FROM_NOTIFICATION,
                    false
                )
        }

        /*
         * Consume the extra so a normal Activity recreation
         * doesn't reopen the Nag unexpectedly.
         */
        intent.removeExtra(
            NagNotificationManager.EXTRA_NAG_ID
        )

        intent.removeExtra(
            NagNotificationManager
                .EXTRA_OPENED_FROM_NOTIFICATION
        )
    }
}
