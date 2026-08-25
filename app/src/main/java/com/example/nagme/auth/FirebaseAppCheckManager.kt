package com.example.nagme.auth

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class FirebaseAppCheckManager {

    fun initialize(
        isDebugBuild: Boolean
    ) {

        val appCheck =
            FirebaseAppCheck.getInstance()

        if (isDebugBuild) {

            /*
             * The debug App Check dependency only exists in
             * debug builds. Load it dynamically so release
             * builds do not need the debug library at compile time.
             */
            val debugProviderFactory =
                Class
                    .forName(
                        "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
                    )
                    .getMethod(
                        "getInstance"
                    )
                    .invoke(null)
                        as AppCheckProviderFactory

            appCheck
                .installAppCheckProviderFactory(
                    debugProviderFactory
                )

        } else {

            appCheck
                .installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory
                        .getInstance()
                )
        }
    }
}