package com.example.nagme.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.nagme.data.NagRepository

class NagScheduler(
    context: Context,
    private val repository: NagRepository
) {

    private val appContext =
        context.applicationContext

    private val alarmManager =
        appContext.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

    suspend fun reschedule() {

        val nextOccurrence =
            repository.getNextDueOccurrence()

        val nextNagAt =
            nextOccurrence?.nextNagAt

        if (nextNagAt == null) {

            Log.d(
                TAG,
                "No pending occurrence. Cancelling current Android alarm."
            )

            cancelCurrentAlarm()
            return
        }

        val now =
            System.currentTimeMillis()

        val triggerAt =
            if (nextNagAt <= now) {
                now + 1_000L
            } else {
                nextNagAt
            }

        Log.d(
            TAG,
            "Rescheduling alarm. " +
                    "occurrenceId=${nextOccurrence.id}, " +
                    "nextNagAt=$nextNagAt, " +
                    "triggerAt=$triggerAt, " +
                    "now=$now"
        )

        scheduleAlarm(
            triggerAtMillis = triggerAt
        )
    }

    private fun scheduleAlarm(
        triggerAtMillis: Long
    ) {

        val pendingIntent =
            createPendingIntent()

        val exactAlarmAvailable =
            Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.S ||
                    alarmManager.canScheduleExactAlarms()

        if (exactAlarmAvailable) {

            try {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )

                Log.d(
                    TAG,
                    "EXACT alarm scheduled successfully. " +
                            "triggerAt=$triggerAtMillis"
                )

                return

            } catch (
                e: SecurityException
            ) {

                Log.w(
                    TAG,
                    "Exact alarm rejected. Falling back to inexact alarm.",
                    e
                )

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Exact alarm scheduling failed. Falling back.",
                    e
                )
            }

        } else {

            Log.w(
                TAG,
                "Exact alarm permission unavailable. " +
                        "Using inexact allow-while-idle fallback."
            )
        }

        scheduleFallbackAlarm(
            triggerAtMillis = triggerAtMillis,
            pendingIntent = pendingIntent
        )
    }

    private fun scheduleFallbackAlarm(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )

            } else {

                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            Log.d(
                TAG,
                "FALLBACK alarm scheduled successfully. " +
                        "triggerAt=$triggerAtMillis"
            )

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "CRITICAL: Failed to schedule fallback alarm. " +
                        "triggerAt=$triggerAtMillis",
                e
            )
        }
    }

    fun cancelCurrentAlarm() {

        alarmManager.cancel(
            createPendingIntent()
        )

        Log.d(
            TAG,
            "Current Android alarm cancelled."
        )
    }

    private fun createPendingIntent():
            PendingIntent {

        val intent =
            Intent(
                appContext,
                AlarmReceiver::class.java
            )

        return PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {

        private const val
                ALARM_REQUEST_CODE =
            1001

        private const val
                TAG =
            "NagMeScheduler"
    }
}