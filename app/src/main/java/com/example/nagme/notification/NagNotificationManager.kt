package com.example.nagme.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.nagme.MainActivity
import com.example.nagme.data.Nag
import com.example.nagme.data.NagOccurrence

class NagNotificationManager(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val notificationManager =
        appContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

    init {
        createChannel()
    }

    fun show(
        nag: Nag,
        occurrence: NagOccurrence
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notificationManager.notify(
            notificationIdForNag(
                nag.id
            ),
            createNotification(
                nag = nag,
                occurrence = occurrence
            )
        )
    }

    fun cancel(
        nagId: Long
    ) {

        notificationManager.cancel(
            notificationIdForNag(
                nagId
            )
        )
    }

    private fun createNotification(
        nag: Nag,
        occurrence: NagOccurrence
    ): Notification {

        val intent =
            Intent(
                appContext,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP

                putExtra(
                    EXTRA_NAG_ID,
                    nag.id
                )
            }

        val pendingIntent =
            PendingIntent.getActivity(
                appContext,
                notificationIdForNag(
                    nag.id
                ),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val countText =
            if (
                nag.maxNagCount != null
            ) {

                "Nagged ${occurrence.nagCount} of ${nag.maxNagCount} times"

            } else {

                "Nagged ${occurrence.nagCount} " +
                        if (
                            occurrence.nagCount == 1
                        ) {
                            "time"
                        } else {
                            "times"
                        }
            }

        val builder =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                Notification.Builder(
                    appContext,
                    CHANNEL_ID
                )

            } else {

                @Suppress("DEPRECATION")
                Notification.Builder(
                    appContext
                )
            }

        return builder
            .setSmallIcon(
                android.R.drawable
                    .ic_lock_idle_alarm
            )
            .setContentTitle(
                "NagMe"
            )
            .setContentText(
                nag.text
            )
            .setSubText(
                countText
            )
            .setContentIntent(
                pendingIntent
            )
            .setAutoCancel(
                false
            )
            .setOngoing(
                true
            )
            .build()
    }

    private fun createChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Nag reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        "Visual notifications shown when NagMe nags you."
                }

            notificationManager
                .createNotificationChannel(
                    channel
                )
        }
    }

    private fun notificationIdForNag(
        nagId: Long
    ): Int {

        return (
                nagId xor
                        (nagId ushr 32)
                )
            .toInt()
            .and(
                Int.MAX_VALUE
            )
            .plus(
                NOTIFICATION_ID_OFFSET
            )
    }

    companion object {

        const val EXTRA_NAG_ID =
            "nagme_extra_nag_id"

        private const val CHANNEL_ID =
            "nagme_user_nags"

        private const val NOTIFICATION_ID_OFFSET =
            10_000
    }
}