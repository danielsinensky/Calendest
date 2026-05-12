package com.example.calendest.mvi

import com.example.calendest.data.model.UiState
import com.example.calendest.data.mvi.EventsAction
import com.example.calendest.data.mvi.EventsState
import com.example.calendest.data.mvi.eventsReducer
import com.example.calendest.testdata.fakeEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsReducerTest {

    @Test
    fun `FetchEvents sets state to Loading`() {
        val initialState = EventsState()

        val newState = eventsReducer(
            state = initialState,
            action = EventsAction.FetchEvents
        )

        assertTrue(newState.uiState is UiState.Loading)
    }

    @Test
    fun `FetchEventsSuccess sets Success with events`() {
        val initialState = EventsState()

        val newState = eventsReducer(
            state = initialState,
            action = EventsAction.FetchEventsSuccess(
                events = listOf(fakeEventEntity)
            )
        )

        assertTrue(newState.uiState is UiState.Success)
        assertEquals(1, newState.events.size)
        assertEquals("Event One", newState.events[0].summary)
    }

    @Test
    fun `FetchEventsSuccess with empty list sets Empty`() {
        val initialState = EventsState()

        val newState = eventsReducer(
            state = initialState,
            action = EventsAction.FetchEventsSuccess(emptyList())
        )

        assertTrue(newState.uiState is UiState.Empty)
        assertEquals(0, newState.events.size)
    }

    @Test
    fun `FetchEventsError sets Error`() {
        val initialState = EventsState()

        val newState = eventsReducer(
            state = initialState,
            action = EventsAction.FetchEventsError("Network error")
        )

        assertTrue(newState.uiState is UiState.Error)
        assertEquals("Network error", newState.error)
    }

    @Test
    fun `UpdateSelectedEvent stores selected event`() {
        val initialState = EventsState()

        val newState = eventsReducer(
            state = initialState,
            action = EventsAction.UpdateSelectedEvent(fakeEventEntity)
        )

        assertEquals(fakeEventEntity, newState.selectedEvent)
    }

    @Test
    fun `AuthSuccess stores access token`() {
        val initialState = EventsState()

        val newState = eventsReducer(
            state = initialState,
            action = EventsAction.AuthSuccess("fake-token")
        )

        assertEquals("fake-token", newState.accessToken)
        assertTrue(newState.uiState is UiState.Loading)
    }

    @Test
    fun `SetAuthRequired clears state`() {
        val initialState = EventsState(
            accessToken = "fake-token",
            selectedEvent = fakeEventEntity,
            events = listOf(fakeEventEntity)
        )

        val newState = eventsReducer(
            state = initialState,
            action = EventsAction.SetAuthRequired
        )

        assertTrue(newState.uiState is UiState.AuthRequired)
        assertEquals("", newState.accessToken)
        assertEquals(emptyList<Any>(), newState.events)
        assertEquals(null, newState.selectedEvent)
    }
}