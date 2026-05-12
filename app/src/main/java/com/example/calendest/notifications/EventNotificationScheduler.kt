package com.example.calendest.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.calendest.data.model.EventReminderOverride
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.example.calendest.R

object EventNotificationScheduler {

    const val CHANNEL_ID = "calendest_event_notifications"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_EVENT_TITLE = "event_title"
    const val EXTRA_MINUTES_BEFORE = "minutes_before"

    private val commonReminderMinutes = listOf(
        0, 5, 10, 15, 30, 60, 90, 120, 1440
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Calendest events"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleEventNotifications(
        context: Context,
        eventId: String,
        eventTitle: String,
        eventStart: String,
        isAllDay: Boolean,
        reminders: List<EventReminderOverride>
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val popupReminders = reminders.filter { it.method == "popup" }

        cancelEventNotifications(
            context = context,
            eventId = eventId,
            reminderMinutes = popupReminders.map { it.minutes } + commonReminderMinutes
        )

        val eventStartMillis = parseEventStartMillis(
            eventStart = eventStart,
            isAllDay = isAllDay
        ) ?: return

        popupReminders.forEach { reminder ->
            val triggerAtMillis = eventStartMillis - reminder.minutes * 60_000L

            if (triggerAtMillis > System.currentTimeMillis()) {
                scheduleSingleNotification(
                    context = context,
                    eventId = eventId,
                    eventTitle = eventTitle,
                    minutesBefore = reminder.minutes,
                    triggerAtMillis = triggerAtMillis
                )
            }
        }
    }

    fun cancelEventNotifications(
        context: Context,
        eventId: String,
        reminderMinutes: List<Int> = commonReminderMinutes
    ) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        reminderMinutes.distinct().forEach { minutesBefore ->
            alarmManager.cancel(
                pendingIntent(
                    context = context,
                    eventId = eventId,
                    eventTitle = "",
                    minutesBefore = minutesBefore
                )
            )
        }
    }

    private fun scheduleSingleNotification(
        context: Context,
        eventId: String,
        eventTitle: String,
        minutesBefore: Int,
        triggerAtMillis: Long
    ) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(
                context = context,
                eventId = eventId,
                eventTitle = eventTitle,
                minutesBefore = minutesBefore
            )
        )
    }

    private fun pendingIntent(
        context: Context,
        eventId: String,
        eventTitle: String,
        minutesBefore: Int
    ): PendingIntent {
        val intent = Intent(
            context,
            EventNotificationReceiver::class.java
        ).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, eventTitle)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }

        return PendingIntent.getBroadcast(
            context,
            notificationId(eventId, minutesBefore),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notificationId(
        eventId: String,
        minutesBefore: Int
    ): Int {
        return "$eventId-$minutesBefore".hashCode()
    }

    private fun parseEventStartMillis(
        eventStart: String,
        isAllDay: Boolean
    ): Long? {
        return try {
            if (isAllDay || !eventStart.contains("T")) {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                parser.timeZone = TimeZone.getDefault()

                val date = parser.parse(eventStart) ?: return null

                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                calendar.timeInMillis
            } else {
                val parser = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    Locale.getDefault()
                )

                parser.parse(eventStart)?.time
            }
        } catch (e: Exception) {
            null
        }
    }

    fun showTestNotification(context: Context) {
        createNotificationChannel(context)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Calendest test notification")
            .setContentText("Notifications are working.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(999999, notification)
    }
}