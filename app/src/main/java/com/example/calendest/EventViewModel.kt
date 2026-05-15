package com.example.calendest

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calendest.auth.CalendarAuthClient
import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.model.EventReminderOverride
import com.example.calendest.data.model.EventReminders
import com.example.calendest.data.mvi.EventsAction
import com.example.calendest.data.mvi.EventsState
import com.example.calendest.data.mvi.eventsReducer
import com.example.calendest.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class DeleteEventMode {
    ONLY_THIS_EVENT,
    THIS_AND_FUTURE_EVENTS,
    ALL_EVENTS
}

class EventViewModel(
    private val repo: EventRepository,
    private val authClient: CalendarAuthClient
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state: StateFlow<EventsState> = _state.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    var recurrenceChoice by mutableStateOf("Does not repeat")
        private set

    var customRecurrenceInterval by mutableStateOf(1)
        private set

    var customRecurrenceUnit by mutableStateOf("days")
        private set

    var customRecurrenceCalendar by mutableStateOf("Gregorian")
        private set

    var customRecurrenceEndType by mutableStateOf("Indefinitely")
        private set

    var customRecurrenceUntilDate by mutableStateOf("")
        private set

    var customRecurrenceCount by mutableStateOf("")
        private set

    var notifications by mutableStateOf<List<EventReminderOverride>>(emptyList())
        private set

    var customNotificationAmount by mutableStateOf(5)
        private set

    var customNotificationUnit by mutableStateOf("minutes")
        private set

    var customNotificationMethod by mutableStateOf("popup")
        private set

    private fun fireAction(action: EventsAction) {
        _state.value = eventsReducer(_state.value, action)
    }

    private fun handleError(
        exception: Exception,
        fallbackMessage: String
    ) {
        if (exception is HttpException && exception.code() == 401) {
            fireAction(EventsAction.SetAuthRequired)
        } else {
            fireAction(
                EventsAction.FetchEventsError(
                    exception.message ?: fallbackMessage
                )
            )
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateRecurrenceChoice(choice: String) {
        recurrenceChoice = choice
    }

    fun updateCustomRecurrence(
        interval: Int,
        unit: String,
        calendar: String,
        endType: String,
        untilDate: String,
        count: String
    ) {
        customRecurrenceInterval = interval
        customRecurrenceUnit = unit
        customRecurrenceCalendar = calendar
        customRecurrenceEndType = endType
        customRecurrenceUntilDate = untilDate
        customRecurrenceCount = count
        recurrenceChoice = "Custom"
    }

    fun addNotification(notification: EventReminderOverride) {
        if (notifications.size >= 5) return

        val alreadyExists = notifications.any {
            it.method == notification.method && it.minutes == notification.minutes
        }

        if (!alreadyExists) {
            notifications = notifications + notification
        }
    }

    fun deleteNotification(notification: EventReminderOverride) {
        notifications = notifications.filterNot {
            it.method == notification.method && it.minutes == notification.minutes
        }
    }

    fun clearNotifications() {
        notifications = emptyList()
    }

    fun updateCustomNotification(
        amount: Int,
        unit: String,
        method: String
    ) {
        customNotificationAmount = amount
        customNotificationUnit = unit
        customNotificationMethod = method

        addNotification(
            EventReminderOverride(
                method = method,
                minutes = convertToMinutes(amount, unit)
            )
        )
    }

    private fun convertToMinutes(
        amount: Int,
        unit: String
    ): Int {
        return when (unit) {
            "minutes" -> amount
            "hours" -> amount * 60
            "days" -> amount * 60 * 24
            "weeks" -> amount * 60 * 24 * 7
            "months" -> amount * 60 * 24 * 30
            "years" -> amount * 60 * 24 * 365
            else -> amount
        }
    }

    fun selectEvent(event: EventEntity) {
        fireAction(EventsAction.UpdateSelectedEvent(event))
    }

    fun refreshHomeScreen(
        accessToken: String = "",
        forceRefresh: Boolean = false
    ) {
        if (accessToken.isNotEmpty()) {
            fireAction(EventsAction.AuthSuccess(accessToken))
        }

        val tokenToUse = state.value.accessToken

        if (tokenToUse.isEmpty()) {
            fireAction(EventsAction.SetAuthRequired)
            return
        }

        fireAction(EventsAction.FetchEvents)

        viewModelScope.launch {
            try {
                val events = repo.getEvents(
                    accessToken = tokenToUse,
                    forceRefresh = forceRefresh
                )

                fireAction(EventsAction.FetchEventsSuccess(events))
            } catch (e: Exception) {
                handleError(e, "Unknown error")
            }
        }
    }

    fun createCalendarEvent(
        summary: String,
        location: String?,
        description: String?,
        startDateTime: String,
        endDateTime: String,
        recurrence: List<String>?,
        reminders: EventReminders?,
        isAllDay: Boolean,
        onCreated: (String) -> Unit = {}
    ) {
        val tokenToUse = state.value.accessToken

        if (tokenToUse.isEmpty()) {
            fireAction(EventsAction.SetAuthRequired)
            return
        }

        fireAction(EventsAction.FetchEvents)

        viewModelScope.launch {
            try {
                val createdEvent = repo.createCalendarEvent(
                    accessToken = tokenToUse,
                    summary = summary,
                    location = location,
                    description = description,
                    startDateTime = startDateTime,
                    endDateTime = endDateTime,
                    recurrence = recurrence,
                    reminders = reminders,
                    isAllDay = isAllDay
                )

                onCreated(createdEvent.id)

                refreshHomeScreen(forceRefresh = true)
            } catch (e: Exception) {
                handleError(e, "Failed to create event.")
            }
        }
    }

    fun updateCalendarEvent(
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
        isAllDay: Boolean
    ) {
        val tokenToUse = state.value.accessToken

        if (tokenToUse.isEmpty()) {
            fireAction(EventsAction.SetAuthRequired)
            return
        }

        fireAction(EventsAction.FetchEvents)

        viewModelScope.launch {
            try {
                repo.updateCalendarEvent(
                    accessToken = tokenToUse,
                    eventId = eventId,
                    recurringEventId = recurringEventId,
                    applyToSeries = applyToSeries,
                    summary = summary,
                    location = location,
                    description = description,
                    startDateTime = startDateTime,
                    endDateTime = endDateTime,
                    recurrence = recurrence,
                    reminders = reminders,
                    wasAllDay = wasAllDay,
                    isAllDay = isAllDay
                )

                refreshHomeScreen(forceRefresh = true)

            } catch (e: Exception) {
                fireAction(
                    EventsAction.FetchEventsError(
                        e.message ?: "Failed to update event."
                    )
                )
            }
        }
    }

    fun deleteCalendarEvent(
        eventId: String,
        recurringEventId: String? = null,
        eventStart: String? = null,
        deleteMode: DeleteEventMode = DeleteEventMode.ONLY_THIS_EVENT
    ) {
        val tokenToUse = state.value.accessToken

        if (tokenToUse.isEmpty()) {
            fireAction(EventsAction.SetAuthRequired)
            return
        }

        fireAction(EventsAction.FetchEvents)

        viewModelScope.launch {
            try {
                repo.deleteCalendarEvent(
                    accessToken = tokenToUse,
                    eventId = eventId,
                    recurringEventId = recurringEventId,
                    eventStart = eventStart,
                    deleteMode = deleteMode
                )

                refreshHomeScreen(forceRefresh = true)
            } catch (e: Exception) {
                handleError(e, "Failed to delete event.")
            }
        }
    }

    fun startAuth(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        fireAction(EventsAction.FetchEvents)

        authClient.startAuth(
            context = context,
            launcher = launcher,
            onTokenReceived = { token ->
                refreshHomeScreen(
                    accessToken = token,
                    forceRefresh = true
                )
            },
            onAuthRequired = {
                fireAction(EventsAction.SetAuthRequired)
            },
            onError = { message ->
                fireAction(EventsAction.FetchEventsError(message))
            }
        )
    }

    fun onAuthCancelled() {
        fireAction(EventsAction.SetAuthRequired)
    }

    fun refreshDetailsScreen() {
        val selectedId = state.value.selectedEvent?.id ?: return
        val token = state.value.accessToken

        viewModelScope.launch {
            try {
                val updatedEvent = if (token.isNotEmpty()) {
                    repo.fetchAndCacheEventDetail(selectedId, token)
                } else {
                    repo.getEventById(selectedId)
                }

                fireAction(EventsAction.UpdateSelectedEvent(updatedEvent))
            } catch (e: Exception) {
                handleError(e, "Failed to refresh event details.")
            }
        }
    }

    fun logout(context: Context) {
        val tokenToUse = state.value.accessToken

        authClient.logout(
            context = context,
            accessToken = tokenToUse,
            onSuccess = {
                viewModelScope.launch {
                    repo.clearLocalCalendarData()
                    fireAction(EventsAction.Logout)
                }
            },
            onError = { message ->
                fireAction(EventsAction.FetchEventsError(message))
            }
        )
    }

    fun switchAccount(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val tokenToUse = state.value.accessToken
        val emailToUse = state.value.currentAccountEmail

        authClient.logout(
            context = context,
            accessToken = tokenToUse,
            onSuccess = {
                viewModelScope.launch {
                    repo.clearLocalCalendarData()

                    if (emailToUse != null) {
                        repo.markAccountLoggedOut(emailToUse)
                    }

                    fireAction(EventsAction.Logout)

                    signInAndAuthorize(
                        context = context,
                        launcher = launcher,
                        filterByAuthorizedAccounts = false
                    )
                }
            },
            onError = { message ->
                fireAction(EventsAction.FetchEventsError(message))
            }
        )
    }

    fun signInAndAuthorize(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        filterByAuthorizedAccounts: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val email = authClient.signInWithGoogle(
                    context = context,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts
                )

                repo.saveLoggedInAccount(email)

                fireAction(
                    EventsAction.AccountSelected(
                        email = email,
                        accessToken = ""
                    )
                )

                fireAction(
                    EventsAction.SavedAccountsLoaded(
                        repo.getSavedAccounts()
                    )
                )

                startAuth(context, launcher)
            } catch (e: Exception) {
                fireAction(
                    EventsAction.FetchEventsError(
                        e.message ?: "Google sign-in failed."
                    )
                )
            }
        }
    }

    fun handleAuthResult(
        context: Context,
        resultData: Intent?
    ) {
        authClient.handleAuthResult(
            context = context,
            resultData = resultData,
            onTokenReceived = { token ->
                val email = state.value.currentAccountEmail

                if (email != null) {
                    fireAction(
                        EventsAction.AccountSelected(
                            email = email,
                            accessToken = token
                        )
                    )
                }

                refreshHomeScreen(
                    accessToken = token,
                    forceRefresh = true
                )
            },
            onError = { message ->
                fireAction(EventsAction.FetchEventsError(message))
            }
        )
    }

    var editingNotificationIndex by mutableStateOf<Int?>(null)
        private set

    fun setNotificationsForEditing(newNotifications: List<EventReminderOverride>) {
        notifications = newNotifications
    }

    fun startEditingNotification(index: Int) {
        editingNotificationIndex = index

        val notification = notifications.getOrNull(index) ?: return

        customNotificationMethod = notification.method

        val minutes = notification.minutes

        when {
            minutes % (60 * 24 * 365) == 0 -> {
                customNotificationAmount = minutes / (60 * 24 * 365)
                customNotificationUnit = "years"
            }

            minutes % (60 * 24 * 30) == 0 -> {
                customNotificationAmount = minutes / (60 * 24 * 30)
                customNotificationUnit = "months"
            }

            minutes % (60 * 24 * 7) == 0 -> {
                customNotificationAmount = minutes / (60 * 24 * 7)
                customNotificationUnit = "weeks"
            }

            minutes % (60 * 24) == 0 -> {
                customNotificationAmount = minutes / (60 * 24)
                customNotificationUnit = "days"
            }

            minutes % 60 == 0 -> {
                customNotificationAmount = minutes / 60
                customNotificationUnit = "hours"
            }

            else -> {
                customNotificationAmount = minutes
                customNotificationUnit = "minutes"
            }
        }
    }

    fun finishEditingNotification(
        amount: Int,
        unit: String,
        method: String
    ) {
        val updatedNotification = EventReminderOverride(
            method = method,
            minutes = convertToMinutes(amount, unit)
        )

        val index = editingNotificationIndex

        notifications = if (index != null && index in notifications.indices) {
            notifications.mapIndexed { i, existing ->
                if (i == index) updatedNotification else existing
            }
        } else {
            if (notifications.size >= 5) {
                notifications
            } else {
                notifications + updatedNotification
            }
        }

        editingNotificationIndex = null
    }

    data class EventEditDraft(
        val eventId: String? = null,
        val isEditing: Boolean = false,
        val isAllDay: Boolean = false,
        val summary: String = "",
        val location: String = "",
        val description: String = "",
        val startDate: String = "",
        val endDate: String = "",
        val startTime: String = "",
        val endTime: String = "",
        val recurrenceChoice: String = "Does not repeat",
        val applyToSeries: Boolean = false
    )

    var eventEditDraft by mutableStateOf<EventEditDraft?>(null)
        private set

    fun startEventDraftIfNeeded(
        eventId: String?,
        existingEvent: EventEntity?,
        existingIsAllDay: Boolean,
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        recurrenceChoice: String
    ) {
        val existingDraft = eventEditDraft

        if (existingDraft != null && existingDraft.eventId == eventId) {
            return
        }

        eventEditDraft = EventEditDraft(
            eventId = eventId,
            isEditing = eventId != null,
            isAllDay = existingIsAllDay,
            summary = existingEvent?.summary ?: "",
            location = existingEvent?.location ?: "",
            description = "",
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            recurrenceChoice = recurrenceChoice
        )
    }

    fun updateEventDraft(update: EventEditDraft.() -> EventEditDraft) {
        eventEditDraft = eventEditDraft?.update()
    }

    fun clearEventDraft() {
        eventEditDraft = null
    }
}

class EventViewModelFactory(
    private val repo: EventRepository,
    private val authClient: CalendarAuthClient
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventViewModel(
                repo = repo,
                authClient = authClient
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}