package com.example.calendest.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
fun NotificationCustomEditScreen(
    navController: NavController,
    eventViewModel: EventViewModel
) {
    var amount by remember {
        mutableStateOf(eventViewModel.customNotificationAmount)
    }

    var unit by remember {
        mutableStateOf(eventViewModel.customNotificationUnit)
    }

    var method by remember {
        mutableStateOf(eventViewModel.customNotificationMethod)
    }

    var unitExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    val units = listOf(
        "minutes",
        "hours",
        "days",
        "weeks",
        "months",
        "years"
    )

    val methods = listOf(
        "popup",
        "email"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Custom Notification")

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Column {
                Text("Amount before event")

                Button(
                    onClick = {
                        if (amount > 1) amount--
                    }
                ) {
                    Text("-")
                }

                Text(amount.toString())

                Button(
                    onClick = {
                        amount++
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
                    Text(if (amount == 1) unit.removeSuffix("s") else unit)
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
                                Text(if (amount == 1) option.removeSuffix("s") else option)
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

        Text("Notification method")

        Button(
            onClick = {
                methodExpanded = true
            }
        ) {
            Text(
                text = if (method == "popup") {
                    "Phone notification"
                } else {
                    "Email"
                }
            )
        }

        DropdownMenu(
            expanded = methodExpanded,
            onDismissRequest = {
                methodExpanded = false
            }
        ) {
            methods.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (option == "popup") {
                                "Phone notification"
                            } else {
                                "Email"
                            }
                        )
                    },
                    onClick = {
                        method = option
                        methodExpanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                eventViewModel.finishEditingNotification(
                    amount = amount,
                    unit = unit,
                    method = method
                )

                navController.popBackStack()
            }
        ) {
            Text("Save Notification")
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