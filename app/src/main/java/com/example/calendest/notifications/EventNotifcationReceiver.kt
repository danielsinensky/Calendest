package com.example.calendest.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.calendest.R

class EventNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val eventId = intent.getStringExtra(EventNotificationScheduler.EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(EventNotificationScheduler.EXTRA_EVENT_TITLE)
            ?: "Calendar event"
        val minutesBefore = intent.getIntExtra(
            EventNotificationScheduler.EXTRA_MINUTES_BEFORE,
            0
        )

        val notification = NotificationCompat.Builder(
            context,
            EventNotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("$minutesBefore minutes before")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            EventNotificationScheduler.notificationId(eventId, minutesBefore),
            notification
        )
    }
}