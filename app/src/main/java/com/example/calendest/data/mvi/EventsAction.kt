package com.example.calendest.data.mvi

import com.example.calendest.data.local.entity.EventEntity

sealed class EventsAction {
    data object SetAuthRequired : EventsAction()
    data class AccountSelected(
        val email: String,
        val accessToken: String
    ) : EventsAction()

    data class SavedAccountsLoaded(
        val accounts: List<String>
    ) : EventsAction()

    data object Logout : EventsAction()

    data object RevokeAccount : EventsAction()
    data class AuthSuccess(val accessToken: String) : EventsAction()
    data object FetchEvents : EventsAction()
    data class FetchEventsSuccess(val events: List<EventEntity>) : EventsAction()
    data class FetchEventsError(val error: String) : EventsAction()
    data class UpdateSelectedEvent(val event: EventEntity?) : EventsAction()
}