package com.example.calendest.data.network

import com.example.calendest.data.model.Event
import com.example.calendest.data.model.EventWriteRequest
import com.example.calendest.data.model.EventsResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT

data class EventRecurrencePatch(
    val recurrence: List<String>
)

interface ApiService {
    @GET("calendar/v3/calendars/primary/events")
    suspend fun getEvents(
        @Header("Authorization") authToken: String,
        @Query("maxResults") maxResults: Int = 2500,
        @Query("pageToken") pageToken: String? = null,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime"
    ): EventsResponse

    @GET("calendar/v3/calendars/primary/events/{eventId}")
    suspend fun getEventById(
        @Path("eventId") eventId: String,
        @Header("Authorization") authToken: String
    ): Event

    @POST("calendar/v3/calendars/primary/events")
    suspend fun createEvent(
        @Header("Authorization") authToken: String,
        @Body event: EventWriteRequest
    ): Event

    @PATCH("calendar/v3/calendars/primary/events/{eventId}")
    suspend fun updateEvent(
        @Path("eventId") eventId: String,
        @Header("Authorization") authToken: String,
        @Body event: EventWriteRequest
    ): Event

    @DELETE("calendar/v3/calendars/primary/events/{eventId}")
    suspend fun deleteEvent(
        @Path("eventId") eventId: String,
        @Header("Authorization") authToken: String
    ): Response<Unit>

    @PUT("calendar/v3/calendars/primary/events/{eventId}")
    suspend fun replaceEvent(
        @Path("eventId") eventId: String,
        @Header("Authorization") authToken: String,
        @Body event: EventWriteRequest
    ): Event

    @PATCH("calendar/v3/calendars/primary/events/{eventId}")
    suspend fun updateEventRecurrence(
        @Path("eventId") eventId: String,
        @Header("Authorization") authToken: String,
        @Body patch: EventRecurrencePatch
    ): Event
}

val service: ApiService = Retrofit.Builder()
    .baseUrl("https://www.googleapis.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(ApiService::class.java)