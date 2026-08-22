package com.example.nagme

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.nagme.auth.FirebaseAppCheckManager

class NagMeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val isDebugBuild =
            (applicationInfo.flags and
                    ApplicationInfo.FLAG_DEBUGGABLE) != 0

        FirebaseAppCheckManager()
            .initialize(
                isDebugBuild = isDebugBuild
            )
    }
}