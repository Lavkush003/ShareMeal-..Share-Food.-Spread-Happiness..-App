package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_sync",
    indices = [
        Index(value = ["idempotency_key"], unique = true),
        Index(value = ["status", "next_retry_at"]),
        Index(value = ["entity_type", "entity_id"])
    ]
)
data class PendingSyncEntity(
    @PrimaryKey
    @ColumnInfo(name = "operation_id")
    val operationId: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    val type: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val path: String,
    val method: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int = 0,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: Long? = null,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "server_snapshot_json") val serverSnapshotJson: String? = null,
    val status: String = "PENDING"
)
