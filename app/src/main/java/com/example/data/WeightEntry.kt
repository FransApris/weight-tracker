package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weight: Double,                 // Weight in kg (or lbs)
    val dateEpochDay: Long,             // Epoch day for uniqueness and searching
    val dateString: String,             // Formatting: YYYY-MM-DD
    val notes: String = "",             // Notes/food/journal
    val feelingEmoji: String = "😊",     // Mood/feeling: 😊, 😴, 💪, 🍕, 😔 etc.
    val waistCircumferenceCm: Double = 0.0, // Waist circumference
    val waterIntakeMl: Int = 0,         // Water in ml
    val exerciseMinutes: Int = 0        // Minutes of activity
)
