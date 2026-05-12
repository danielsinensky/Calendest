package com.example.calendest.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.calendest.EventViewModel
import com.example.calendest.SnagReportViewModel
import com.example.calendest.data.local.dao.EventDao
import com.example.calendest.data.local.dao.SnagReportDao
import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.local.entity.SnagReportEntity
import com.example.calendest.data.model.Creator
import com.example.calendest.data.model.Event
import com.example.calendest.data.model.EventDateTime
import com.example.calendest.data.model.EventWriteRequest
import com.example.calendest.data.model.EventsResponse
import com.example.calendest.data.network.ApiService
import com.example.calendest.data.repository.EventRepository
import com.example.calendest.data.repository.SnagReportRepository
import com.example.calendest.fakes.AndroidFakeCalendarAuthClient
import com.example.calendest.navigation.Navigation
import com.example.calendest.testdata.fakeEventEntity
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class CalendestEndToEndTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appFlow_loadEvent_openDetailsScreen() {
        val eventViewModel = EventViewModel(
            repo = EventRepository(
                service = E2EFakeApiService(),
                eventDao = E2EFakeEventDao(listOf(fakeEventEntity))
            ),
            authClient = AndroidFakeCalendarAuthClient()
        )

        val snagReportViewModel = SnagReportViewModel(
            repository = SnagReportRepository(
                snagReportDao = E2EFakeSnagReportDao()
            )
        )

        eventViewModel.refreshHomeScreen(
            accessToken = "fake-token",
            forceRefresh = false
        )

        composeRule.setContent {
            Navigation(
                viewModel = eventViewModel,
                snagReportViewModel = snagReportViewModel
            )
        }

        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("Event One")
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("Event: Event One")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Creator: Creator")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Location: Room 101")
            .assertIsDisplayed()
    }
}

private class E2EFakeEventDao(
    initialEvents: List<EventEntity>
) : EventDao {

    private val events = initialEvents.toMutableList()

    override suspend fun getEvents(): List<EventEntity> = events

    override suspend fun getEventById(id: String): EventEntity? {
        return events.firstOrNull { it.id == id }
    }

    override suspend fun insertEvent(event: EventEntity) {
        events.removeAll { it.id == event.id }
        events.add(event)
    }

    override suspend fun insertEvents(events: List<EventEntity>) {
        this.events.clear()
        this.events.addAll(events)
    }

    override suspend fun deleteEventById(id: String) {
        events.removeAll { it.id == id }
    }

    override suspend fun deleteAllEvents() {
        events.clear()
    }
}

private class E2EFakeSnagReportDao : SnagReportDao {

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

private class E2EFakeApiService(
    private val events: List<Event> = listOf(fakeApiEvent)
) : ApiService {

    override suspend fun getEvents(
        authToken: String,
        maxResults: Int,
        pageToken: String?,
        singleEvents: Boolean,
        orderBy: String
    ): EventsResponse {
        return EventsResponse(
            items = events,
            nextPageToken = null
        )
    }

    override suspend fun getEventById(
        eventId: String,
        authToken: String
    ): Event {
        return events.firstOrNull { it.id == eventId } ?: fakeApiEvent.copy(id = eventId)
    }

    override suspend fun createEvent(
        authToken: String,
        event: EventWriteRequest
    ): Event {
        return fakeApiEvent.copy(
            id = "created-event-id",
            summary = event.summary,
            location = event.location,
            start = event.start,
            end = event.end,
            recurrence = event.recurrence,
            reminders = event.reminders
        )
    }

    override suspend fun updateEvent(
        eventId: String,
        authToken: String,
        event: EventWriteRequest
    ): Event {
        return fakeApiEvent.copy(
            id = eventId,
            summary = event.summary,
            location = event.location,
            start = event.start,
            end = event.end,
            recurrence = event.recurrence,
            reminders = event.reminders
        )
    }

    override suspend fun replaceEvent(
        eventId: String,
        authToken: String,
        event: EventWriteRequest
    ): Event {
        return updateEvent(
            eventId = eventId,
            authToken = authToken,
            event = event
        )
    }

    override suspend fun deleteEvent(
        eventId: String,
        authToken: String
    ): Response<Unit> {
        return Response.success(Unit)
    }
}

private val fakeApiEvent = Event(
    id = "1",
    summary = "Event One",
    location = "Room 101",
    creator = Creator(
        email = "creator@example.com",
        displayName = "Creator"
    ),
    organizer = null,
    start = EventDateTime(
        dateTime = "2026-05-01T10:00:00-04:00",
        date = null,
        timeZone = "America/New_York"
    ),
    end = EventDateTime(
        dateTime = "2026-05-01T11:00:00-04:00",
        date = null,
        timeZone = "America/New_York"
    ),
    recurrence = null,
    recurringEventId = null,
    reminders = null
)