package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["recipient_id", "created_at"]),
        Index(value = ["request_id"]),
        Index(value = ["sync_state"])
    ]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "recipient_id") val recipientId: String,
    val type: String,
    val title: String,
    val body: String,
    @ColumnInfo(name = "request_id") val requestId: String?,
    @ColumnInfo(name = "donation_id") val donationId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_read") val isRead: Boolean,
    @Embedded(prefix = "sync_") val sync: SyncMetadata = SyncMetadata()
)
