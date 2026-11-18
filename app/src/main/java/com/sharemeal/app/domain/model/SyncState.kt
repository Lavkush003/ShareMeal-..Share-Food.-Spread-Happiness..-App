package com.sharemeal.app.domain.model

enum class SyncState {
    SYNCED,
    PENDING,
    IN_PROGRESS,
    FAILED,
    CONFLICT,
    OFFLINE;

    companion object {
        fun from(value: String?): SyncState =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}
