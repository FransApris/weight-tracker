package com.example.utils

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import java.util.Calendar

object CalendarHelper {
    /**
     * Launch an intent to create a schedule slot in the user's default calendar (Google Calendar, etc.)
     * for daily weight tracking. This ensures seamless integration with third-party calendars.
     */
    fun createReminderEventInCalendar(context: Context, hour: Int = 8, minute: Int = 0) {
        val beginTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // If it's already past this hour, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val endTime = Calendar.getInstance().apply {
            timeInMillis = beginTime.timeInMillis
            add(Calendar.MINUTE, 15) // 15-minute slot
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.timeInMillis)
            putExtra(CalendarContract.Events.TITLE, "⚖️ Timbang & Catat Berat Badan")
            putExtra(CalendarContract.Events.DESCRIPTION, "Jadwal harian untuk merekam berat badan di aplikasi Weight Tracker untuk memantau progres kesehatan.")
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Di Rumah")
            putExtra(CalendarContract.Events.ALL_DAY, false)
            // Add Daily Recurrence rule
            putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY")
            // Prompt choice of calendars
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Aplikasi Kalender tidak ditemukan!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
