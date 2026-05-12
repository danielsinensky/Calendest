package com.example.calendest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.calendest.data.local.entity.SnagReportEntity

@Dao
interface SnagReportDao {

    @Insert
    suspend fun insertSnagReport(report: SnagReportEntity)

    @Query("SELECT * FROM snag_reports ORDER BY createdAt DESC")
    suspend fun getAllSnagReports(): List<SnagReportEntity>

    @Query("SELECT * FROM snag_reports WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedReports(): List<SnagReportEntity>

    @Query("UPDATE snag_reports SET synced = 1 WHERE id = :id")
    suspend fun markReportAsSynced(id: Int)

    @Query("DELETE FROM snag_reports WHERE id = :id")
    suspend fun deleteReport(id: Int)
}