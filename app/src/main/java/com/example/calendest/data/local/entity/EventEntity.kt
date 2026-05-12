package com.example.calendest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val summary: String,
    val start: String,
    val end: String,
    val creatorName: String?,
    val location: String?,
    val recurrence: String? = null,
    val reminders: String? = null,
    val recurringEventId: String? = null
)