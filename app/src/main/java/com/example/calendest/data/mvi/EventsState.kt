package com.example.calendest.data.mvi

import com.example.calendest.data.local.entity.EventEntity
import com.example.calendest.data.model.UiState

data class EventsState(
    val uiState: UiState = UiState.AuthRequired,
    val accessToken: String = "",
    val selectedEvent: EventEntity? = null,
    val events: List<EventEntity> = emptyList(),
    val error: String? = null,
    val currentAccountEmail: String? = null,
    val savedAccounts: List<String> = emptyList()
)