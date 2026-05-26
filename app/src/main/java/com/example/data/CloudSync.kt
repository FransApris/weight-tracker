package com.example.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over cloud synchronization so implementations can be swapped (real, fake, tests).
 */
interface CloudSync {
    val isSyncing: StateFlow<Boolean>
    val syncLogs: StateFlow<List<SyncLog>>
    val syncState: StateFlow<String>

    fun updateServerUrl(url: String)
    suspend fun performCloudSync(entries: List<WeightEntry>)
    fun clearLogs()
}

/**
 * Default adapter that delegates to the existing CloudSyncService object.
 * Keeps backwards compatibility while allowing tests to inject alternatives later.
 */
object CloudSyncAdapter : CloudSync {
    override val isSyncing: StateFlow<Boolean> = CloudSyncService.isSyncing
    override val syncLogs: StateFlow<List<SyncLog>> = CloudSyncService.syncLogs
    override val syncState: StateFlow<String> = CloudSyncService.syncState

    override fun updateServerUrl(url: String) = CloudSyncService.updateServerUrl(url)
    override suspend fun performCloudSync(entries: List<WeightEntry>) = CloudSyncService.performCloudSync(entries)
    override fun clearLogs() = CloudSyncService.clearLogs()
}
