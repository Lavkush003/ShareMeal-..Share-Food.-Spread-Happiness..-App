package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import com.sharemeal.app.domain.model.SyncState

data class SyncMetadata(
    @ColumnInfo(name = "server_id") val serverId: String? = null,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "state") val syncState: String = "SYNCED",
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = false,
    @ColumnInfo(name = "last_error") val lastError: String? = null
) {
    val state: SyncState
        get() = SyncState.from(syncState)
}
