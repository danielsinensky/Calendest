package com.example.calendest.testdata

import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.model.Creator
import com.example.calendest.data.model.Event
import com.example.calendest.data.model.EventDateTime
import com.example.calendest.data.model.Organizer

val fakeEvent = Event(
    id = "1",
    summary = "Event One",
    location = "Room 101",
    creator = Creator(
        email = "creator@example.com",
        displayName = "Creator"
    ),
    organizer = Organizer(
        email = "organizer@example.com",
        displayName = "Organizer"
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
    recurringEventId = null,
    reminders = null
)

val fakeEventEntity = EventEntity(
    id = "1",
    summary = "Event One",
    start = "2026-05-01T10:00:00-04:00",
    end = "2026-05-01T11:00:00-04:00",
    creatorName = "Creator",
    location = "Room 101",
    recurrence = "Does not repeat",
    reminders = "Calendar default reminders"
)

val fakeEvents = listOf(fakeEvent)

val fakeEventEntities = listOf(fakeEventEntity)