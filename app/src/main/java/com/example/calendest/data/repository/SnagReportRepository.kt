package com.example.calendest.data.repository

import com.example.calendest.data.local.dao.SnagReportDao
import com.example.calendest.data.local.entity.SnagReportEntity

class SnagReportRepository(
    private val snagReportDao: SnagReportDao
) {
    suspend fun submitReport(
        title: String,
        description: String,
        stepsToReproduce: String,
        severity: String
    ) {
        val report = SnagReportEntity(
            title = title,
            description = description,
            stepsToReproduce = stepsToReproduce,
            severity = severity
        )

        snagReportDao.insertSnagReport(report)
    }

    suspend fun getReports(): List<SnagReportEntity> {
        return snagReportDao.getAllSnagReports()
    }

    suspend fun markReportAsEmailed(id: Int) {
        snagReportDao.markReportAsSynced(id)
    }

    suspend fun deleteReport(id: Int) {
        snagReportDao.deleteReport(id)
    }
}