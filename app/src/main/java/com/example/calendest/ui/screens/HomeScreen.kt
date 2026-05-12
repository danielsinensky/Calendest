package com.example.calendest.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.calendest.EventViewModel
import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.model.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    eventViewModel: EventViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    val state by eventViewModel.state.collectAsState()
    val uiState = state.uiState

    var snagMenuExpanded by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            eventViewModel.handleAuthResult(context, result.data)
        } else {
            eventViewModel.onAuthCancelled()
        }
    }

    LaunchedEffect(state.accessToken) {
        if (state.accessToken.isNotEmpty() && state.events.isEmpty()) {
            eventViewModel.refreshHomeScreen()
        }
    }

    val refreshing = uiState is UiState.Loading

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            focusManager.clearFocus()
            keyboardController?.hide()
            eventViewModel.refreshHomeScreen(forceRefresh = true)
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        when (uiState) {
            is UiState.AuthRequired -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sign in to load your Google Calendar events.")

                    Button(
                        onClick = {
                            eventViewModel.signInAndAuthorize(
                                context = context,
                                launcher = launcher,
                                filterByAuthorizedAccounts = false
                            )
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Authorize Google Calendar")
                    }
                }
            }

            is UiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()

                    Text(
                        text = if (state.accessToken.isEmpty()) {
                            "Authorizing Google Calendar..."
                        } else {
                            "Loading calendar events..."
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            is UiState.Error -> {
                SnagScreen(
                    navController = navController,
                    errorMessage = uiState.message,
                    onTryAgain = {
                        if (state.accessToken.isEmpty()) {
                            eventViewModel.signInAndAuthorize(
                                context = context,
                                launcher = launcher,
                                filterByAuthorizedAccounts = false
                            )
                        } else {
                            eventViewModel.refreshHomeScreen(forceRefresh = true)
                        }
                    }
                )
            }

            is UiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("No events found.")

                    Button(
                        onClick = {
                            navController.navigate("createEventScreen")
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Create Event")
                    }
                }
            }

            is UiState.Success -> {
                val searchQuery = eventViewModel.searchQuery

                val filteredEvents = if (searchQuery.isBlank()) {
                    uiState.data
                } else {
                    uiState.data.filter { event ->
                        event.summary.contains(searchQuery, ignoreCase = true)
                    }
                }

                val groupedEvents = filteredEvents
                    .sortedBy { parseEventMillis(it.start) ?: Long.MAX_VALUE }
                    .groupBy { eventGroupLabel(it.start) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                eventViewModel.updateSearchQuery(query)
                            },
                            label = { Text("Search events") },
                            singleLine = true,
                            modifier = Modifier
                                .padding(16.dp)
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        keyboardController?.show()
                                    }
                                }
                        )

                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "${filteredEvents.size} event(s) found",
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 8.dp
                                )
                            )
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                navController.navigate("createEventScreen")
                            },
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 8.dp
                            )
                        ) {
                            Text("Create Event")
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Calendar Events",
                                    modifier = Modifier.padding(
                                        top = 8.dp,
                                        bottom = 8.dp
                                    )
                                )
                            }

                            groupedEvents.forEach { (dateLabel, eventsForDate) ->
                                item {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = dateLabel,
                                            modifier = Modifier.width(96.dp)
                                        )

                                        Column {
                                            eventsForDate.forEach { event ->
                                                Column(
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                ) {
                                                    Text(
                                                        text = event.summary,
                                                        modifier = Modifier.clickable {
                                                            focusManager.clearFocus()
                                                            keyboardController?.hide()
                                                            eventViewModel.selectEvent(event)
                                                            navController.navigate("detailsScreen")
                                                        }
                                                    )

                                                    Text(
                                                        text = homeEventDateTimeLabel(event)
                                                    )

                                                    Row {
                                                        Text(
                                                            text = "Edit",
                                                            modifier = Modifier.clickable {
                                                                focusManager.clearFocus()
                                                                keyboardController?.hide()
                                                                eventViewModel.selectEvent(event)
                                                                navController.navigate("editEventScreen/${event.id}")
                                                            }
                                                        )

                                                        Spacer(modifier = Modifier.width(16.dp))

                                                        Text(
                                                            text = "Delete",
                                                            modifier = Modifier.clickable {
                                                                focusManager.clearFocus()
                                                                keyboardController?.hide()
                                                                eventViewModel.deleteCalendarEvent(
                                                                    eventId = event.id,
                                                                    recurringEventId = event.recurringEventId
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = false,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        if (state.accessToken.isNotEmpty() && uiState !is UiState.AuthRequired) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = {
                        accountMenuExpanded = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Account menu"
                    )
                }

                DropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = {
                        accountMenuExpanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                state.currentAccountEmail?.let {
                                    "Signed in as $it"
                                } ?: "Signed in"
                            )
                        },
                        enabled = false,
                        onClick = {}
                    )

                    DropdownMenuItem(
                        text = { Text("Log out") },
                        onClick = {
                            accountMenuExpanded = false
                            eventViewModel.logout(context)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Switch account") },
                        onClick = {
                            accountMenuExpanded = false
                            eventViewModel.switchAccount(context, launcher)
                        }
                    )
                }
            }
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
                    contentDescription = "Snag menu"
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

private fun homeEventDateTimeLabel(event: EventEntity): String {
    val isAllDay = !event.start.contains("T")

    val startDate = eventDatePart(event.start)
    val endDate = if (isAllDay) {
        subtractOneDay(event.end)
    } else {
        eventDatePart(event.end)
    }

    val startsAndEndsSameDate = startDate == endDate

    return when {
        isAllDay && startsAndEndsSameDate -> {
            "All day"
        }

        isAllDay -> {
            "All day through ${homeDateLabel(endDate)}"
        }

        startsAndEndsSameDate -> {
            "${eventTimeLabel(event.start)} - ${eventTimeLabel(event.end)}"
        }

        else -> {
            "${eventTimeLabel(event.start)} - ${homeDateLabel(event.end)} ${eventTimeLabel(event.end)}"
        }
    }
}

private fun parseEventMillis(value: String): Long? {
    return try {
        val parser = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        )

        parser.parse(value)?.time
    } catch (e: Exception) {
        try {
            val parser = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

            parser.parse(value)?.time
        } catch (e: Exception) {
            null
        }
    }
}

private fun eventGroupLabel(value: String): String {
    val millis = parseEventMillis(value) ?: return "Unknown"

    val formatter = SimpleDateFormat(
        "EEE\nMMM d\nyyyy",
        Locale.getDefault()
    )

    formatter.timeZone = TimeZone.getDefault()

    return formatter.format(millis)
}

private fun eventTimeLabel(value: String): String {
    if (!value.contains("T")) {
        return "All day"
    }

    val millis = parseEventMillis(value) ?: return "Unknown"

    val formatter = SimpleDateFormat(
        "h:mm a",
        Locale.getDefault()
    )

    formatter.timeZone = TimeZone.getDefault()

    return formatter.format(millis)
}

private fun eventDatePart(value: String): String {
    return if (value.contains("T")) {
        value.substringBefore("T")
    } else {
        value
    }
}

private fun homeDateLabel(value: String): String {
    return try {
        val rawDate = eventDatePart(value)

        val parser = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

        val date = parser.parse(rawDate)

        val formatter = SimpleDateFormat(
            "EEE, MMM d, yyyy",
            Locale.getDefault()
        )

        formatter.format(date!!)
    } catch (e: Exception) {
        value
    }
}

private fun subtractOneDay(value: String): String {
    return try {
        val parser = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

        val date = parser.parse(value)

        val calendar = Calendar.getInstance()
        calendar.time = date!!
        calendar.add(Calendar.DATE, -1)

        parser.format(calendar.time)
    } catch (e: Exception) {
        value
    }
}