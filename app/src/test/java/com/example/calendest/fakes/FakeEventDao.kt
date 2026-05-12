package com.example.calendest.fakes

import com.example.calendest.data.local.dao.EventDao
import com.example.calendest.data.local.entity.EventEntity

class FakeEventDao : EventDao {

    private val events = mutableListOf<EventEntity>()

    override suspend fun getEvents(): List<EventEntity> {
        return events
    }

    override suspend fun getEventById(id: String): EventEntity? {
        return events.firstOrNull { it.id == id }
    }

    override suspend fun insertEvent(event: EventEntity) {
        events.removeAll { it.id == event.id }
        events.add(event)
    }

    override suspend fun insertEvents(events: List<EventEntity>) {
        this.events.removeAll { existing ->
            events.any { newEvent -> newEvent.id == existing.id }
        }
        this.events.addAll(events)
    }

    override suspend fun deleteEventById(id: String) {
        events.removeAll { it.id == id }
    }

    override suspend fun deleteAllEvents() {
        events.clear()
    }
}