package com.example.nagme.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nagme.data.NagDatabase
import com.example.nagme.data.RoomNagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action !=
            Intent.ACTION_BOOT_COMPLETED
        ) {
            return
        }

        val pendingResult =
            goAsync()

        val appContext =
            context.applicationContext

        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        ).launch {

            try {

                val database =
                    NagDatabase.getInstance(
                        appContext
                    )

                val repository =
                    RoomNagRepository(
                        database
                    )

                val scheduler =
                    NagScheduler(
                        context = appContext,
                        repository = repository
                    )

                scheduler.reschedule()

            } finally {

                pendingResult.finish()
            }
        }
    }
}