package com.example.nagme.scheduler

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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

        Log.d(
            TAG,
            "BootReceiver received action=${intent.action}"
        )

        val isSupportedAction =
            intent.action ==
                    Intent.ACTION_BOOT_COMPLETED ||
                    intent.action ==
                    Intent.ACTION_MY_PACKAGE_REPLACED ||
                    (
                            Build.VERSION.SDK_INT >=
                                    Build.VERSION_CODES.S &&
                                    intent.action ==
                                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
                            )

        if (!isSupportedAction) {
            return
        }

        val appContext =
            context.applicationContext

        /*
         * Android sends the exact-alarm permission broadcast
         * when permission is GRANTED.
         *
         * Check again before rescheduling because the permission
         * could have been revoked again before this receiver runs.
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S &&
            intent.action ==
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {

            val alarmManager =
                appContext.getSystemService(
                    Context.ALARM_SERVICE
                ) as AlarmManager

            if (!alarmManager.canScheduleExactAlarms()) {

                Log.w(
                    TAG,
                    "Exact-alarm permission broadcast received, " +
                            "but permission is no longer available."
                )

                return
            }
        }

        val pendingResult =
            goAsync()

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

                Log.d(
                    TAG,
                    "Rescheduling alarms after ${intent.action}"
                )

                scheduler.reschedule()

                Log.d(
                    TAG,
                    "Alarm reschedule completed after ${intent.action}"
                )

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Failed to reschedule alarms after ${intent.action}",
                    e
                )

            } finally {

                pendingResult.finish()
            }
        }
    }

    companion object {

        private const val TAG =
            "NagMeScheduler"
    }
}