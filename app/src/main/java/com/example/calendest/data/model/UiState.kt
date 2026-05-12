package com.example.calendest.data.model

import com.example.calendest.data.local.entity.EventEntity

sealed class UiState {
    data object AuthRequired : UiState()

    data object Loading : UiState()

    data object Empty : UiState()

    data class Success(
        val data: List<EventEntity>,
        val refreshMessage: String? = null
    ) : UiState()

    data class Error(
        val message: String
    ) : UiState()
}