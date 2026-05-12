package com.example.calendest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendest.data.local.entity.SnagReportEntity
import com.example.calendest.data.repository.SnagReportRepository
import kotlinx.coroutines.launch

class SnagReportViewModel(
    private val repository: SnagReportRepository
) : ViewModel() {

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var stepsToReproduce by mutableStateOf("")
    var severity by mutableStateOf("Medium")

    var reports by mutableStateOf<List<SnagReportEntity>>(emptyList())

    var isSubmitting by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    fun submitReport(onSuccess: () -> Unit) {

        successMessage = null
        errorMessage = null

        val missingFields = mutableListOf<String>()

        if (title.isBlank()) {
            missingFields.add("Title")
        }

        if (description.isBlank()) {
            missingFields.add("Description")
        }

        if (stepsToReproduce.isBlank()) {
            missingFields.add("Steps to Reproduce")
        }

        if (missingFields.isNotEmpty()) {
            errorMessage =
                "Required field(s) missing: ${missingFields.joinToString(", ")}"

            return
        }

        viewModelScope.launch {
            try {
                isSubmitting = true
                successMessage = null
                errorMessage = null

                repository.submitReport(
                    title = title,
                    description = description,
                    stepsToReproduce = stepsToReproduce,
                    severity = severity
                )

                title = ""
                description = ""
                stepsToReproduce = ""
                severity = "Medium"

                loadReports()
                onSuccess()

            } catch (e: Exception) {

                errorMessage =
                    e.message ?: "Failed to submit snag report."

            } finally {
                isSubmitting = false
            }
        }
    }

    fun loadReports() {
        viewModelScope.launch {
            try {
                reports = repository.getReports()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load snag reports."
            }
        }
    }

    fun markReportAsEmailed(reportId: Int) {
        viewModelScope.launch {
            repository.markReportAsEmailed(reportId)
            loadReports()
        }
    }

    fun deleteReport(id: Int) {
        viewModelScope.launch {
            repository.deleteReport(id)
            loadReports()
        }
    }
}