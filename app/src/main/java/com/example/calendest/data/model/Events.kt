package com.example.calendest.data.model

import com.google.api.client.util.DateTime

data class EventsResponse(
    val kind: String? = null,
    val etag: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val updated: String? = null,
    val timeZone: String? = null,
    val accessRole: String? = null,
    val items: List<Event> = emptyList(),
    val nextPageToken: String? = null
)

data class Event(
    val kind: String? = null,
    val etag: String? = null,
    val id: String = "",
    val status: String? = null,
    val htmlLink: String? = null,
    val created: String? = null,
    val updated: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val creator: Creator? = null,
    val organizer: Organizer? = null,
    val start: EventDateTime? = null,
    val end: EventDateTime? = null,
    val recurrence: List<String>? = null,
    val recurringEventId: String? = null,
    val reminders: EventReminders? = null,
    val attendees: List<Attendee>? = null
)

data class EventDateTime(
    val dateTime: String? = null,
    val date: String? = null,
    val timeZone: String? = null
) {
    val parsedDateTime: DateTime?
        get() = dateTime?.let { DateTime(it) }
}

data class Creator(
    val id: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val self: Boolean? = null
)

data class Organizer(
    val id: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val self: Boolean? = null
)

data class Attendee(
    val id: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val organizer: Boolean? = null,
    val self: Boolean? = null,
    val resource: Boolean? = null,
    val optional: Boolean? = null,
    val responseStatus: String? = null,
    val comment: String? = null,
    val additionalGuests: Int? = null
)

data class EventWriteRequest(
    val summary: String,
    val location: String? = null,
    val description: String? = null,
    val start: EventDateTime,
    val end: EventDateTime,
    val recurrence: List<String>? = null,
    val reminders: EventReminders? = null
)

data class EventReminders(
    val useDefault: Boolean = true,
    val overrides: List<EventReminderOverride>? = null
)

data class EventReminderOverride(
    val method: String,
    val minutes: Int
)