package com.example.calendest.data.repository

import com.example.calendest.data.local.dao.EventDao
import com.example.calendest.data.local.dao.SavedGoogleAccountDao
import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.local.entity.SavedGoogleAccountEntity
import com.example.calendest.data.model.Event
import com.example.calendest.data.model.EventDateTime
import com.example.calendest.data.model.EventReminders
import com.example.calendest.data.model.EventWriteRequest
import com.example.calendest.data.network.ApiService

class EventRepository(
    private val service: ApiService,
    private val eventDao: EventDao,
    private val savedGoogleAccountDao: SavedGoogleAccountDao? = null
) {
    suspend fun getEvents(accessToken: String, forceRefresh: Boolean = false): List<EventEntity> {
        val local = eventDao.getEvents()

        if (local.isNotEmpty() && !forceRefresh) {
            return local
        }

        val allEvents = mutableListOf<EventEntity>()
        var pageToken: String? = null

        do {
            val response = service.getEvents(
                authToken = "Bearer $accessToken",
                pageToken = pageToken
            )

            allEvents.addAll(response.items.map { it.toEntity() })
            pageToken = response.nextPageToken
        } while (pageToken != null)

        eventDao.deleteAllEvents()
        eventDao.insertEvents(allEvents)

        return eventDao.getEvents()
    }

    suspend fun getEventById(id: String): EventEntity? {
        return eventDao.getEventById(id)
    }

    suspend fun fetchAndCacheEventDetail(id: String, accessToken: String): EventEntity? {
        val event = service.getEventById(id, "Bearer $accessToken")
        val entity = event.toEntity()

        eventDao.insertEvent(entity)
        return eventDao.getEventById(id)
    }

    suspend fun createCalendarEvent(
        accessToken: String,
        summary: String,
        location: String?,
        description: String?,
        startDateTime: String,
        endDateTime: String,
        recurrence: List<String>?,
        reminders: EventReminders?,
        isAllDay: Boolean,
        timeZone: String = "America/New_York"
    ): EventEntity {
        val createdEvent = service.createEvent(
            authToken = "Bearer $accessToken",
            event = EventWriteRequest(
                summary = summary.ifBlank { "No title" },
                location = location,
                description = description,
                start = if (isAllDay) {
                    EventDateTime(date = startDateTime)
                } else {
                    EventDateTime(
                        dateTime = startDateTime,
                        timeZone = timeZone
                    )
                },
                end = if (isAllDay) {
                    EventDateTime(date = endDateTime)
                } else {
                    EventDateTime(
                        dateTime = endDateTime,
                        timeZone = timeZone
                    )
                },
                recurrence = recurrence,
                reminders = reminders
            )
        )

        val entity = createdEvent.toEntity()
        eventDao.insertEvent(entity)
        return entity
    }

    suspend fun updateCalendarEvent(
        accessToken: String,
        eventId: String,
        recurringEventId: String?,
        applyToSeries: Boolean,
        summary: String,
        location: String?,
        description: String?,
        startDateTime: String,
        endDateTime: String,
        recurrence: List<String>?,
        reminders: EventReminders?,
        wasAllDay: Boolean,
        isAllDay: Boolean,
        timeZone: String = "America/New_York"
    ): EventEntity {
        val targetEventId = if (applyToSeries && !recurringEventId.isNullOrBlank()) {
            recurringEventId
        } else {
            eventId
        }

        val request = EventWriteRequest(
            summary = summary.ifBlank { "No title" },
            location = location,
            description = description,
            start = if (isAllDay) {
                EventDateTime(date = startDateTime)
            } else {
                EventDateTime(
                    dateTime = startDateTime,
                    timeZone = timeZone
                )
            },
            end = if (isAllDay) {
                EventDateTime(date = endDateTime)
            } else {
                EventDateTime(
                    dateTime = endDateTime,
                    timeZone = timeZone
                )
            },
            recurrence = if (applyToSeries) recurrence else null,
            reminders = reminders
        )

        val updatedEvent = if (wasAllDay != isAllDay) {
            service.replaceEvent(
                eventId = targetEventId,
                authToken = "Bearer $accessToken",
                event = request
            )
        } else {
            service.updateEvent(
                eventId = targetEventId,
                authToken = "Bearer $accessToken",
                event = request
            )
        }

        val entity = updatedEvent.toEntity()
        eventDao.insertEvent(entity)

        return entity
    }

    suspend fun deleteCalendarEvent(
        accessToken: String,
        eventId: String,
        recurringEventId: String? = null
    ) {
        val targetEventId = recurringEventId ?: eventId

        val response = service.deleteEvent(
            eventId = targetEventId,
            authToken = "Bearer $accessToken"
        )

        if (!response.isSuccessful && response.code() != 404) {
            throw Exception("Failed to delete event: ${response.code()}")
        }

        eventDao.deleteEventById(eventId)

        if (targetEventId != eventId) {
            eventDao.deleteEventById(targetEventId)
        }
    }

    suspend fun clearLocalCalendarData() {
        eventDao.deleteAllEvents()
    }

    suspend fun getSavedAccounts(): List<String> {
        return savedGoogleAccountDao?.getSavedAccounts()?.map { it.email } ?: emptyList()
    }

    suspend fun getSwitchableAccounts(): List<String> {
        return savedGoogleAccountDao?.getSwitchableAccounts()?.map { it.email } ?: emptyList()
    }

    suspend fun saveLoggedInAccount(email: String) {
        savedGoogleAccountDao?.upsertAccount(
            SavedGoogleAccountEntity(
                email = email,
                isLoggedOut = false,
                isRevoked = false,
                lastUsedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markAccountLoggedOut(email: String) {
        savedGoogleAccountDao?.markLoggedOut(email)
    }

    suspend fun markAccountRevoked(email: String) {
        savedGoogleAccountDao?.markRevoked(email)
    }

    private fun Event.toEntity(): EventEntity {
        return EventEntity(
            id = id,
            summary = summary ?: "No title",
            start = start?.dateTime ?: start?.date ?: "No start time",
            end = end?.dateTime ?: end?.date ?: "No end time",
            creatorName = creator?.displayName ?: creator?.email ?: "Unknown creator",
            location = location,
            recurrence = recurrenceLabel(recurrence, recurringEventId),
            reminders = remindersLabel(reminders),
            recurringEventId = recurringEventId
        )
    }

    private fun recurrenceLabel(
        recurrence: List<String>?,
        recurringEventId: String?
    ): String {
        return when {
            !recurrence.isNullOrEmpty() -> recurrence.joinToString("\n")
            !recurringEventId.isNullOrBlank() -> "Part of a recurring series"
            else -> "Does not repeat"
        }
    }

    private fun remindersLabel(reminders: EventReminders?): String {
        if (reminders == null) return "Calendar default reminders"
        if (reminders.useDefault) return "Calendar default reminders"

        val overrides = reminders.overrides.orEmpty()

        if (overrides.isEmpty()) {
            return "No reminders"
        }

        return overrides.joinToString("\n") { reminder ->
            "${reminder.method}: ${reminder.minutes} minutes before"
        }
    }
}