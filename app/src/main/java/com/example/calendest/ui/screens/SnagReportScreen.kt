package com.example.calendest.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendest.SnagReportViewModel
import androidx.compose.foundation.layout.statusBarsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnagReportScreen(
    snagReportViewModel: SnagReportViewModel,
    onReportSubmitted: () -> Unit
) {
    val severities = listOf("Low", "Medium", "High", "Critical")
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(text = "Report a Snag")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = snagReportViewModel.title,
            onValueChange = { snagReportViewModel.title = it },
            label = { Text("Title *") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = snagReportViewModel.description,
            onValueChange = { snagReportViewModel.description = it },
            label = { Text("Description *") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = snagReportViewModel.stepsToReproduce,
            onValueChange = { snagReportViewModel.stepsToReproduce = it },
            label = { Text("Steps to Reproduce *") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = snagReportViewModel.severity,
                onValueChange = {},
                readOnly = true,
                label = { Text("Severity") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                severities.forEach { severity ->
                    DropdownMenuItem(
                        text = {
                            Text(severity)
                        },
                        onClick = {
                            snagReportViewModel.severity = severity
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                snagReportViewModel.submitReport(
                    onSuccess = onReportSubmitted
                )
            },
            enabled = !snagReportViewModel.isSubmitting
        ) {
            Text("Submit Report")
        }

        Spacer(modifier = Modifier.height(12.dp))

        snagReportViewModel.errorMessage?.let {
            Text(text = it)
        }

        if (snagReportViewModel.isSubmitting) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }
    }
}