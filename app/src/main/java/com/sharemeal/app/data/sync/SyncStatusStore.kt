package com.sharemeal.app.data.sync

import com.sharemeal.app.core.network.NetworkMonitor
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.domain.model.SyncState
import com.sharemeal.app.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Singleton
class SyncStatusStore @Inject constructor(
    dao: ShareMealDao,
    networkMonitor: NetworkMonitor
) {
    private val lastSyncedAt = MutableStateFlow<Long?>(null)
    private val lastError = MutableStateFlow<String?>(null)

    val status: StateFlow<SyncStatus> = combine(
        networkMonitor.isOnline,
        dao.observePendingCount(),
        dao.observeConflictCount(),
        lastSyncedAt,
        lastError
    ) { online, pendingCount, conflictCount, syncedAt, error ->
        SyncStatus(
            state = when {
                !online -> SyncState.OFFLINE
                conflictCount > 0 -> SyncState.CONFLICT
                pendingCount > 0 -> SyncState.PENDING
                error != null -> SyncState.FAILED
                else -> SyncState.SYNCED
            },
            pendingCount = pendingCount,
            lastSyncedAt = syncedAt,
            lastError = error
        )
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncStatus(
            state = SyncState.PENDING,
            pendingCount = 0,
            lastSyncedAt = null
        )
    )

    fun markSuccess(timestamp: Long = System.currentTimeMillis()) {
        lastError.value = null
        lastSyncedAt.value = timestamp
    }

    fun markFailure(message: String?) {
        lastError.value = message
    }
}
