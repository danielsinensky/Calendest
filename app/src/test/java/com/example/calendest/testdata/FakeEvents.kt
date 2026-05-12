package com.example.calendest.testdata

import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.model.Creator
import com.example.calendest.data.model.Event
import com.example.calendest.data.model.EventDateTime
import com.example.calendest.data.model.Organizer

val fakeEvent = Event(
    kind = "calendar#event",
    etag = "etag-1",
    id = "1",
    status = "confirmed",
    htmlLink = "https://example.com/1",
    created = "2026-01-01T00:00:00Z",
    updated = "2026-01-01T00:00:00Z",
    summary = "Event One",
    location = "Room 101",
    creator = Creator(
        id = null,
        email = "creator@example.com",
        displayName = "Creator",
        self = true
    ),
    organizer = Organizer(
        id = null,
        email = "organizer@example.com",
        displayName = "Organizer",
        self = true
    ),
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
    attendees = null
)

val fakeEventEntity = EventEntity(
    id = "1",
    summary = "Event One",
    creatorName = "Creator",
    location = "Room 101",
    start = "2026-05-01T10:00:00-04:00",
    end = "2026-05-01T11:00:00-04:00"
)