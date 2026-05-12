package com.example.calendest.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleCalendarAuthClientTest {

    @Test
    fun googleCalendarAuthClient_canBeCreated() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val authClient = GoogleCalendarAuthClient()

        assertNotNull(context)
        assertNotNull(authClient)
    }
}