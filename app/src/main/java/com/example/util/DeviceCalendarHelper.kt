package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DeviceCalendarEvent(
    val title: String,
    val location: String,
    val timeLabel: String,
    val startMillis: Long
)

object DeviceCalendarHelper {
    private const val TAG = "DeviceCalendarHelper"

    fun readUpcomingCalendarEvents(context: Context, maxResults: Int = 8): List<DeviceCalendarEvent> {
        val events = mutableListOf<DeviceCalendarEvent>()
        val now = System.currentTimeMillis()
        val twoWeeksLater = now + 14L * 24L * 60L * 60L * 1000L
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DELETED} != 1"
        val selectionArgs = arrayOf(now.toString(), twoWeeksLater.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val locIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                while (cursor.moveToNext() && events.size < maxResults) {
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else ""
                    val location = if (locIdx >= 0) cursor.getString(locIdx).orEmpty() else ""
                    val startMillis = if (startIdx >= 0) cursor.getLong(startIdx) else now
                    if (title.isNotBlank()) {
                        events.add(
                            DeviceCalendarEvent(
                                title = title.take(140),
                                location = location.ifBlank { "Device Calendar" }.take(140),
                                timeLabel = formatter.format(Date(startMillis)),
                                startMillis = startMillis
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_CALENDAR permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading device calendar events", e)
        }
        return events
    }

    fun launchAddAppointmentToCalendar(
        context: Context,
        title: String,
        location: String,
        description: String,
        scheduledDateTime: String,
        durationMinutes: Long
    ) {
        val startMillis = parseScheduledTimeMillis(scheduledDateTime)
        val endMillis = startMillis + (durationMinutes.coerceAtLeast(15L) * 60_000L)
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "No calendar app available to handle insert intent", e)
        }
    }

    fun launchEmailCoordination(
        context: Context,
        recipientEmail: String,
        subject: String,
        body: String
    ) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            if (recipientEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "No email app available to handle mailto intent", e)
        }
    }

    fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun nowTimestampLabel(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }

    private fun parseScheduledTimeMillis(scheduledDateTime: String): Long {
        val patterns = listOf("yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm", "MMM d, yyyy HH:mm")
        for (pattern in patterns) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.US).parse(scheduledDateTime.trim())
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 3)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
        return cal.timeInMillis
    }
}
