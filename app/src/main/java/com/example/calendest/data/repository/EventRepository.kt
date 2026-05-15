package com.example.calendest.data.repository

import com.example.calendest.DeleteEventMode
import com.example.calendest.data.local.dao.EventDao
import com.example.calendest.data.local.dao.SavedGoogleAccountDao
import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.local.entity.SavedGoogleAccountEntity
import com.example.calendest.data.model.Event
import com.example.calendest.data.model.EventDateTime
import com.example.calendest.data.model.EventReminderOverride
import com.example.calendest.data.model.EventReminders
import com.example.calendest.data.model.EventWriteRequest
import com.example.calendest.data.network.ApiService
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EventRepository(
    private val service: ApiService,
    private val eventDao: EventDao,
    private val savedGoogleAccountDao: SavedGoogleAccountDao? = null
) {

    suspend fun getEvents(
        accessToken: String,
        forceRefresh: Boolean = false
    ): List<EventEntity> {
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

    suspend fun fetchAndCacheEventDetail(
        id: String,
        accessToken: String
    ): EventEntity? {
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
        recurringEventId: String? = null,
        eventStart: String? = null,
        deleteMode: DeleteEventMode = DeleteEventMode.ONLY_THIS_EVENT
    ) {
        when (deleteMode) {
            DeleteEventMode.ONLY_THIS_EVENT -> {
                val response = service.deleteEvent(
                    eventId = eventId,
                    authToken = "Bearer $accessToken"
                )

                if (!response.isSuccessful) {
                    throw Exception("Failed to delete event: ${response.code()}")
                }
            }

            DeleteEventMode.ALL_EVENTS -> {
                val targetId = recurringEventId ?: eventId

                val response = service.deleteEvent(
                    eventId = targetId,
                    authToken = "Bearer $accessToken"
                )

                if (!response.isSuccessful) {
                    throw Exception("Failed to delete recurring series: ${response.code()}")
                }
            }

            DeleteEventMode.THIS_AND_FUTURE_EVENTS -> {
                val seriesId = recurringEventId
                    ?: throw Exception("This event is not part of a recurring series.")

                val start = eventStart
                    ?: throw Exception("Missing event start time.")

                val selectedStartMillis = parseEventStartMillis(start)

                val series = service.getEventById(
                    eventId = seriesId,
                    authToken = "Bearer $accessToken"
                )

                val updatedRecurrence = series.recurrence
                    ?.mapNotNull { rule ->
                        when {
                            rule.startsWith("RRULE:") -> {
                                replaceRRuleEndWithUntil(
                                    rule = rule,
                                    until = untilBeforeStart(start)
                                )
                            }

                            rule.startsWith("RDATE") -> {
                                trimRDateRuleBeforeMillis(
                                    rule = rule,
                                    cutoffMillis = selectedStartMillis
                                )
                            }

                            else -> rule
                        }
                    }
                    ?: emptyList()

                service.updateEvent(
                    eventId = seriesId,
                    authToken = "Bearer $accessToken",
                    event = EventWriteRequest(
                        summary = series.summary ?: "No title",
                        location = series.location,
                        description = series.description,
                        start = series.start ?: EventDateTime(),
                        end = series.end ?: EventDateTime(),
                        recurrence = updatedRecurrence,
                        reminders = series.reminders
                    )
                )
            }
        }

        eventDao.deleteAllEvents()
    }

    suspend fun clearLocalCalendarData() {
        eventDao.deleteAllEvents()
    }

    suspend fun getSavedAccounts(): List<String> {
        return savedGoogleAccountDao?.getSavedAccounts()?.map { it.email }
            ?: emptyList()
    }

    suspend fun getSwitchableAccounts(): List<String> {
        return savedGoogleAccountDao?.getSwitchableAccounts()?.map { it.email }
            ?: emptyList()
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

    private fun replaceRRuleEndWithUntil(
        rule: String,
        until: String
    ): String {
        val withoutUntil = rule
            .replace(Regex(";UNTIL=[^;]+"), "")
            .replace(Regex(";COUNT=[^;]+"), "")

        return "$withoutUntil;UNTIL=$until"
    }

    private fun untilBeforeStart(start: String): String {
        return try {
            if (start.contains("T")) {
                val parser = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    Locale.US
                )

                val utcFormatter = SimpleDateFormat(
                    "yyyyMMdd'T'HHmmss'Z'",
                    Locale.US
                )

                utcFormatter.timeZone = TimeZone.getTimeZone("UTC")

                val date = parser.parse(start)!!

                val millis = date.time - 1000L

                utcFormatter.format(millis)
            } else {
                val parser = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

                val formatter = SimpleDateFormat(
                    "yyyyMMdd'T'000000'Z'",
                    Locale.US
                )

                formatter.timeZone = TimeZone.getTimeZone("UTC")

                val date = parser.parse(start)!!

                val millis = date.time - 1000L

                formatter.format(millis)
            }
        } catch (e: Exception) {
            throw Exception("Failed to calculate UNTIL date.")
        }
    }

    private fun parseEventStartMillis(start: String): Long {
        return if (start.contains("T")) {
            val parser = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                Locale.US
            )

            parser.parse(start)!!.time
        } else {
            val parser = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )

            parser.timeZone = TimeZone.getDefault()
            parser.parse(start)!!.time
        }
    }

    private fun trimRDateRuleBeforeMillis(
        rule: String,
        cutoffMillis: Long
    ): String? {
        val prefix = rule.substringBefore(":", missingDelimiterValue = "")
        val dateList = rule.substringAfter(":", missingDelimiterValue = "")

        if (prefix.isBlank() || dateList.isBlank()) {
            return null
        }

        val timezoneId = Regex("""TZID=([^:;]+)""")
            .find(prefix)
            ?.groupValues
            ?.getOrNull(1)
            ?: TimeZone.getDefault().id

        val timeZone = TimeZone.getTimeZone(timezoneId)

        val effectiveCutoffMillis = cutoffMillis - 1_000L

        val keptDates = dateList
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { rdate ->
                val millis = parseRDateMillis(
                    rdate = rdate,
                    timeZone = timeZone
                )

                millis != null && millis < effectiveCutoffMillis
            }

        if (keptDates.isEmpty()) {
            return null
        }

        return "$prefix:${keptDates.joinToString(",")}"
    }

    private fun parseRDateMillis(
        rdate: String,
        timeZone: TimeZone
    ): Long? {
        return try {
            val parser = SimpleDateFormat(
                "yyyyMMdd'T'HHmmss",
                Locale.US
            )

            parser.timeZone = timeZone
            parser.parse(rdate)?.time
        } catch (e: Exception) {
            try {
                val parser = SimpleDateFormat(
                    "yyyyMMdd",
                    Locale.US
                )

                parser.timeZone = timeZone
                parser.parse(rdate)?.time
            } catch (e: Exception) {
                null
            }
        }
    }
}