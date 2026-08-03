package com.boykta.net.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    const val CHANNEL_ID = "walk_win_reminder"
    private const val ALARM_REQUEST_CODE = 1001

    /** Creates the notification channel (safe to call multiple times). */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكير امشِ واربح",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكير بتفعيل 2 جيجابايت الأسبوعية المجانية"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /** Schedules a local notification 7 days from now. Replaces any existing pending alarm. */
    fun scheduleWalkWinReminder(context: Context) {
        createChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = buildPendingIntent(context)
        val triggerAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    /** Cancels any pending reminder alarm. */
    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WalkWinReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
