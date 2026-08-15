package com.example.nagme.settings

data class UserSettings(

    // Set during onboarding.
    val userName: String = "",

    // Android TTS Voice.name.
    // Null means use our default/fallback voice.
    val defaultVoiceName: String? = null,

    // We locked V1 to speak even when the phone is on silent.
    val speakOnSilent: Boolean = true,

    // Show a visual Android notification when a Nag fires.
    // Off by default. The user can enable it in Settings.
    // Speech always happens regardless of this setting.
    val showNagNotifications: Boolean = false,

    // How late a missed Nag is still allowed to run.
    val missedNagWindowMinutes: Int = 120,

    // Determines whether onboarding or Home opens.
    val onboardingComplete: Boolean = false
)