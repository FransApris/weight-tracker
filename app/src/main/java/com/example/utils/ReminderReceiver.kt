package com.example.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Alarm received - showing weight tracker reminder")
        
        // Ensure Notification Channel is created
        NotificationHelper.createNotificationChannel(context)

        // Open MainActivity when user clicks the notification
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System default fallback
            .setContentTitle("Waktunya Timbang Berat Badan! ⚖️")
            .setContentText("Ayo catat berat badanmu hari ini untuk tetap konsisten melacak progres sehatmu!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error showing notification", e)
        }

        // Reschedule for next day to guarantee repeating on custom schedules
        NotificationHelper.rescheduleNextDay(context)
    }

    companion object {
        private const val NOTIFICATION_ID = 2002
    }
}

object NotificationHelper {
    const val CHANNEL_ID = "daily_remainder_channel"
    private const val PREFS_NAME = "ReminderSettings"
    private const val PREF_KEY_ENABLED = "reminder_enabled"
    private const val PREF_KEY_HOUR = "reminder_hour"
    private const val PREF_KEY_MINUTE = "reminder_minute"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pengingat Harian Berat Badan"
            val descriptionText = "Push notifikasi untuk mengingatkan pencatatan berat badan setiap hari"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun setReminderEnabled(context: Context, enabled: Boolean, hour: Int = 8, minute: Int = 0) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean(PREF_KEY_ENABLED, enabled)
            .putInt(PREF_KEY_HOUR, hour)
            .putInt(PREF_KEY_MINUTE, minute)
            .apply()

        if (enabled) {
            scheduleReminder(context, hour, minute)
        } else {
            cancelReminder(context)
        }
    }

    fun getReminderSettings(context: Context): Triple<Boolean, Int, Int> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = sharedPrefs.getBoolean(PREF_KEY_ENABLED, true) // Default enabled
        val hour = sharedPrefs.getInt(PREF_KEY_HOUR, 8) // Default 8:00 AM
        val minute = sharedPrefs.getInt(PREF_KEY_MINUTE, 0)
        return Triple(enabled, hour, minute)
    }

    fun rescheduleNextDay(context: Context) {
        val (enabled, hour, minute) = getReminderSettings(context)
        if (enabled) {
            scheduleReminder(context, hour, minute, delayOneDay = true)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleReminder(context: Context, hour: Int, minute: Int, delayOneDay: Boolean = false) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (delayOneDay || timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            // Using setAndAllowWhileIdle for reliable trigger in Doze mode without requiring EXACT_ALARM approvals
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("NotificationHelper", "Reminder scheduled successfully at ${calendar.time}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Could not schedule alarm", e)
        }
    }

    private fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("NotificationHelper", "Reminder alarm cancelled")
        }
    }
}
