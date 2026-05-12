package com.example.calendest

import android.app.Application
import com.example.calendest.notifications.EventNotificationScheduler

class CalendestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EventNotificationScheduler.createNotificationChannel(this)
    }
}