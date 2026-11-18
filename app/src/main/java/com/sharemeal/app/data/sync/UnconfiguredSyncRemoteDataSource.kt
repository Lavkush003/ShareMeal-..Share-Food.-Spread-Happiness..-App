package com.sharemeal.app.data.sync

import com.sharemeal.app.domain.model.PendingOperation
import com.sharemeal.app.domain.model.SyncRemoteResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnconfiguredSyncRemoteDataSource @Inject constructor() : SyncRemoteDataSource {
    override suspend fun send(operation: PendingOperation): SyncRemoteResult =
        SyncRemoteResult.Failure(
            message = "Remote synchronization is not configured for this build.",
            retryable = false
        )
}
