package com.example.calendest

import com.example.calendest.data.repository.EventRepository
import com.example.calendest.fakes.FakeApiService
import com.example.calendest.fakes.FakeEventDao
import com.example.calendest.testdata.fakeEvent
import com.example.calendest.testdata.fakeEventEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventRepositoryTest {

    @Test
    fun getEvents_returnsLocalCache_whenCacheExistsAndForceRefreshFalse() = runTest {
        val api = FakeApiService()
        val dao = FakeEventDao()

        dao.insertEvent(fakeEventEntity)

        val repository = EventRepository(api, dao)

        val result = repository.getEvents(
            accessToken = "fake-token",
            forceRefresh = false
        )

        assertEquals(1, result.size)
        assertEquals("Event One", result[0].summary)
    }

    @Test
    fun getEvents_fetchesFromApi_whenForceRefreshTrue() = runTest {
        val api = FakeApiService()
        val dao = FakeEventDao()

        api.events = listOf(fakeEvent)

        val repository = EventRepository(api, dao)

        val result = repository.getEvents(
            accessToken = "fake-token",
            forceRefresh = true
        )

        assertEquals(1, result.size)
        assertEquals("Event One", result[0].summary)

        val cached = dao.getEvents()

        assertEquals(1, cached.size)
        assertEquals("Event One", cached[0].summary)
    }

    @Test
    fun getEvents_fetchesFromApi_whenCacheIsEmpty() = runTest {
        val api = FakeApiService()
        val dao = FakeEventDao()

        api.events = listOf(fakeEvent)

        val repository = EventRepository(api, dao)

        val result = repository.getEvents(
            accessToken = "fake-token",
            forceRefresh = false
        )

        assertEquals(1, result.size)
        assertEquals("Event One", result[0].summary)
    }

    @Test
    fun getEventById_returnsCachedEvent() = runTest {
        val api = FakeApiService()
        val dao = FakeEventDao()

        dao.insertEvent(fakeEventEntity)

        val repository = EventRepository(api, dao)

        val result = repository.getEventById("1")

        assertEquals("Event One", result?.summary)
    }

    @Test
    fun getEventById_returnsNull_whenEventDoesNotExist() = runTest {
        val api = FakeApiService()
        val dao = FakeEventDao()

        val repository = EventRepository(api, dao)

        val result = repository.getEventById("missing-id")

        assertNull(result)
    }

    @Test
    fun fetchAndCacheEventDetail_fetchesFromApiAndStoresDetail() = runTest {
        val api = FakeApiService()
        val dao = FakeEventDao()

        api.events = listOf(fakeEvent)

        val repository = EventRepository(api, dao)

        val result = repository.fetchAndCacheEventDetail(
            id = "1",
            accessToken = "fake-token"
        )

        assertEquals("Event One", result?.summary)
        assertEquals("Creator", result?.creatorName)

        val cached = dao.getEventById("1")

        assertEquals("Event One", cached?.summary)
        assertEquals("Creator", cached?.creatorName)
    }
}