package com.example.nagme.auth

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class FirebaseAppCheckManager {

    fun initialize(
        isDebugBuild: Boolean
    ) {
        val appCheck =
            FirebaseAppCheck.getInstance()

        if (isDebugBuild) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}