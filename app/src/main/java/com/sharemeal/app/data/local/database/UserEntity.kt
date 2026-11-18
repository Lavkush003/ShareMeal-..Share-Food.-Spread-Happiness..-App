package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["sync_state"]),
        Index(value = ["sync_server_id"])
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val mobile: String,
    val role: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @Embedded(prefix = "sync_") val sync: SyncMetadata = SyncMetadata()
)
