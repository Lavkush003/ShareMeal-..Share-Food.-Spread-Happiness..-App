package com.sharemeal.app.feature.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.data.sync.SyncManager
import com.sharemeal.app.data.sync.SyncStatusStore
import com.sharemeal.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    syncStatusStore: SyncStatusStore,
    private val syncManager: SyncManager
) : ViewModel() {

    val status: StateFlow<SyncStatus> = syncStatusStore.status.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = syncStatusStore.status.value
    )

    fun retry() {
        syncManager.scheduleImmediateSync()
    }
}
