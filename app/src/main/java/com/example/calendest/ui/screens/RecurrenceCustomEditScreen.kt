package com.example.calendest.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.calendest.EventViewModel

@Composable
fun RecurrenceCustomEditScreen(
    navController: NavController,
    eventViewModel: EventViewModel
) {
    var interval by remember {
        mutableStateOf(eventViewModel.customRecurrenceInterval)
    }

    var unit by remember {
        mutableStateOf(eventViewModel.customRecurrenceUnit)
    }

    var calendar by remember {
        mutableStateOf(eventViewModel.customRecurrenceCalendar)
    }

    var endType by remember {
        mutableStateOf(eventViewModel.customRecurrenceEndType)
    }

    var untilDate by remember {
        mutableStateOf(eventViewModel.customRecurrenceUntilDate)
    }

    var count by remember {
        mutableStateOf(eventViewModel.customRecurrenceCount)
    }

    var unitExpanded by remember { mutableStateOf(false) }
    var calendarExpanded by remember { mutableStateOf(false) }
    var endTypeExpanded by remember { mutableStateOf(false) }

    val units = listOf(
        "minutes",
        "hours",
        "days",
        "weeks",
        "months",
        "years"
    )

    val calendars = listOf(
        "Gregorian",
        "Chinese calendar - Simplified",
        "Chinese calendar - Traditional",
        "Hebrew calendar",
        "Hijri calendar - Civil",
        "Hijri calendar - Kuwaiti",
        "Hijri calendar - Saudi",
        "Indian calendar - Hindu (Saka)",
        "Persian calendar"
    )

    val endTypes = listOf(
        "Indefinitely",
        "Until date",
        "After number of occurrences"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Custom Recurrence")

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Column {
                Text("Every")

                Button(
                    onClick = {
                        if (interval > 1) interval--
                    }
                ) {
                    Text("-")
                }

                Text(interval.toString())

                Button(
                    onClick = {
                        interval++
                    }
                ) {
                    Text("+")
                }
            }

            Column(
                modifier = Modifier.padding(start = 24.dp)
            ) {
                Text("Unit")

                Button(
                    onClick = {
                        unitExpanded = true
                    }
                ) {
                    Text(if (interval == 1) unit.removeSuffix("s") else unit)
                }

                DropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = {
                        unitExpanded = false
                    }
                ) {
                    units.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(if (interval == 1) option.removeSuffix("s") else option)
                            },
                            onClick = {
                                unit = option
                                unitExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Alternate calendar recurrence basis")

        Button(
            onClick = {
                calendarExpanded = true
            }
        ) {
            Text(calendar)
        }

        DropdownMenu(
            expanded = calendarExpanded,
            onDismissRequest = {
                calendarExpanded = false
            }
        ) {
            calendars.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        calendar = option
                        calendarExpanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("End recurrence")

        Button(
            onClick = {
                endTypeExpanded = true
            }
        ) {
            Text(endType)
        }

        DropdownMenu(
            expanded = endTypeExpanded,
            onDismissRequest = {
                endTypeExpanded = false
            }
        ) {
            endTypes.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        endType = option
                        endTypeExpanded = false
                    }
                )
            }
        }

        if (endType == "Until date") {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = untilDate,
                onValueChange = {
                    untilDate = it
                },
                label = {
                    Text("Until date YYYY-MM-DD")
                },
                singleLine = true
            )
        }

        if (endType == "After number of occurrences") {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = count,
                onValueChange = {
                    count = it
                },
                label = {
                    Text("Number of occurrences")
                },
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                eventViewModel.updateCustomRecurrence(
                    interval = interval,
                    unit = unit,
                    calendar = calendar,
                    endType = endType,
                    untilDate = untilDate,
                    count = count
                )

                navController.popBackStack()
            }
        ) {
            Text("Save Custom Recurrence")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                navController.popBackStack()
            }
        ) {
            Text("Cancel")
        }
    }
}