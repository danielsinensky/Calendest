package com.example.calendest.data.mvi

import com.example.calendest.data.model.UiState

fun eventsReducer(state: EventsState, action: EventsAction): EventsState {
    return when (action) {
        is EventsAction.SetAuthRequired -> {
            state.copy(
                uiState = UiState.AuthRequired,
                accessToken = "",
                events = emptyList(),
                selectedEvent = null,
                error = null
            )
        }

        is EventsAction.AuthSuccess -> {
            state.copy(
                accessToken = action.accessToken,
                uiState = UiState.Loading,
                error = null
            )
        }

        is EventsAction.AccountSelected -> state.copy(
            accessToken = action.accessToken,
            currentAccountEmail = action.email,
            uiState = UiState.Loading,
            error = null
        )

        is EventsAction.SavedAccountsLoaded -> state.copy(
            savedAccounts = action.accounts
        )

        EventsAction.Logout -> state.copy(
            accessToken = "",
            currentAccountEmail = null,
            events = emptyList(),
            selectedEvent = null,
            uiState = UiState.AuthRequired,
            error = null
        )

        EventsAction.RevokeAccount -> state.copy(
            accessToken = "",
            currentAccountEmail = null,
            events = emptyList(),
            selectedEvent = null,
            uiState = UiState.AuthRequired,
            error = null
        )

        is EventsAction.FetchEvents -> {
            state.copy(
                uiState = UiState.Loading,
                error = null
            )
        }

        is EventsAction.FetchEventsSuccess -> {
            state.copy(
                uiState = if (action.events.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(data = action.events)
                },
                events = action.events,
                error = null
            )
        }

        is EventsAction.FetchEventsError -> {
            state.copy(
                uiState = UiState.Error(message = action.error),
                error = action.error
            )
        }

        is EventsAction.UpdateSelectedEvent -> {
            state.copy(
                selectedEvent = action.event
            )
        }
    }
}