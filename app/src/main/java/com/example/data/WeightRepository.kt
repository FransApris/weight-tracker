package com.example.data

import kotlinx.coroutines.flow.Flow

class WeightRepository(private val weightDao: WeightDao) {
    val allEntries: Flow<List<WeightEntry>> = weightDao.getAllEntries()

    fun getEntriesInRange(startDay: Long, endDay: Long): Flow<List<WeightEntry>> {
        return weightDao.getEntriesInRange(startDay, endDay)
    }

    suspend fun getEntryByDate(epochDay: Long): WeightEntry? {
        return weightDao.getEntryByDate(epochDay)
    }

    suspend fun insert(entry: WeightEntry) {
        weightDao.insertEntry(entry)
    }

    suspend fun delete(entry: WeightEntry) {
        weightDao.deleteEntry(entry)
    }

    suspend fun clearAll() {
        weightDao.clearAllEntries()
    }
}
