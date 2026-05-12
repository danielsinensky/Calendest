package com.example.calendest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.calendest.EventViewModel
import com.example.calendest.data.model.EventReminderOverride
import com.example.calendest.data.model.EventReminders
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DetailsScreen(
    navController: NavController,
    eventViewModel: EventViewModel
) {
    val state by eventViewModel.state.collectAsState()
    val event = state.selectedEvent

    var snagMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(eventViewModel) {
        eventViewModel.refreshDetailsScreen()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (event != null) {
            Column(
                modifier = Modifier
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(16.dp)
            ) {
                Text(text = "Event: ${event.summary}")

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Creator: ${event.creatorName ?: "Unknown"}")

                Spacer(modifier = Modifier.height(8.dp))

                event.location?.let {
                    Text(text = "Location: $it")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(text = "Start: ${detailDateTimeLabel(event.start)}")

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "End: ${detailDateTimeLabel(event.end)}")

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Recurrence: ${event.recurrence ?: "Does not repeat"}")

                Spacer(modifier = Modifier.height(8.dp))

                Text("Notifications:")

                val parsedNotifications = parseReminderOverrides(event.reminders)

                if (parsedNotifications.isEmpty()) {
                    Text("No notifications")
                } else {
                    parsedNotifications.forEachIndexed { index, notification ->
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(text = notificationLabel(notification))

                            Text(
                                text = "Edit",
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable {
                                        eventViewModel.setNotificationsForEditing(parsedNotifications)
                                        eventViewModel.startEditingNotification(index)
                                        navController.navigate("notificationCustomEditScreen")
                                    }
                            )

                            Text(
                                text = "Delete",
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable {
                                        val updatedNotifications =
                                            parsedNotifications.filterIndexed { i, _ ->
                                                i != index
                                            }

                                        eventViewModel.setNotificationsForEditing(updatedNotifications)

                                        eventViewModel.updateCalendarEvent(
                                            eventId = event.id,
                                            recurringEventId = event.recurringEventId,
                                            applyToSeries = false,
                                            summary = event.summary,
                                            location = event.location,
                                            description = null,
                                            startDateTime = event.start,
                                            endDateTime = event.end,
                                            recurrence = null,
                                            reminders = EventReminders(
                                                useDefault = false,
                                                overrides = updatedNotifications
                                            ),
                                            wasAllDay = !event.start.contains("T"),
                                            isAllDay = !event.start.contains("T")
                                        )
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        eventViewModel.selectEvent(event)
                        navController.navigate("editEventScreen/${event.id}")
                    }
                ) {
                    Text("Edit Event")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        eventViewModel.deleteCalendarEvent(event.id)

                        navController.navigate("homeScreen") {
                            popUpTo("homeScreen") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("Delete Event")
                }
            }
        } else {
            Text(
                text = "Loading...",
                modifier = Modifier
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(16.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    snagMenuExpanded = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Report,
                    contentDescription = "Snag Menu"
                )
            }

            DropdownMenu(
                expanded = snagMenuExpanded,
                onDismissRequest = {
                    snagMenuExpanded = false
                }
            ) {
                DropdownMenuItem(
                    text = { Text("Report a Snag") },
                    onClick = {
                        snagMenuExpanded = false
                        navController.navigate("snagReportScreen")
                    }
                )

                DropdownMenuItem(
                    text = { Text("View Snag Reports") },
                    onClick = {
                        snagMenuExpanded = false
                        navController.navigate("snagListScreen")
                    }
                )
            }
        }
    }
}

private fun detailDateTimeLabel(value: String): String {
    return try {
        val parser = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        )

        val date = parser.parse(value)

        val formatter = SimpleDateFormat(
            "EEEE, yyyy-MM-dd h:mm a",
            Locale.getDefault()
        )

        formatter.format(date!!)
    } catch (e: Exception) {
        value
    }
}

private fun parseReminderOverrides(value: String?): List<EventReminderOverride> {
    if (value.isNullOrBlank()) return emptyList()
    if (value == "Calendar default reminders") return emptyList()
    if (value == "No reminders") return emptyList()

    return value.lines().mapNotNull { line ->
        val method = when {
            line.startsWith("email") -> "email"
            line.startsWith("popup") -> "popup"
            else -> return@mapNotNull null
        }

        val minutes = Regex("""(\d+)\s+minutes""")
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return@mapNotNull null

        EventReminderOverride(
            method = method,
            minutes = minutes
        )
    }
}

private fun notificationLabel(notification: EventReminderOverride): String {
    val method = if (notification.method == "email") {
        "email"
    } else {
        "phone notification"
    }

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

    if (years > 0) {
        parts.add("$years ${if (years == 1) "year" else "years"}")
    }

    if (months > 0) {
        parts.add("$months ${if (months == 1) "month" else "months"}")
    }

    if (days > 0) {
        parts.add("$days ${if (days == 1) "day" else "days"}")
    }

    if (hours > 0) {
        parts.add("$hours ${if (hours == 1) "hour" else "hours"}")
    }

    if (minutes > 0 || parts.isEmpty()) {
        parts.add("$minutes ${if (minutes == 1) "minute" else "minutes"}")
    }

    return parts.joinToString(" ")
}