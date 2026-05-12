package com.example.calendest

import com.example.calendest.util.formatDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeFormatterTest {

    @Test
    fun formatDateTime_validDate_returnsFormattedDate() {
        val result = "2026-05-01T10:00:00-04:00".formatDateTime()

        assertEquals("2026-05-01 10:00", result)
    }

    @Test
    fun formatDateTime_invalidDate_returnsUnknown() {
        val result = "bad-date".formatDateTime()

        assertEquals("Unknown", result)
    }

    @Test
    fun formatDateTime_emptyString_returnsUnknown() {
        val result = "".formatDateTime()

        assertEquals("Unknown", result)
    }
}