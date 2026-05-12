package com.example.calendest

import androidx.lifecycle.ViewModel
import com.example.calendest.data.repository.EventRepository
import com.example.calendest.fakes.FakeApiService
import com.example.calendest.fakes.FakeCalendarAuthClient
import com.example.calendest.fakes.FakeEventDao
import org.junit.Assert.assertTrue
import org.junit.Test

class EventViewModelFactoryTest {

    @Test
    fun create_returnsEventViewModel_whenModelClassMatches() {
        val repository = EventRepository(
            service = FakeApiService(),
            eventDao = FakeEventDao()
        )

        val factory = EventViewModelFactory(
            repo = repository,
            authClient = FakeCalendarAuthClient()
        )

        val viewModel = factory.create(EventViewModel::class.java)

        assertTrue(viewModel is EventViewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_throwsException_whenModelClassDoesNotMatch() {
        val repository = EventRepository(
            service = FakeApiService(),
            eventDao = FakeEventDao()
        )

        val factory = EventViewModelFactory(
            repo = repository,
            authClient = FakeCalendarAuthClient()
        )

        factory.create(FakeViewModel::class.java)
    }

    private class FakeViewModel : ViewModel()
}