package com.sharemeal.app.domain.model

data class SyncStatus(
    val state: SyncState,
    val pendingCount: Int,
    val lastSyncedAt: Long?,
    val lastError: String? = null
)
