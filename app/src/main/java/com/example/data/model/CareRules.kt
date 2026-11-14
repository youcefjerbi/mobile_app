package com.example.data.model

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared validation and daily dose semantics; no Android dependencies. */
object CareRules {
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    fun takenOn(taken: Boolean, lastTakenDate: String, date: String): Boolean = taken && lastTakenDate == date
    fun validTime(value: String): Boolean = Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(value)
    fun appointmentMillis(value: String): Long? {
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}").matches(value)) return null
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = parser.parse(value, position) ?: return null
        return parsed.time.takeIf { position.index == value.length }
    }
    fun medicationError(name: String, dose: String, time: String): String? = when {
        name.isBlank() || name.length > 120 -> "Enter a medication name (1–120 characters)."
        dose.isBlank() || dose.length > 60 -> "Enter the prescribed dose (1–60 characters)."
        !validTime(time) -> "Enter a time in 24-hour format, for example 08:00."
        else -> null
    }
}
