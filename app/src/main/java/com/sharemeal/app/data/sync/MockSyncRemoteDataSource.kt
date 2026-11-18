package com.sharemeal.app.data.sync

import com.sharemeal.app.domain.model.PendingOperation
import com.sharemeal.app.domain.model.SyncRemoteResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class MockSyncRemoteDataSource @Inject constructor() : SyncRemoteDataSource {
    override suspend fun send(operation: PendingOperation): SyncRemoteResult {
        delay(150)
        return SyncRemoteResult.Success(
            serverId = "mock_server_${UUID.randomUUID()}",
            serverUpdatedAt = System.currentTimeMillis(),
            responseJson = "{\"status\":\"accepted\"}"
        )
    }
}
