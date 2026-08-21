package com.example.nagme.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

        if (
            nextNagAt == null
        ) {
            cancelCurrentAlarm()
            return
        }

        val now =
            System.currentTimeMillis()

        val triggerAt =
            if (
                nextNagAt <= now
            ) {
                now + 1_000L
            } else {
                nextNagAt
            }

        scheduleAlarm(
            triggerAtMillis =
                triggerAt
        )
    }

    private fun scheduleAlarm(
        triggerAtMillis: Long
    ) {

        val pendingIntent =
            createPendingIntent()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return
        }

        try {

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )

        } catch (
            e: SecurityException
        ) {

            // Exact alarm permission is unavailable.
            // Permission/FIX UI is handled separately.
        }
    }

    fun cancelCurrentAlarm() {

        alarmManager.cancel(
            createPendingIntent()
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
    }
}