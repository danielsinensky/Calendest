package com.example.calendest.ui.screens

import android.icu.util.Calendar as IcuCalendar
import android.icu.util.TimeZone as IcuTimeZone
import android.icu.util.ULocale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.calendest.EventViewModel
import com.example.calendest.data.model.EventReminderOverride
import com.example.calendest.data.model.EventReminders
import com.example.calendest.notifications.EventNotificationScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material3.AlertDialog
import com.example.calendest.DeleteEventMode
import com.example.calendest.data.local.entity.EventEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditEventScreen(
    navController: NavController,
    eventViewModel: EventViewModel,
    eventId: String?
) {
    val context = LocalContext.current
    val state by eventViewModel.state.collectAsState()

    val existingEvent = state.events.find { it.id == eventId }
        ?: state.selectedEvent?.takeIf { it.id == eventId }

    val isEditing = eventId != null
    val timeZoneId = "America/New_York"
    val existingIsAllDay = existingEvent?.start?.contains("T") == false

    val initialStartDate = datePartFromEvent(existingEvent?.start)

    val initialEndDate = if (existingIsAllDay) {
        subtractOneDay(datePartFromEvent(existingEvent?.end))
    } else {
        datePartFromEvent(existingEvent?.end)
    }

    val initialStartTime = timePartFromEvent(existingEvent?.start)
    val initialEndTime = timePartFromEvent(existingEvent?.end)

    val initialRecurrenceChoice =
        if (eventViewModel.recurrenceChoice == "Does not repeat") {
            recurrenceChoiceFromLabel(existingEvent?.recurrence)
        } else {
            eventViewModel.recurrenceChoice
        }

    eventViewModel.startEventDraftIfNeeded(
        eventId = eventId,
        existingEvent = existingEvent,
        existingIsAllDay = existingIsAllDay,
        startDate = initialStartDate,
        endDate = initialEndDate,
        startTime = initialStartTime,
        endTime = initialEndTime,
        recurrenceChoice = initialRecurrenceChoice
    )

    val draft = eventViewModel.eventEditDraft ?: return

    var activeDatePicker by remember { mutableStateOf<String?>(null) }
    var startTimeExpanded by remember { mutableStateOf(false) }
    var endTimeExpanded by remember { mutableStateOf(false) }
    var notificationMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val monthlyByWeekdayOption = monthlyByWeekdayLabel(draft.startDate)

    val notificationOptions = presetNotificationOptions().filterNot { option ->
        option.override != null && eventViewModel.notifications.any {
            it.method == option.override.method && it.minutes == option.override.minutes
        }
    }

    activeDatePicker?.let { picker ->
        CalendarDatePickerDialog(
            initialDate = if (picker == "start") draft.startDate else draft.endDate,
            onDateSelected = { selectedDate ->
                if (picker == "start") {
                    eventViewModel.updateEventDraft {
                        copy(startDate = selectedDate)
                    }
                } else {
                    eventViewModel.updateEventDraft {
                        copy(endDate = selectedDate)
                    }
                }

                activeDatePicker = null
            },
            onDismiss = {
                activeDatePicker = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = if (isEditing) "Edit Event" else "Create Event")

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text("All-day event")

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = draft.isAllDay,
                onCheckedChange = { checked ->
                    eventViewModel.updateEventDraft {
                        copy(
                            isAllDay = checked,
                            startTime = if (checked) "" else startTime,
                            endTime = if (checked) "" else endTime
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = draft.summary,
            onValueChange = { value ->
                eventViewModel.updateEventDraft {
                    copy(summary = value)
                }
            },
            label = { Text("Summary / Title *") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = draft.location,
            onValueChange = { value ->
                eventViewModel.updateEventDraft {
                    copy(location = value)
                }
            },
            label = { Text("Location optional") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = draft.description,
            onValueChange = { value ->
                eventViewModel.updateEventDraft {
                    copy(description = value)
                }
            },
            label = { Text("Description optional") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Start")

        Text(
            text = draft.startDate.ifBlank { "Select start date *" },
            modifier = Modifier
                .clickable { activeDatePicker = "start" }
                .padding(8.dp)
        )

        if (!draft.isAllDay) {
            Text(
                text = draft.startTime.ifBlank { "Select start time *" },
                modifier = Modifier
                    .clickable { startTimeExpanded = true }
                    .padding(8.dp)
            )

            DropdownMenu(
                expanded = startTimeExpanded,
                onDismissRequest = { startTimeExpanded = false }
            ) {
                timeOptions().forEach { time ->
                    DropdownMenuItem(
                        text = { Text(time) },
                        onClick = {
                            eventViewModel.updateEventDraft {
                                copy(startTime = time)
                            }
                            startTimeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("End")

        Text(
            text = draft.endDate.ifBlank { "Select end date *" },
            modifier = Modifier
                .clickable { activeDatePicker = "end" }
                .padding(8.dp)
        )

        if (!draft.isAllDay) {
            Text(
                text = draft.endTime.ifBlank { "Select end time *" },
                modifier = Modifier
                    .clickable { endTimeExpanded = true }
                    .padding(8.dp)
            )

            DropdownMenu(
                expanded = endTimeExpanded,
                onDismissRequest = { endTimeExpanded = false }
            ) {
                timeOptions().forEach { time ->
                    DropdownMenuItem(
                        text = { Text(time) },
                        onClick = {
                            eventViewModel.updateEventDraft {
                                copy(endTime = time)
                            }
                            endTimeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Recurrence")

        RecurrenceOption(
            label = "Does not repeat",
            selected = draft.recurrenceChoice == "Does not repeat",
            onClick = {
                eventViewModel.updateEventDraft {
                    copy(recurrenceChoice = "Does not repeat")
                }
                eventViewModel.updateRecurrenceChoice("Does not repeat")
            }
        )

        RecurrenceOption(
            label = "Daily",
            selected = draft.recurrenceChoice == "Daily",
            onClick = {
                eventViewModel.updateEventDraft {
                    copy(recurrenceChoice = "Daily")
                }
                eventViewModel.updateRecurrenceChoice("Daily")
            }
        )

        RecurrenceOption(
            label = "Weekly",
            selected = draft.recurrenceChoice == "Weekly",
            onClick = {
                eventViewModel.updateEventDraft {
                    copy(recurrenceChoice = "Weekly")
                }
                eventViewModel.updateRecurrenceChoice("Weekly")
            }
        )

        RecurrenceOption(
            label = "Monthly",
            selected = draft.recurrenceChoice == "Monthly",
            onClick = {
                eventViewModel.updateEventDraft {
                    copy(recurrenceChoice = "Monthly")
                }
                eventViewModel.updateRecurrenceChoice("Monthly")
            }
        )

        RecurrenceOption(
            label = monthlyByWeekdayOption,
            selected = draft.recurrenceChoice == monthlyByWeekdayOption,
            onClick = {
                eventViewModel.updateEventDraft {
                    copy(recurrenceChoice = monthlyByWeekdayOption)
                }
                eventViewModel.updateRecurrenceChoice(monthlyByWeekdayOption)
            }
        )

        RecurrenceOption(
            label = "Yearly",
            selected = draft.recurrenceChoice == "Yearly",
            onClick = {
                eventViewModel.updateEventDraft {
                    copy(recurrenceChoice = "Yearly")
                }
                eventViewModel.updateRecurrenceChoice("Yearly")
            }
        )

        RecurrenceOption(
            label = "Custom",
            selected = draft.recurrenceChoice == "Custom",
            onClick = {
                eventViewModel.updateEventDraft {
                    copy(recurrenceChoice = "Custom")
                }
                eventViewModel.updateRecurrenceChoice("Custom")
                navController.navigate("recurrenceCustomEditScreen")
            }
        )

        if (draft.recurrenceChoice == "Custom") {
            Text(
                text = "Custom recurrence: every ${eventViewModel.customRecurrenceInterval} " +
                        unitLabel(
                            eventViewModel.customRecurrenceInterval,
                            eventViewModel.customRecurrenceUnit
                        ) +
                        ", ${eventViewModel.customRecurrenceCalendar}, " +
                        eventViewModel.customRecurrenceEndType,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (isAlternateCalendar(eventViewModel.customRecurrenceCalendar)) {
                Text(
                    text = "Alternate calendar recurrence will be saved as generated recurrence dates.",
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Notifications")

        if (eventViewModel.notifications.isEmpty()) {
            Text(
                text = "No notifications",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        eventViewModel.notifications.forEachIndexed { index, notification ->
            Column(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(text = notificationLabel(notification))

                Row {
                    Text(
                        text = "Edit",
                        modifier = Modifier
                            .clickable {
                                eventViewModel.startEditingNotification(index)
                                navController.navigate("notificationCustomEditScreen")
                            }
                            .padding(end = 16.dp)
                    )

                    Text(
                        text = "Delete",
                        modifier = Modifier.clickable {
                            eventViewModel.deleteNotification(notification)
                        }
                    )
                }
            }
        }

        if (eventViewModel.notifications.size < 5) {
            Button(
                onClick = {
                    notificationMenuExpanded = true
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Add Notification")
            }

            DropdownMenu(
                expanded = notificationMenuExpanded,
                onDismissRequest = {
                    notificationMenuExpanded = false
                }
            ) {
                notificationOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            notificationMenuExpanded = false

                            when {
                                option.label == "Custom" -> {
                                    navController.navigate("notificationCustomEditScreen")
                                }

                                option.label == "None" -> {
                                    eventViewModel.clearNotifications()
                                }

                                option.override != null -> {
                                    eventViewModel.addNotification(option.override)
                                }
                            }
                        }
                    )
                }
            }
        } else {
            Text("Maximum of 5 notifications reached.")
        }

        if (isEditing && !existingEvent?.recurringEventId.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Apply non-recurrence changes to")

            Text(
                text = if (!draft.applyToSeries) "• This event only" else "This event only",
                modifier = Modifier
                    .clickable {
                        eventViewModel.updateEventDraft {
                            copy(applyToSeries = false)
                        }
                    }
                    .padding(vertical = 4.dp)
            )

            Text(
                text = if (draft.applyToSeries) "• Entire recurring series" else "Entire recurring series",
                modifier = Modifier
                    .clickable {
                        eventViewModel.updateEventDraft {
                            copy(applyToSeries = true)
                        }
                    }
                    .padding(vertical = 4.dp)
            )

            Text(
                text = "Recurrence changes always apply to the entire recurring series.",
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (draft.summary.isBlank()) {
                    errorMessage = "Summary is required."
                    return@Button
                }

                if (draft.startDate.isBlank() || draft.endDate.isBlank()) {
                    errorMessage = "Start and end dates are required."
                    return@Button
                }

                if (!draft.isAllDay && (draft.startTime.isBlank() || draft.endTime.isBlank())) {
                    errorMessage = "Start and end times are required."
                    return@Button
                }

                if (isDateBefore(draft.endDate, draft.startDate)) {
                    errorMessage = "End date cannot come before start date."
                    return@Button
                }

                if (
                    !draft.isAllDay &&
                    draft.startDate == draft.endDate &&
                    isTimeBefore(draft.endTime, draft.startTime)
                ) {
                    errorMessage = "End time cannot come before start time."
                    return@Button
                }

                val startDateTime = if (draft.isAllDay) {
                    draft.startDate
                } else {
                    buildGoogleDateTime(
                        date = draft.startDate,
                        timeLabel = draft.startTime,
                        timeZoneId = timeZoneId
                    )
                }

                val endDateTime = if (draft.isAllDay) {
                    addOneDay(draft.endDate)
                } else {
                    buildGoogleDateTime(
                        date = draft.endDate,
                        timeLabel = draft.endTime,
                        timeZoneId = timeZoneId
                    )
                }

                val recurrence = buildRecurrence(
                    choice = draft.recurrenceChoice,
                    startDate = draft.startDate,
                    startTime = if (draft.isAllDay) "12:00 AM" else draft.startTime,
                    timeZoneId = timeZoneId,
                    customInterval = eventViewModel.customRecurrenceInterval,
                    customUnit = eventViewModel.customRecurrenceUnit,
                    customCalendar = eventViewModel.customRecurrenceCalendar,
                    customEndType = eventViewModel.customRecurrenceEndType,
                    customUntilDate = eventViewModel.customRecurrenceUntilDate,
                    customCount = eventViewModel.customRecurrenceCount
                )

                val reminders = EventReminders(
                    useDefault = false,
                    overrides = eventViewModel.notifications
                )

                if (isEditing && eventId != null) {
                    val recurrenceWasEdited =
                        draft.recurrenceChoice != recurrenceChoiceFromLabel(existingEvent?.recurrence)

                    val shouldApplyToSeries = when {
                        recurrenceWasEdited -> true
                        recurrence != null -> true
                        draft.applyToSeries -> true
                        else -> false
                    }

                    eventViewModel.updateCalendarEvent(
                        eventId = eventId,
                        recurringEventId = existingEvent?.recurringEventId,
                        applyToSeries = shouldApplyToSeries,
                        summary = draft.summary,
                        location = draft.location.ifBlank { null },
                        description = draft.description.ifBlank { null },
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                        recurrence = recurrence,
                        reminders = reminders,
                        wasAllDay = existingIsAllDay,
                        isAllDay = draft.isAllDay
                    )

                    EventNotificationScheduler.cancelEventNotifications(
                        context = context,
                        eventId = eventId,
                        reminderMinutes = reminders.overrides?.map { it.minutes }.orEmpty()
                    )

                    EventNotificationScheduler.scheduleEventNotifications(
                        context = context,
                        eventId = eventId,
                        eventTitle = draft.summary,
                        eventStart = startDateTime,
                        isAllDay = draft.isAllDay,
                        reminders = reminders.overrides ?: emptyList()
                    )
                } else {
                    eventViewModel.createCalendarEvent(
                        summary = draft.summary,
                        location = draft.location.ifBlank { null },
                        description = draft.description.ifBlank { null },
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                        recurrence = recurrence,
                        reminders = reminders,
                        isAllDay = draft.isAllDay,
                        onCreated = { createdEventId ->
                            EventNotificationScheduler.scheduleEventNotifications(
                                context = context,
                                eventId = createdEventId,
                                eventTitle = draft.summary,
                                eventStart = startDateTime,
                                isAllDay = draft.isAllDay,
                                reminders = reminders.overrides ?: emptyList()
                            )
                        }
                    )
                }

                eventViewModel.clearEventDraft()

                navController.navigate("homeScreen") {
                    popUpTo("homeScreen") {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        ) {
            Text(text = if (isEditing) "Update Event" else "Create Event")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isEditing && existingEvent != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (eventHasRecurrence(existingEvent)) {
                        showDeleteDialog = true
                    } else {
                        eventViewModel.deleteCalendarEvent(
                            eventId = existingEvent.id,
                            eventStart = existingEvent.start
                        )

                        eventViewModel.clearEventDraft()

                        navController.navigate("homeScreen") {
                            popUpTo("homeScreen") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                }
            ) {
                Text("Delete Event")
            }
        }

        Button(
            onClick = {
                eventViewModel.clearEventDraft()

                navController.navigate("homeScreen") {
                    popUpTo("homeScreen") {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        ) {
            Text("Cancel")
        }

        if (showDeleteDialog && existingEvent != null) {
            RecurringDeleteDialog(
                onDeleteOnlyThis = {
                    showDeleteDialog = false

                    eventViewModel.deleteCalendarEvent(
                        eventId = existingEvent.id,
                        recurringEventId = existingEvent.recurringEventId,
                        eventStart = existingEvent.start,
                        deleteMode = DeleteEventMode.ONLY_THIS_EVENT
                    )

                    eventViewModel.clearEventDraft()

                    navController.navigate("homeScreen") {
                        popUpTo("homeScreen") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onDeleteThisAndFuture = {
                    showDeleteDialog = false

                    eventViewModel.deleteCalendarEvent(
                        eventId = existingEvent.id,
                        recurringEventId = existingEvent.recurringEventId,
                        eventStart = existingEvent.start,
                        deleteMode = DeleteEventMode.THIS_AND_FUTURE_EVENTS
                    )

                    eventViewModel.clearEventDraft()

                    navController.navigate("homeScreen") {
                        popUpTo("homeScreen") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onDeleteAll = {
                    showDeleteDialog = false

                    eventViewModel.deleteCalendarEvent(
                        eventId = existingEvent.id,
                        recurringEventId = existingEvent.recurringEventId,
                        eventStart = existingEvent.start,
                        deleteMode = DeleteEventMode.ALL_EVENTS
                    )

                    eventViewModel.clearEventDraft()

                    navController.navigate("homeScreen") {
                        popUpTo("homeScreen") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }
    }
}

@Composable
private fun RecurrenceOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = if (selected) "• $label" else label,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    )
}

private data class NotificationOption(
    val label: String,
    val override: EventReminderOverride?
)

private fun presetNotificationOptions(): List<NotificationOption> {
    return listOf(
        NotificationOption("None", null),
        NotificationOption("5 minutes before", EventReminderOverride("popup", 5)),
        NotificationOption("10 minutes before", EventReminderOverride("popup", 10)),
        NotificationOption("15 minutes before", EventReminderOverride("popup", 15)),
        NotificationOption("30 minutes before", EventReminderOverride("popup", 30)),
        NotificationOption("1 hour before", EventReminderOverride("popup", 60)),
        NotificationOption("90 minutes before", EventReminderOverride("popup", 90)),
        NotificationOption("2 hours before", EventReminderOverride("popup", 120)),
        NotificationOption("1 day before by email", EventReminderOverride("email", 1440)),
        NotificationOption("Custom", null)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateToUtcMillis(initialDate)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(utcMillisToDate(it))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun RecurringDeleteDialog(
    onDeleteOnlyThis: () -> Unit,
    onDeleteThisAndFuture: () -> Unit,
    onDeleteAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete recurring event")
        },
        text = {
            Text("What do you want to delete?")
        },
        confirmButton = {
            Column {
                Button(onClick = onDeleteOnlyThis) {
                    Text("Only this event")
                }

                Button(onClick = onDeleteThisAndFuture) {
                    Text("This and future events")
                }

                Button(onClick = onDeleteAll) {
                    Text("All occurrences")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun timeOptions(): List<String> {
    val times = mutableListOf<String>()
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val calendar = Calendar.getInstance()

    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    repeat(96) {
        times.add(formatter.format(calendar.time))
        calendar.add(Calendar.MINUTE, 15)
    }

    return times
}

private fun buildGoogleDateTime(
    date: String,
    timeLabel: String,
    timeZoneId: String
): String {
    val timeZone = TimeZone.getTimeZone(timeZoneId)
    val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeParser = SimpleDateFormat("h:mm a", Locale.getDefault())

    dateParser.timeZone = timeZone
    timeParser.timeZone = timeZone

    val dateCalendar = Calendar.getInstance(timeZone)
    dateCalendar.time = dateParser.parse(date)!!

    val timeCalendar = Calendar.getInstance(timeZone)
    timeCalendar.time = timeParser.parse(timeLabel)!!

    dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
    dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
    dateCalendar.set(Calendar.SECOND, 0)
    dateCalendar.set(Calendar.MILLISECOND, 0)

    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    formatter.timeZone = timeZone

    return formatter.format(dateCalendar.time)
}

private fun buildRecurrence(
    choice: String,
    startDate: String,
    startTime: String,
    timeZoneId: String,
    customInterval: Int,
    customUnit: String,
    customCalendar: String,
    customEndType: String,
    customUntilDate: String,
    customCount: String
): List<String>? {
    if (choice == "Does not repeat") {
        return emptyList()
    }

    if (choice == "Custom" && isAlternateCalendar(customCalendar)) {
        return buildAlternateCalendarRecurrence(
            startDate = startDate,
            startTime = startTime,
            timeZoneId = timeZoneId,
            interval = customInterval,
            unit = customUnit,
            calendarName = customCalendar,
            endType = customEndType,
            untilDate = customUntilDate,
            countText = customCount
        )
    }

    val base = when {
        choice == "Daily" -> "RRULE:FREQ=DAILY"
        choice == "Weekly" -> "RRULE:FREQ=WEEKLY"
        choice == "Monthly" -> "RRULE:FREQ=MONTHLY"
        choice == "Yearly" -> "RRULE:FREQ=YEARLY"
        choice.startsWith("Every") -> monthlyByWeekdayRRule(startDate)
        choice == "Custom" -> customRRule(customInterval, customUnit)
        else -> null
    } ?: return null

    val ending = when (customEndType) {
        "Until date" -> {
            if (customUntilDate.isNotBlank()) {
                ";UNTIL=${customUntilDate.replace("-", "")}T235959Z"
            } else {
                ""
            }
        }

        "After number of occurrences" -> {
            val count = customCount.toIntOrNull()
            if (count != null && count > 0) ";COUNT=$count" else ""
        }

        else -> ""
    }

    return listOf(base + ending)
}

private fun buildAlternateCalendarRecurrence(
    startDate: String,
    startTime: String,
    timeZoneId: String,
    interval: Int,
    unit: String,
    calendarName: String,
    endType: String,
    untilDate: String,
    countText: String
): List<String> {
    val timeZone = TimeZone.getTimeZone(timeZoneId)

    val startDateTime = parseDateAndTime(
        date = startDate,
        timeLabel = startTime,
        timeZone = timeZone
    ) ?: return emptyList()

    val icuCalendar = createAlternateCalendar(calendarName, timeZone)
    icuCalendar.time = startDateTime

    val originalDayOfMonth = icuCalendar.get(IcuCalendar.DAY_OF_MONTH)
    val originalMonth = icuCalendar.get(IcuCalendar.MONTH)
    val originalHour = icuCalendar.get(IcuCalendar.HOUR_OF_DAY)
    val originalMinute = icuCalendar.get(IcuCalendar.MINUTE)

    val maxOccurrences = when (endType) {
        "After number of occurrences" -> countText.toIntOrNull()?.coerceIn(1, 500) ?: 20
        "Until date" -> 500
        else -> 120
    }

    val untilMillis = if (endType == "Until date" && untilDate.isNotBlank()) {
        parseDateAndTime(
            date = untilDate,
            timeLabel = "11:59 PM",
            timeZone = timeZone
        )?.time
    } else {
        null
    }

    val outputFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
    outputFormatter.timeZone = timeZone

    val rdates = mutableListOf<String>()

    repeat(maxOccurrences) { index ->
        if (index > 0) {
            addAlternateCalendarInterval(
                calendar = icuCalendar,
                unit = unit,
                interval = interval.coerceAtLeast(1)
            )
        }

        if (unit == "months" || unit == "years") {
            icuCalendar.set(IcuCalendar.DAY_OF_MONTH, originalDayOfMonth)

            if (unit == "years") {
                icuCalendar.set(IcuCalendar.MONTH, originalMonth)
            }
        }

        icuCalendar.set(IcuCalendar.HOUR_OF_DAY, originalHour)
        icuCalendar.set(IcuCalendar.MINUTE, originalMinute)
        icuCalendar.set(IcuCalendar.SECOND, 0)
        icuCalendar.set(IcuCalendar.MILLISECOND, 0)

        val occurrenceDate = icuCalendar.time

        if (untilMillis != null && occurrenceDate.time > untilMillis) {
            return@repeat
        }

        rdates.add(outputFormatter.format(occurrenceDate))
    }

    if (rdates.isEmpty()) {
        return emptyList()
    }

    return listOf("RDATE;TZID=$timeZoneId:${rdates.joinToString(",")}")
}

private fun createAlternateCalendar(
    calendarName: String,
    timeZone: TimeZone
): IcuCalendar {
    val icuTimeZone = IcuTimeZone.getTimeZone(timeZone.id)

    val locale = when (calendarName) {
        "Chinese calendar - Simplified" -> ULocale("zh_CN@calendar=chinese")
        "Chinese calendar - Traditional" -> ULocale("zh_TW@calendar=chinese")
        "Hebrew calendar" -> ULocale("he_IL@calendar=hebrew")
        "Hijri calendar - Civil" -> ULocale("ar@calendar=islamic-civil")
        "Hijri calendar - Kuwaiti" -> ULocale("ar@calendar=islamic")
        "Hijri calendar - Saudi" -> ULocale("ar_SA@calendar=islamic-umalqura")
        "Indian calendar - Hindu (Saka)" -> ULocale("hi_IN@calendar=indian")
        "Persian calendar" -> ULocale("fa_IR@calendar=persian")
        else -> ULocale.getDefault()
    }

    return IcuCalendar.getInstance(icuTimeZone, locale)
}

private fun addAlternateCalendarInterval(
    calendar: IcuCalendar,
    unit: String,
    interval: Int
) {
    when (unit) {
        "minutes" -> calendar.add(IcuCalendar.MINUTE, interval)
        "hours" -> calendar.add(IcuCalendar.HOUR_OF_DAY, interval)
        "days" -> calendar.add(IcuCalendar.DATE, interval)
        "weeks" -> calendar.add(IcuCalendar.DATE, interval * 7)
        "months" -> calendar.add(IcuCalendar.MONTH, interval)
        "years" -> calendar.add(IcuCalendar.YEAR, interval)
        else -> calendar.add(IcuCalendar.DATE, interval)
    }
}

private fun parseDateAndTime(
    date: String,
    timeLabel: String,
    timeZone: TimeZone
): Date? {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
        parser.timeZone = timeZone
        parser.parse("$date $timeLabel")
    } catch (e: Exception) {
        null
    }
}

private fun isAlternateCalendar(calendarName: String): Boolean {
    return calendarName in listOf(
        "Chinese calendar - Simplified",
        "Chinese calendar - Traditional",
        "Hebrew calendar",
        "Hijri calendar - Civil",
        "Hijri calendar - Kuwaiti",
        "Hijri calendar - Saudi",
        "Indian calendar - Hindu (Saka)",
        "Persian calendar"
    )
}

private fun customRRule(
    interval: Int,
    unit: String
): String {
    val freq = when (unit) {
        "minutes" -> "MINUTELY"
        "hours" -> "HOURLY"
        "days" -> "DAILY"
        "weeks" -> "WEEKLY"
        "months" -> "MONTHLY"
        "years" -> "YEARLY"
        else -> "DAILY"
    }

    return "RRULE:FREQ=$freq;INTERVAL=${interval.coerceAtLeast(1)}"
}

private fun monthlyByWeekdayRRule(date: String): String {
    val calendar = Calendar.getInstance()
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    calendar.time = parser.parse(date) ?: return "RRULE:FREQ=MONTHLY"

    val weekNumber = ((calendar.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1

    val byDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "MO"
        Calendar.TUESDAY -> "TU"
        Calendar.WEDNESDAY -> "WE"
        Calendar.THURSDAY -> "TH"
        Calendar.FRIDAY -> "FR"
        Calendar.SATURDAY -> "SA"
        Calendar.SUNDAY -> "SU"
        else -> "MO"
    }

    return "RRULE:FREQ=MONTHLY;BYDAY=$weekNumber$byDay"
}

private fun monthlyByWeekdayLabel(date: String): String {
    if (date.isBlank()) return "Every same weekday of the month"

    return try {
        val calendar = Calendar.getInstance()
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calendar.time = parser.parse(date)!!

        val weekNumber = ((calendar.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1

        val ordinal = when (weekNumber) {
            1 -> "first"
            2 -> "second"
            3 -> "third"
            4 -> "fourth"
            else -> "fifth"
        }

        val weekday = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)

        "Every $ordinal $weekday of the month"
    } catch (e: Exception) {
        "Every same weekday of the month"
    }
}

private fun notificationLabel(notification: EventReminderOverride): String {
    val method = if (notification.method == "email") "email" else "phone notification"
    return "${minutesToReadableDuration(notification.minutes)} before by $method"
}

private fun minutesToReadableDuration(totalMinutes: Int): String {
    var remaining = totalMinutes

    val years = remaining / (60 * 24 * 365)
    remaining %= 60 * 24 * 365

    val months = remaining / (60 * 24 * 30)
    remaining %= 60 * 24 * 30

    val days = remaining / (60 * 24)
    remaining %= 60 * 24

    val hours = remaining / 60
    remaining %= 60

    val minutes = remaining

    val parts = mutableListOf<String>()

    if (years > 0) parts.add("$years ${if (years == 1) "year" else "years"}")
    if (months > 0) parts.add("$months ${if (months == 1) "month" else "months"}")
    if (days > 0) parts.add("$days ${if (days == 1) "day" else "days"}")
    if (hours > 0) parts.add("$hours ${if (hours == 1) "hour" else "hours"}")
    if (minutes > 0 || parts.isEmpty()) {
        parts.add("$minutes ${if (minutes == 1) "minute" else "minutes"}")
    }

    return parts.joinToString(" ")
}

private fun unitLabel(
    interval: Int,
    unit: String
): String {
    return if (interval == 1) unit.removeSuffix("s") else unit
}

private fun datePartFromEvent(value: String?): String {
    if (value.isNullOrBlank()) return ""

    return try {
        if (!value.contains("T")) {
            value
        } else {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatter.format(parser.parse(value)!!)
        }
    } catch (e: Exception) {
        ""
    }
}

private fun timePartFromEvent(value: String?): String {
    if (value.isNullOrBlank() || !value.contains("T")) return ""

    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        formatter.format(parser.parse(value)!!)
    } catch (e: Exception) {
        ""
    }
}

private fun addOneDay(date: String): String {
    return adjustDateByDays(date, 1)
}

private fun subtractOneDay(date: String): String {
    return adjustDateByDays(date, -1)
}

private fun adjustDateByDays(
    date: String,
    days: Int
): String {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = formatter.parse(date) ?: return date

        val calendar = Calendar.getInstance()
        calendar.time = parsedDate
        calendar.add(Calendar.DATE, days)

        formatter.format(calendar.time)
    } catch (e: Exception) {
        date
    }
}

private fun dateToUtcMillis(date: String): Long? {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        formatter.parse(date)?.time
    } catch (e: Exception) {
        null
    }
}

private fun utcMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(millis)
}

private fun isDateBefore(
    first: String,
    second: String
): Boolean {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.parse(first)!!.before(formatter.parse(second))
    } catch (e: Exception) {
        false
    }
}

private fun isTimeBefore(
    first: String,
    second: String
): Boolean {
    return try {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        formatter.parse(first)!!.before(formatter.parse(second))
    } catch (e: Exception) {
        false
    }
}

private fun recurrenceChoiceFromLabel(value: String?): String {
    return when {
        value == null -> "Does not repeat"
        value.contains("RDATE") -> "Custom"
        value.contains("FREQ=DAILY") -> "Daily"
        value.contains("FREQ=WEEKLY") -> "Weekly"
        value.contains("FREQ=MONTHLY") && value.contains("BYDAY") -> "Every same weekday of the month"
        value.contains("FREQ=MONTHLY") -> "Monthly"
        value.contains("FREQ=YEARLY") -> "Yearly"
        else -> "Does not repeat"
    }
}

private fun eventHasRecurrence(event: EventEntity): Boolean {
    return !event.recurringEventId.isNullOrBlank() ||
            event.recurrence?.contains("FREQ=") == true ||
            event.recurrence == "Part of a recurring series"
}