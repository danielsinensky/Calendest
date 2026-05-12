package com.example.calendest.data.local

import android.content.Context
import androidx.room.Room
import com.example.calendest.auth.CalendarAuthClient
import com.example.calendest.auth.GoogleCalendarAuthClient
import com.example.calendest.data.network.service
import com.example.calendest.data.repository.EventRepository

object AppModule {

    @Volatile
    private var dbInstance: AppDatabase? = null

    private fun provideDatabase(context: Context): AppDatabase {
        return dbInstance ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "calendest_db"
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

            dbInstance = instance
            instance
        }
    }

    fun provideRepository(context: Context): EventRepository {
        val db = provideDatabase(context)

        return EventRepository(
            service = service,
            eventDao = db.eventDao(),
            savedGoogleAccountDao = db.savedGoogleAccountDao()
        )
    }

    fun provideAuthClient(): CalendarAuthClient {
        return GoogleCalendarAuthClient()
    }
}