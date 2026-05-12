package com.example.calendest.util

import com.example.calendest.data.model.EventDateTime
import com.google.api.client.util.DateTime
import java.text.SimpleDateFormat
import java.util.Locale

fun EventDateTime.format(): String {
    val dt = parsedDateTime ?: date?.let { DateTime(it) }
    return if (dt != null) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(java.util.Date(dt.value))
    } else {
        "Unknown"
    }
}

fun String.formatDateTime(): String {
    return try {
        val dt = DateTime(this)
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(java.util.Date(dt.value))
    } catch (e: Exception) {
        "Unknown"
    }
}

fun detailDateTimeLabel(value: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val date = parser.parse(value)

        val formatter = SimpleDateFormat(
            "EEEE, yyyy-MM-dd h:mm a",
            Locale.getDefault()
        )

        formatter.format(date!!)
    } catch (e: Exception) {
        value
    }
}