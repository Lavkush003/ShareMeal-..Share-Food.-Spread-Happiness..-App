package com.sharemeal.app.domain.model

enum class SyncConflictCode {
    QUANTITY_CHANGED,
    DONATION_CANCELLED,
    REQUEST_ALREADY_APPROVED,
    DEADLINE_EXPIRED,
    PROFILE_UPDATED_ELSEWHERE,
    VERSION_CONFLICT,
    UNKNOWN
}

data class SyncConflict(
    val code: SyncConflictCode,
    val message: String,
    val serverSnapshotJson: String? = null
)

sealed interface SyncRemoteResult {
    data class Success(
        val serverId: String?,
        val serverUpdatedAt: Long?,
        val responseJson: String
    ) : SyncRemoteResult

    data class Conflict(val conflict: SyncConflict) : SyncRemoteResult

    data class Failure(
        val message: String,
        val retryable: Boolean
    ) : SyncRemoteResult
}
