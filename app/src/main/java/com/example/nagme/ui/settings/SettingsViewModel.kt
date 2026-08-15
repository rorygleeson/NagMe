package com.example.nagme.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nagme.settings.UserSettings
import com.example.nagme.settings.UserSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        UserSettingsRepository(application)

    val settings =
        repository.settings
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(
                        5_000
                    ),
                initialValue =
                    UserSettings()
            )

    fun updateName(
        name: String
    ) {

        if (name.isBlank()) return

        viewModelScope.launch {
            repository.setUserName(
                name.trim()
            )
        }
    }

    fun setSpeakOnSilent(
        enabled: Boolean
    ) {

        viewModelScope.launch {
            repository.setSpeakOnSilent(
                enabled
            )
        }
    }

    fun setShowNagNotifications(
        enabled: Boolean
    ) {

        viewModelScope.launch {
            repository.setShowNagNotifications(
                enabled
            )
        }
    }

    fun setMissedNagWindowMinutes(
        minutes: Int
    ) {

        viewModelScope.launch {
            repository
                .setMissedNagWindowMinutes(
                    minutes
                )
        }
    }

    fun setDefaultVoiceName(
        voiceName: String?
    ) {

        viewModelScope.launch {
            repository.setDefaultVoiceName(
                voiceName
            )
        }
    }
}