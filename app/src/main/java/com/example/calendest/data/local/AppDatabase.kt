package com.example.calendest.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.calendest.data.local.dao.SnagReportDao
import com.example.calendest.data.local.entity.SnagReportEntity
import com.example.calendest.data.local.dao.EventDao
import com.example.calendest.data.local.dao.SavedGoogleAccountDao
import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.local.entity.SavedGoogleAccountEntity

@Database(
    entities = [
        EventEntity::class,
        SnagReportEntity::class,
        SavedGoogleAccountEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    abstract fun snagReportDao(): SnagReportDao

    abstract fun savedGoogleAccountDao(): SavedGoogleAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calendest_db"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}