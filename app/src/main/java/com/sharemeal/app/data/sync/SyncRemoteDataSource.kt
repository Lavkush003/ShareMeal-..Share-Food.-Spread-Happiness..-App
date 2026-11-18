package com.sharemeal.app.data.sync

import com.sharemeal.app.domain.model.PendingOperation
import com.sharemeal.app.domain.model.SyncRemoteResult

interface SyncRemoteDataSource {
    suspend fun send(operation: PendingOperation): SyncRemoteResult
}
