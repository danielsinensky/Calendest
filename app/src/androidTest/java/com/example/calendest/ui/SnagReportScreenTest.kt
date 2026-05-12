package com.example.calendest.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import com.example.calendest.SnagReportViewModel
import com.example.calendest.data.local.dao.SnagReportDao
import com.example.calendest.data.local.entity.SnagReportEntity
import com.example.calendest.data.repository.SnagReportRepository
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.example.calendest.ui.screens.SnagReportScreen

class SnagReportScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun submitReport_withMissingFields_showsMissingFieldsMessage() {
        val viewModel = createViewModel()

        composeRule.setContent {
            SnagReportScreen(
                snagReportViewModel = viewModel,
                onReportSubmitted = {}
            )
        }

        composeRule.onNodeWithText("Submit Report").performClick()

        composeRule
            .onNodeWithText(
                "Required field(s) missing: Title, Description, Steps to Reproduce"
            )
            .assertIsDisplayed()
    }

    @Test
    fun submitReport_withAllFields_callsOnReportSubmitted() {
        val viewModel = createViewModel()
        var submitted = false

        composeRule.setContent {
            SnagReportScreen(
                snagReportViewModel = viewModel,
                onReportSubmitted = {
                    submitted = true
                }
            )
        }

        composeRule.onNodeWithText("Title *").performTextInput("Crash on launch")
        composeRule.onNodeWithText("Description *").performTextInput("App crashes immediately.")
        composeRule.onNodeWithText("Steps to Reproduce *").performTextInput("Open the app.")

        composeRule.onNodeWithText("Submit Report").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            submitted
        }

        assertTrue(submitted)
    }

    private fun createViewModel(): SnagReportViewModel {
        val dao = FakeSnagReportDao()
        val repository = SnagReportRepository(dao)
        return SnagReportViewModel(repository)
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