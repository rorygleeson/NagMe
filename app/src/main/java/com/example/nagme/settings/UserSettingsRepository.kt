package com.example.nagme.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(
    name = "user_settings"
)

class UserSettingsRepository(
    context: Context
) {

    private val dataStore =
        context.applicationContext.userSettingsDataStore

    val settings: Flow<UserSettings> =
        dataStore.data.map { preferences ->

            UserSettings(
                userName =
                    preferences[USER_NAME] ?: "",

                defaultVoiceName =
                    preferences[DEFAULT_VOICE_NAME],

                speakOnSilent =
                    preferences[SPEAK_ON_SILENT] ?: true,

                showNagNotifications =
                    preferences[SHOW_NAG_NOTIFICATIONS] ?: false,

                missedNagWindowMinutes =
                    preferences[MISSED_NAG_WINDOW_MINUTES] ?: 120,

                onboardingComplete =
                    preferences[ONBOARDING_COMPLETE] ?: false
            )
        }

    suspend fun setUserName(
        name: String
    ) {
        dataStore.edit { preferences ->
            preferences[USER_NAME] = name.trim()
        }
    }

    suspend fun setDefaultVoiceName(
        voiceName: String?
    ) {
        dataStore.edit { preferences ->

            if (voiceName == null) {
                preferences.remove(DEFAULT_VOICE_NAME)
            } else {
                preferences[DEFAULT_VOICE_NAME] = voiceName
            }
        }
    }

    suspend fun setSpeakOnSilent(
        enabled: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[SPEAK_ON_SILENT] = enabled
        }
    }

    suspend fun setShowNagNotifications(
        enabled: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[SHOW_NAG_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setMissedNagWindowMinutes(
        minutes: Int
    ) {
        require(minutes >= 0)

        dataStore.edit { preferences ->
            preferences[MISSED_NAG_WINDOW_MINUTES] = minutes
        }
    }

    suspend fun setOnboardingComplete(
        complete: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun completeOnboarding(
        userName: String,
        voiceName: String?
    ) {
        dataStore.edit { preferences ->

            preferences[USER_NAME] =
                userName.trim()

            if (voiceName == null) {
                preferences.remove(DEFAULT_VOICE_NAME)
            } else {
                preferences[DEFAULT_VOICE_NAME] =
                    voiceName
            }

            preferences[ONBOARDING_COMPLETE] = true
        }
    }

    companion object {

        private val USER_NAME =
            stringPreferencesKey(
                "user_name"
            )

        private val DEFAULT_VOICE_NAME =
            stringPreferencesKey(
                "default_voice_name"
            )

        private val SPEAK_ON_SILENT =
            booleanPreferencesKey(
                "speak_on_silent"
            )

        private val SHOW_NAG_NOTIFICATIONS =
            booleanPreferencesKey(
                "show_nag_notifications"
            )

        private val MISSED_NAG_WINDOW_MINUTES =
            intPreferencesKey(
                "missed_nag_window_minutes"
            )

        private val ONBOARDING_COMPLETE =
            booleanPreferencesKey(
                "onboarding_complete"
            )
    }
}