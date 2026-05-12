package com.example.calendest.fakes

import com.example.calendest.data.model.Event
import com.example.calendest.data.model.EventWriteRequest
import com.example.calendest.data.model.EventsResponse
import com.example.calendest.data.network.ApiService
import retrofit2.Response

class FakeApiService : ApiService {

    var events: List<Event> = emptyList()
    var shouldThrowError: Boolean = false

    override suspend fun getEvents(
        authToken: String,
        maxResults: Int,
        pageToken: String?,
        singleEvents: Boolean,
        orderBy: String
    ): EventsResponse {
        if (shouldThrowError) throw RuntimeException("Fake API error")

        return EventsResponse(
            items = events,
            nextPageToken = null
        )
    }

    override suspend fun getEventById(
        eventId: String,
        authToken: String
    ): Event {
        if (shouldThrowError) throw RuntimeException("Fake API error")

        return events.firstOrNull { it.id == eventId }
            ?: Event(id = eventId, summary = "Fake Event")
    }

    override suspend fun createEvent(
        authToken: String,
        event: EventWriteRequest
    ): Event {
        if (shouldThrowError) throw RuntimeException("Fake API error")

        val created = Event(
            id = "created-event-id",
            summary = event.summary,
            location = event.location,
            start = event.start,
            end = event.end,
            recurrence = event.recurrence,
            reminders = event.reminders
        )

        events = events + created
        return created
    }

    override suspend fun updateEvent(
        eventId: String,
        authToken: String,
        event: EventWriteRequest
    ): Event {
        if (shouldThrowError) throw RuntimeException("Fake API error")

        val updated = Event(
            id = eventId,
            summary = event.summary,
            location = event.location,
            start = event.start,
            end = event.end,
            recurrence = event.recurrence,
            reminders = event.reminders
        )

        events = events.map {
            if (it.id == eventId) updated else it
        }

        return updated
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
        if (shouldThrowError) throw RuntimeException("Fake API error")

        events = events.filterNot { it.id == eventId }
        return Response.success(Unit)
    }
}