package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeightDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: WeightDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.weightDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetAllEntries() = runTest {
        val entry = WeightEntry(
            id = 0,
            weight = 68.5,
            dateEpochDay = 19000,
            dateString = "2023-01-01",
            notes = "test",
            feelingEmoji = "😊",
            waistCircumferenceCm = 80.0,
            waterIntakeMl = 1200,
            exerciseMinutes = 20
        )

        dao.insertEntry(entry)

        val list = dao.getAllEntries().first()
        assertEquals(1, list.size)
        assertEquals(68.5, list[0].weight, 0.001)
    }

    @Test
    fun getEntryByDate_returnsNullWhenAbsent() = runTest {
        val result = dao.getEntryByDate(12345)
        assertEquals(null, result)
    }
}
