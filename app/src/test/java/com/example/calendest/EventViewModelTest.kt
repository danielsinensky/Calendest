package com.example.calendest

import com.example.calendest.data.model.UiState
import com.example.calendest.data.repository.EventRepository
import com.example.calendest.fakes.FakeApiService
import com.example.calendest.fakes.FakeCalendarAuthClient
import com.example.calendest.fakes.FakeEventDao
import com.example.calendest.testdata.fakeEvent
import com.example.calendest.testdata.fakeEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelTest {

    private lateinit var viewModel: EventViewModel
    private lateinit var fakeApiService: FakeApiService
    private lateinit var fakeDao: FakeEventDao
    private lateinit var fakeAuthClient: FakeCalendarAuthClient
    private lateinit var repository: EventRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeApiService = FakeApiService()
        fakeDao = FakeEventDao()
        fakeAuthClient = FakeCalendarAuthClient()

        repository = EventRepository(
            service = fakeApiService,
            eventDao = fakeDao
        )

        viewModel = EventViewModel(
            repo = repository,
            authClient = fakeAuthClient
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshHomeScreen_setsListOfItemsToWhatApiReturns() = runTest {
        fakeApiService.events = listOf(fakeEvent)

        viewModel.refreshHomeScreen(
            accessToken = "fake-token",
            forceRefresh = true
        )

        advanceUntilIdle()

        val state = viewModel.state.value

        assertTrue(state.uiState is UiState.Success)

        val successState = state.uiState as UiState.Success

        assertEquals(1, successState.data.size)
        assertEquals("Event One", successState.data[0].summary)

        assertEquals(1, state.events.size)
        assertEquals("Event One", state.events[0].summary)
    }

    @Test
    fun refreshHomeScreen_withNoToken_setsAuthRequired() = runTest {
        viewModel.refreshHomeScreen(
            accessToken = "",
            forceRefresh = true
        )

        advanceUntilIdle()

        val state = viewModel.state.value

        assertTrue(state.uiState is UiState.AuthRequired)
    }

    @Test
    fun refreshHomeScreen_withEmptyApiResult_setsEmptyState() = runTest {
        fakeApiService.events = emptyList()

        viewModel.refreshHomeScreen(
            accessToken = "fake-token",
            forceRefresh = true
        )

        advanceUntilIdle()

        val state = viewModel.state.value

        assertTrue(state.uiState is UiState.Empty)
        assertEquals(0, state.events.size)
    }

    @Test
    fun refreshHomeScreen_whenApiThrows_setsErrorState() = runTest {
        fakeApiService.shouldThrowError = true

        viewModel.refreshHomeScreen(
            accessToken = "fake-token",
            forceRefresh = true
        )

        advanceUntilIdle()

        val state = viewModel.state.value

        assertTrue(state.uiState is UiState.Error)

        val errorState = state.uiState as UiState.Error
        assertEquals("Network error", errorState.message)
    }

    @Test
    fun selectEvent_updatesSelectedEvent() = runTest {
        viewModel.selectEvent(fakeEventEntity)

        val state = viewModel.state.value

        assertEquals(fakeEventEntity, state.selectedEvent)
    }

    @Test
    fun onAuthCancelled_setsAuthRequired() = runTest {
        viewModel.onAuthCancelled()

        val state = viewModel.state.value

        assertTrue(state.uiState is UiState.AuthRequired)
    }

    @Test
    fun refreshDetailsScreen_returnsEarly_whenNoSelectedEvent() = runTest {
        viewModel.refreshDetailsScreen()

        advanceUntilIdle()

        val state = viewModel.state.value

        assertTrue(state.uiState is UiState.AuthRequired)
    }

    @Test
    fun refreshDetailsScreen_updatesSelectedEventFromRepository() = runTest {
        fakeApiService.events = listOf(fakeEvent)

        viewModel.refreshHomeScreen(
            accessToken = "fake-token",
            forceRefresh = true
        )

        advanceUntilIdle()

        viewModel.selectEvent(fakeEventEntity)
        viewModel.refreshDetailsScreen()

        advanceUntilIdle()

        val state = viewModel.state.value

        assertEquals("Event One", state.selectedEvent?.summary)
        assertEquals("Creator", state.selectedEvent?.creatorName)
    }
}