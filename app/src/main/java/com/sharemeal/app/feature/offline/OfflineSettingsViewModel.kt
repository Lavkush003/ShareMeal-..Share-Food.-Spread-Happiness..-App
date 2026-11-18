package com.sharemeal.app.feature.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.data.offline.RoomCachedDataRepository
import com.sharemeal.app.data.sync.SyncManager
import com.sharemeal.app.data.sync.SyncStatusStore
import com.sharemeal.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class OfflineSettingsUiState(
    val status: SyncStatus,
    val cachedDonationCount: Int
)

@HiltViewModel
class OfflineSettingsViewModel @Inject constructor(
    syncStatusStore: SyncStatusStore,
    cachedData: RoomCachedDataRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    val uiState: StateFlow<OfflineSettingsUiState> = combine(
        syncStatusStore.status,
        cachedData.observeActiveDonations()
    ) { status, donations ->
        OfflineSettingsUiState(
            status = status,
            cachedDonationCount = donations.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OfflineSettingsUiState(
            status = syncStatusStore.status.value,
            cachedDonationCount = 0
        )
    )

    fun syncNow() {
        syncManager.scheduleImmediateSync()
    }
}
