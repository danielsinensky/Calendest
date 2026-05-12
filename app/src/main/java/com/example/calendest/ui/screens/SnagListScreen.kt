package com.example.calendest.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.calendest.SnagReportViewModel
import com.example.calendest.data.local.entity.SnagReportEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.calendest.BuildConfig

@Composable
fun SnagListScreen(
    snagReportViewModel: SnagReportViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        snagReportViewModel.loadReports()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text("Reported Snags")

        Spacer(modifier = Modifier.height(16.dp))

        snagReportViewModel.successMessage?.let {
            Text(it)
            Spacer(modifier = Modifier.height(8.dp))
        }

        snagReportViewModel.errorMessage?.let {
            Text("Error: $it")
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn {
            items(snagReportViewModel.reports) { report ->
                SnagReportCard(
                    report = report,
                    snagReportViewModel = snagReportViewModel,
                    onEmailReport = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}")
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "Calendest Snag Report: ${report.title}"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                buildSnagEmailBody(report)
                            )
                        }

                        context.startActivity(
                            Intent.createChooser(intent, "Email snag report")
                        )

                        snagReportViewModel.markReportAsEmailed(report.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun SnagReportCard(
    report: SnagReportEntity,
    snagReportViewModel: SnagReportViewModel,
    onEmailReport: () -> Unit
) {
    Card(
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text("Title: ${report.title}")
            Text("Severity: ${report.severity}")
            Text("Description: ${report.description}")

            if (report.stepsToReproduce.isNotBlank()) {
                Text("Steps: ${report.stepsToReproduce}")
            }

            Text(
                text = "Created: ${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    ).format(Date(report.createdAt))
                }"
            )

            Text(
                text = if (report.synced) {
                    "Status: Email opened"
                } else {
                    "Status: Not emailed"
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column {

                Button(
                    onClick = onEmailReport
                ) {
                    Text("Email to Support")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        snagReportViewModel.deleteReport(report.id)
                    }
                ) {
                    Text("Delete Report")
                }
            }
        }
    }
}

private fun buildSnagEmailBody(report: SnagReportEntity): String {
    val createdAt = SimpleDateFormat(
        "yyyy-MM-dd HH:mm",
        Locale.getDefault()
    ).format(Date(report.createdAt))

    return """
        Calendest Snag Report

        Title:
        ${report.title}

        Severity:
        ${report.severity}

        Description:
        ${report.description}

        Steps to Reproduce:
        ${report.stepsToReproduce.ifBlank { "Not provided" }}

        Created At:
        $createdAt

        Local Report ID:
        ${report.id}
    """.trimIndent()
}