package com.sharemeal.app.domain.model

import java.util.UUID

enum class SyncOperationType {
    CREATE_DONATION,
    SUBMIT_REQUEST,
    APPROVE_REQUEST,
    REJECT_REQUEST,
    CANCEL_REQUEST,
    SCHEDULE_PICKUP,
    NGO_READY,
    DONOR_READY,
    COLLECT,
    DISTRIBUTE,
    COMPLETE_PICKUP,
    RECORD_EXCEPTION,
    UPDATE_PROFILE
}

enum class SyncOperationStatus {
    PENDING,
    IN_PROGRESS,
    FAILED,
    CONFLICT,
    SYNCED
}

data class PendingOperation(
    val operationId: String,
    val idempotencyKey: String,
    val type: SyncOperationType,
    val entityType: String,
    val entityId: String,
    val path: String,
    val method: String,
    val payloadJson: String,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val nextRetryAt: Long? = null,
    val lastError: String? = null,
    val serverSnapshotJson: String? = null,
    val status: SyncOperationStatus = SyncOperationStatus.PENDING
) {
    companion object {
        fun create(
            type: SyncOperationType,
            entityType: String,
            entityId: String,
            path: String,
            method: String = "POST",
            payloadJson: String = "{}",
            idempotencyKey: String = UUID.randomUUID().toString()
        ): PendingOperation = PendingOperation(
            operationId = "op_${UUID.randomUUID()}",
            idempotencyKey = idempotencyKey,
            type = type,
            entityType = entityType,
            entityId = entityId,
            path = path,
            method = method,
            payloadJson = payloadJson,
            createdAt = System.currentTimeMillis()
        )
    }
}
