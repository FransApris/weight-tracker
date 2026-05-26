package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.CalendarHelper
import com.example.utils.ExportCsvHelper
import com.example.utils.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeightViewModel(
    application: Application,
    repositoryParam: com.example.data.WeightRepository? = null,
    cloudSyncParam: com.example.data.CloudSync? = null
) : AndroidViewModel(application) {

    private val repository: WeightRepository = repositoryParam ?: run {
        val database = AppDatabase.getDatabase(application)
        WeightRepository(database.weightDao())
    }
    private val sharedPrefs = application.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    // Data lists
    val allEntries: StateFlow<List<WeightEntry>>
    
    // Theme configurations
    private val _themePreset = MutableStateFlow(sharedPrefs.getString("theme_preset", "classic") ?: "classic")
    val themePreset: StateFlow<String> = _themePreset

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode

    // Alarm reminder states
    private val _reminderEnabled = MutableStateFlow(true)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled

    private val _reminderHour = MutableStateFlow(8)
    val reminderHour: StateFlow<Int> = _reminderHour

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute: StateFlow<Int> = _reminderMinute

    // Cloud syncing states (abstracted behind CloudSync)
    private val cloudSync: com.example.data.CloudSync = cloudSyncParam ?: com.example.data.CloudSyncAdapter
    val isSyncing = cloudSync.isSyncing
    val syncLogs = cloudSync.syncLogs
    val syncState = cloudSync.syncState

    init {
        
        allEntries = repository.allEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load reminder alarm settings
        val (enabled, hour, min) = NotificationHelper.getReminderSettings(application)
        _reminderEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = min

        // Ensure channel is registered
        NotificationHelper.createNotificationChannel(application)
    }

    fun addWeightEntry(
        weight: Double,
        date: Date,
        notes: String,
        feelingEmoji: String,
        waistCircumference: Double,
        waterIntake: Int,
        exerciseMinutes: Int
    ) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { time = date }
            val epochDay = cal.timeInMillis / (24 * 60 * 60 * 1000)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = sdf.format(date)

            val existing = repository.getEntryByDate(epochDay)
            val entry = WeightEntry(
                id = existing?.id ?: 0,
                weight = weight,
                dateEpochDay = epochDay,
                dateString = dateStr,
                notes = notes,
                feelingEmoji = feelingEmoji,
                waistCircumferenceCm = waistCircumference,
                waterIntakeMl = waterIntake,
                exerciseMinutes = exerciseMinutes
            )
            repository.insert(entry)
        }
    }

    fun deleteWeightEntry(entry: WeightEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // Setters for Theme
    fun setThemePreset(preset: String) {
        _themePreset.value = preset
        sharedPrefs.edit().putString("theme_preset", preset).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    // Alarm reminder updates
    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        _reminderEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = minute
        NotificationHelper.setReminderEnabled(getApplication(), enabled, hour, minute)
    }

    // Cloud Synchronization
    fun syncWithCloud() {
        viewModelScope.launch {
            cloudSync.performCloudSync(allEntries.value)
        }
    }

    fun clearSyncLogs() {
        cloudSync.clearLogs()
    }

    fun updateCloudServerUrl(url: String) {
        cloudSync.updateServerUrl(url)
    }

    // Integration Calendar Native Slot
    fun scheduleCalendarSlot() {
        CalendarHelper.createReminderEventInCalendar(
            getApplication(),
            _reminderHour.value,
            _reminderMinute.value
        )
    }

    // CSV File Exporter
    fun exportCsv(context: Context) {
        ExportCsvHelper.exportWeightDataToCsv(context, allEntries.value)
    }
}
