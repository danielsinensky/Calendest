package com.example.calendest

import com.example.calendest.data.local.dao.SnagReportDao
import com.example.calendest.data.local.entity.SnagReportEntity
import com.example.calendest.data.repository.SnagReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnagReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var dao: FakeSnagReportDao
    private lateinit var repository: SnagReportRepository
    private lateinit var viewModel: SnagReportViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeSnagReportDao()
        repository = SnagReportRepository(dao)
        viewModel = SnagReportViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submitReport_withMissingFields_setsErrorMessageAndDoesNotNavigate() = runTest {
        var navigatedBack = false

        viewModel.title = ""
        viewModel.description = ""
        viewModel.stepsToReproduce = ""

        viewModel.submitReport {
            navigatedBack = true
        }

        assertFalse(navigatedBack)
        assertEquals(
            "Required field(s) missing: Title, Description, Steps to Reproduce",
            viewModel.errorMessage
        )
        assertTrue(dao.reports.isEmpty())
    }

    @Test
    fun submitReport_withAllRequiredFields_savesReportAndNavigatesBack() = runTest {
        var navigatedBack = false

        viewModel.title = "Crash on details screen"
        viewModel.description = "The app crashes after opening an event."
        viewModel.stepsToReproduce = "Open app, tap event, wait for details screen."
        viewModel.severity = "High"

        viewModel.submitReport {
            navigatedBack = true
        }

        advanceUntilIdle()

        assertTrue(navigatedBack)
        assertEquals(1, dao.reports.size)
        assertEquals("Crash on details screen", dao.reports.first().title)
        assertEquals("", viewModel.title)
        assertEquals("", viewModel.description)
        assertEquals("", viewModel.stepsToReproduce)
        assertEquals("Medium", viewModel.severity)
    }

    @Test
    fun deleteReport_removesReportFromList() = runTest {
        dao.insertSnagReport(
            SnagReportEntity(
                id = 1,
                title = "Bad refresh",
                description = "Refresh does not update UI.",
                stepsToReproduce = "Pull to refresh.",
                severity = "Medium"
            )
        )

        viewModel.loadReports()
        advanceUntilIdle()

        assertEquals(1, viewModel.reports.size)

        viewModel.deleteReport(1)
        advanceUntilIdle()

        assertTrue(viewModel.reports.isEmpty())
    }

    private class FakeSnagReportDao : SnagReportDao {
        val reports = mutableListOf<SnagReportEntity>()

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