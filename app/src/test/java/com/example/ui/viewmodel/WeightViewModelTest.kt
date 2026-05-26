package com.example.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SyncLog
import com.example.data.WeightEntry
import com.example.data.WeightRepository
import com.example.data.CloudSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WeightViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: WeightRepository
    private lateinit var vm: WeightViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = WeightRepository(db.weightDao())

        val fakeCloud = object : CloudSync {
            override val isSyncing = MutableStateFlow(false)
            override val syncLogs = MutableStateFlow<List<SyncLog>>(emptyList())
            override val syncState = MutableStateFlow("idle")
            override fun updateServerUrl(url: String) {}
            override suspend fun performCloudSync(entries: List<WeightEntry>) {}
            override fun clearLogs() {}
        }

        vm = WeightViewModel(ApplicationProvider.getApplicationContext(), repositoryParam = repo, cloudSyncParam = fakeCloud)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addWeightEntry_insertsEntry() = runTest {
        val now = Date()
        vm.addWeightEntry(70.0, now, "note", "😊", 80.0, 1500, 30)
        advanceUntilIdle()

        val entries = vm.allEntries.value
        assertEquals(1, entries.size)
        assertEquals(70.0, entries[0].weight, 0.001)
    }
}
