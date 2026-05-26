package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY dateEpochDay DESC")
    fun getAllEntries(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries WHERE dateEpochDay >= :startDay AND dateEpochDay <= :endDay ORDER BY dateEpochDay ASC")
    fun getEntriesInRange(startDay: Long, endDay: Long): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries WHERE dateEpochDay = :epochDay LIMIT 1")
    suspend fun getEntryByDate(epochDay: Long): WeightEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WeightEntry): Long

    @Delete
    suspend fun deleteEntry(entry: WeightEntry)

    @Query("DELETE FROM weight_entries")
    suspend fun clearAllEntries()
}
