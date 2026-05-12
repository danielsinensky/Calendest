package com.example.calendest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snag_reports")
data class SnagReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val stepsToReproduce: String,
    val severity: String,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)