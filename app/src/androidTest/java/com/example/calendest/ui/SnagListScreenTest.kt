package com.example.calendest.ui.screens

import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.calendest.SnagReportViewModel
import com.example.calendest.data.local.dao.SnagReportDao
import com.example.calendest.data.local.entity.SnagReportEntity
import com.example.calendest.data.repository.SnagReportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class SnagListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun snagListScreen_displaysSavedReports() {
        runBlocking {

            val dao = FakeSnagReportDao()

            dao.insertSnagReport(
                SnagReportEntity(
                    id = 1,
                    title = "Refresh bug",
                    description = "Pull to refresh does not update events.",
                    stepsToReproduce = "Open HomeScreen and pull down.",
                    severity = "Medium"
                )
            )

            val viewModel = SnagReportViewModel(
                SnagReportRepository(dao)
            )

            composeRule.setContent {
                SnagListScreen(
                    snagReportViewModel = viewModel
                )
            }

            composeRule.onNodeWithText("Reported Snags").assertIsDisplayed()
            composeRule.onNodeWithText("Title: Refresh bug").assertIsDisplayed()
            composeRule.onNodeWithText("Severity: Medium").assertIsDisplayed()
        }
    }

    @Test
    fun deleteReport_removesReportFromScreen() {
        runBlocking {

            val dao = FakeSnagReportDao()

            dao.insertSnagReport(
                SnagReportEntity(
                    id = 1,
                    title = "Delete me",
                    description = "This report should be deleted.",
                    stepsToReproduce = "Tap delete.",
                    severity = "Low"
                )
            )

            val viewModel = SnagReportViewModel(
                SnagReportRepository(dao)
            )

            composeRule.setContent {
                SnagListScreen(
                    snagReportViewModel = viewModel
                )
            }

            composeRule.onNodeWithText("Title: Delete me").assertIsDisplayed()

            composeRule.onNodeWithText("Delete Report").performClick()

            composeRule.waitForIdle()

            composeRule.onNodeWithText("Title: Delete me").assertIsNotDisplayed()
        }
    }

    private class FakeSnagReportDao : SnagReportDao {
        private val reports = mutableListOf<SnagReportEntity>()

        override suspend fun insertSnagReport(report: SnagReportEntity) {
            val newId = if (report.id == 0) reports.size + 1 else report.id
            reports.add(report.copy(id = newId))
        }

        override suspend fun getAllSnagReports(): List<SnagReportEntity> {
            return reports.sortedByDescending { it.createdAt }
        }

        override suspend fun markReportAsSynced(id: Int) {
            val index = reports.indexOfFirst { it.id == id }
            if (index != -1) {
                reports[index] = reports[index].copy(synced = true)
            }
        }

        override suspend fun deleteReport(id: Int) {
            reports.removeAll { it.id == id }
        }

        override suspend fun getUnsyncedReports(): List<SnagReportEntity> {
            return reports.filter { !it.synced }
        }
    }
}