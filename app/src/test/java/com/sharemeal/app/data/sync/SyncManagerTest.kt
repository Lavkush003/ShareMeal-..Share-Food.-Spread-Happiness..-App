package com.sharemeal.app.data.sync

import com.sharemeal.app.core.network.NetworkMonitor
import com.sharemeal.app.data.local.database.DonationHistoryEntity
import com.sharemeal.app.data.local.database.DonationRequestEntity
import com.sharemeal.app.data.local.database.DonationWithHistory
import com.sharemeal.app.data.local.database.DonationWithRequests
import com.sharemeal.app.data.local.database.DonorProfileEntity
import com.sharemeal.app.data.local.database.DonorWithDonations
import com.sharemeal.app.data.local.database.FoodDonationEntity
import com.sharemeal.app.data.local.database.NgoProfileEntity
import com.sharemeal.app.data.local.database.NgoWithRequests
import com.sharemeal.app.data.local.database.NotificationEntity
import com.sharemeal.app.data.local.database.PendingSyncEntity
import com.sharemeal.app.data.local.database.PickupEntity
import com.sharemeal.app.data.local.database.RequestWithPickup
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.data.local.database.UserEntity
import com.sharemeal.app.data.local.database.UserWithProfiles
import com.sharemeal.app.domain.model.PendingOperation
import com.sharemeal.app.domain.model.SyncConflict
import com.sharemeal.app.domain.model.SyncConflictCode
import com.sharemeal.app.domain.model.SyncOperationStatus
import com.sharemeal.app.domain.model.SyncOperationType
import com.sharemeal.app.domain.model.SyncRemoteResult
import com.sharemeal.app.domain.model.SyncRunOutcome
import com.sharemeal.app.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncManagerTest {

    @Test
    fun enqueueIgnoresDuplicateIdempotencyKey() = runTest {
        val dao = FakeShareMealDao()
        val manager = manager(dao, FakeSyncRemoteDataSource { successResult() })
        val first = operation(idempotencyKey = "stable-key", operationId = "op-1")
        val duplicate = first.copy(operationId = "op-2")

        assertTrue(manager.enqueue(first))
        assertFalse(manager.enqueue(duplicate))

        assertEquals(1, dao.operations.size)
        assertEquals("op-1", dao.operations.values.single().operationId)
    }

    @Test
    fun syncPendingReconcilesSuccessfulOperation() = runTest {
        val dao = FakeShareMealDao()
        val remote = FakeSyncRemoteDataSource { successResult() }
        val manager = manager(dao, remote)
        manager.enqueue(operation())

        val outcome = manager.syncPending()

        assertEquals(SyncRunOutcome.SUCCESS, outcome)
        assertEquals(1, remote.callCount)
        assertTrue(dao.operations.isEmpty())
        val update = dao.donationSyncUpdate!!
        assertEquals("server-1", update.serverId)
        assertEquals(SyncState.SYNCED.name, update.syncState)
        assertFalse(update.isDirty)
    }

    @Test
    fun syncPendingKeepsRetryableFailureInQueueWithBackoff() = runTest {
        val dao = FakeShareMealDao()
        val remote = FakeSyncRemoteDataSource {
            SyncRemoteResult.Failure("Temporary outage", retryable = true)
        }
        val manager = manager(dao, remote)
        manager.enqueue(operation())

        val outcome = manager.syncPending()

        assertEquals(SyncRunOutcome.RETRY, outcome)
        val queued = dao.operations.values.single()
        assertEquals(SyncOperationStatus.FAILED.name, queued.status)
        assertEquals(1, queued.attemptCount)
        assertNotNull(queued.nextRetryAt)
        assertEquals("Temporary outage", queued.lastError)
    }

    @Test
    fun syncPendingMarksConflictAndStoresServerSnapshot() = runTest {
        val dao = FakeShareMealDao()
        val remote = FakeSyncRemoteDataSource {
            SyncRemoteResult.Conflict(
                SyncConflict(
                    code = SyncConflictCode.QUANTITY_CHANGED,
                    message = "Quantity changed on server",
                    serverSnapshotJson = "{\"quantity\":10}"
                )
            )
        }
        val manager = manager(dao, remote)
        manager.enqueue(operation())

        val outcome = manager.syncPending()

        assertEquals(SyncRunOutcome.FAILED, outcome)
        val queued = dao.operations.values.single()
        assertEquals(SyncOperationStatus.CONFLICT.name, queued.status)
        assertEquals("{\"quantity\":10}", queued.serverSnapshotJson)
        assertEquals(SyncState.CONFLICT.name, dao.donationSyncUpdate?.syncState)
    }

    @Test
    fun syncPendingDoesNotCallRemoteWhenOffline() = runTest {
        val dao = FakeShareMealDao()
        val remote = FakeSyncRemoteDataSource { successResult() }
        val manager = manager(dao, remote, online = false)
        manager.enqueue(operation())

        val outcome = manager.syncPending()

        assertEquals(SyncRunOutcome.OFFLINE, outcome)
        assertEquals(0, remote.callCount)
        assertEquals(1, dao.operations.size)
    }

    private fun manager(
        dao: FakeShareMealDao,
        remote: SyncRemoteDataSource,
        online: Boolean = true
    ): SyncManager {
        val network = FakeNetworkMonitor(online)
        return SyncManager(
            dao = dao,
            remote = remote,
            networkMonitor = network,
            statusStore = SyncStatusStore(dao, network),
            scheduler = FakeSyncWorkScheduler()
        )
    }

    private fun operation(
        idempotencyKey: String = "key-1",
        operationId: String = "op-1",
        entityId: String = "donation-1"
    ): PendingOperation = PendingOperation.create(
        type = SyncOperationType.CREATE_DONATION,
        entityType = "donation",
        entityId = entityId,
        path = "/donations",
        idempotencyKey = idempotencyKey
    ).copy(operationId = operationId)

    private fun successResult() = SyncRemoteResult.Success(
        serverId = "server-1",
        serverUpdatedAt = 1_700_000_000_000L,
        responseJson = "{}"
    )

    private class FakeNetworkMonitor(online: Boolean) : NetworkMonitor {
        override val isOnline: StateFlow<Boolean> = MutableStateFlow(online)
    }

    private class FakeSyncWorkScheduler : SyncWorkScheduler {
        var immediateCount: Int = 0
        var periodicCount: Int = 0

        override fun scheduleImmediate() {
            immediateCount++
        }

        override fun schedulePeriodic() {
            periodicCount++
        }
    }

    private class FakeSyncRemoteDataSource(
        private val result: () -> SyncRemoteResult
    ) : SyncRemoteDataSource {
        var callCount: Int = 0

        override suspend fun send(operation: PendingOperation): SyncRemoteResult {
            callCount++
            return result()
        }
    }

    private data class DonationSyncUpdate(
        val serverId: String?,
        val serverUpdatedAt: Long?,
        val syncState: String,
        val isDirty: Boolean,
        val lastError: String?
    )

    private class FakeShareMealDao : ShareMealDao {
        val operations = linkedMapOf<String, PendingSyncEntity>()
        var donationSyncUpdate: DonationSyncUpdate? = null
        private val pendingCount = MutableStateFlow(0)
        private val conflictCount = MutableStateFlow(0)

        override suspend fun upsertUsers(users: List<UserEntity>) = Unit

        override suspend fun upsertUser(user: UserEntity) = Unit

        override suspend fun upsertDonorProfile(profile: DonorProfileEntity) = Unit

        override suspend fun upsertNgoProfile(profile: NgoProfileEntity) = Unit

        override suspend fun getUser(userId: String): UserEntity? = null

        override fun observeUsers(): Flow<List<UserEntity>> = MutableStateFlow(emptyList())

        override suspend fun getUserWithProfiles(userId: String): UserWithProfiles? = null

        override fun observeUserWithProfiles(userId: String): Flow<UserWithProfiles?> =
            MutableStateFlow(null)

        override suspend fun getDonorWithDonations(donorId: String): DonorWithDonations? = null

        override suspend fun upsertDonations(donations: List<FoodDonationEntity>) = Unit

        override suspend fun upsertDonation(donation: FoodDonationEntity) = Unit

        override suspend fun getDonation(donationId: String): FoodDonationEntity? = null

        override fun observeDonations(): Flow<List<FoodDonationEntity>> =
            MutableStateFlow(emptyList())

        override fun observeActiveDonations(now: Long): Flow<List<FoodDonationEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun getDraft(donorId: String): FoodDonationEntity? = null

        override fun observeDrafts(donorId: String): Flow<List<FoodDonationEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun upsertRequests(requests: List<DonationRequestEntity>) = Unit

        override suspend fun upsertRequest(request: DonationRequestEntity) = Unit

        override suspend fun getRequest(requestId: String): DonationRequestEntity? = null

        override fun observeRequests(): Flow<List<DonationRequestEntity>> =
            MutableStateFlow(emptyList())

        override fun observeDonationRequests(donationId: String): Flow<List<DonationRequestEntity>> =
            MutableStateFlow(emptyList())

        override fun observeNgoRequests(ngoId: String): Flow<List<DonationRequestEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun getRequestWithPickup(requestId: String): RequestWithPickup? = null

        override suspend fun getDonationWithRequests(donationId: String): DonationWithRequests? = null

        override suspend fun getNgoWithRequests(ngoId: String): NgoWithRequests? = null

        override suspend fun upsertPickup(pickup: PickupEntity) = Unit

        override suspend fun getPickup(requestId: String): PickupEntity? = null

        override fun observePickup(requestId: String): Flow<PickupEntity?> = MutableStateFlow(null)

        override suspend fun upsertNotifications(notifications: List<NotificationEntity>) = Unit

        override fun observeNotifications(userId: String): Flow<List<NotificationEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun markNotificationRead(notificationId: String) = Unit

        override suspend fun markAllNotificationsRead(userId: String) = Unit

        override suspend fun upsertHistory(events: List<DonationHistoryEntity>) =
            Unit

        override suspend fun deleteHistoryForDonation(donationId: String) = Unit

        override fun observeHistory(donationId: String): Flow<List<DonationHistoryEntity>> =
            MutableStateFlow(emptyList())

        override fun observeAllHistory(): Flow<List<DonationHistoryEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun getDonationWithHistory(donationId: String): DonationWithHistory? = null

        override suspend fun insertPendingOperation(operation: PendingSyncEntity): Long {
            if (operations.values.any { it.idempotencyKey == operation.idempotencyKey }) return -1
            operations[operation.operationId] = operation
            refreshCounts()
            return 1L
        }

        override suspend fun getPendingOperationByKey(idempotencyKey: String): PendingSyncEntity? =
            operations.values.firstOrNull { it.idempotencyKey == idempotencyKey }

        override suspend fun countPendingOperationsByKey(idempotencyKey: String): Int =
            operations.values.count { it.idempotencyKey == idempotencyKey }

        override fun observePendingOperations(): Flow<List<PendingSyncEntity>> =
            MutableStateFlow(operations.values.toList())

        override suspend fun getOperationsForRetry(now: Long, limit: Int): List<PendingSyncEntity> =
            operations.values
                .filter { entity ->
                    entity.status == SyncOperationStatus.PENDING.name ||
                        entity.status == SyncOperationStatus.FAILED.name
                }
                .filter { it.nextRetryAt == null || it.nextRetryAt <= now }
                .take(limit)

        override fun observePendingCount(): Flow<Int> = pendingCount.asStateFlow()

        override fun observeConflictCount(): Flow<Int> = conflictCount.asStateFlow()

        override suspend fun updatePendingOperation(
            operationId: String,
            status: String,
            attemptCount: Int,
            nextRetryAt: Long?,
            lastError: String?,
            serverSnapshotJson: String?
        ) {
            val current = operations[operationId] ?: return
            operations[operationId] = current.copy(
                status = status,
                attemptCount = attemptCount,
                nextRetryAt = nextRetryAt,
                lastError = lastError,
                serverSnapshotJson = serverSnapshotJson ?: current.serverSnapshotJson
            )
            refreshCounts()
        }

        override suspend fun deletePendingOperation(operationId: String) {
            operations.remove(operationId)
            refreshCounts()
        }

        override suspend fun updateDonationSync(
            entityId: String,
            serverId: String?,
            serverUpdatedAt: Long?,
            syncState: String,
            isDirty: Boolean,
            lastError: String?
        ) {
            donationSyncUpdate = DonationSyncUpdate(
                serverId = serverId,
                serverUpdatedAt = serverUpdatedAt,
                syncState = syncState,
                isDirty = isDirty,
                lastError = lastError
            )
        }

        override suspend fun updateRequestSync(
            entityId: String,
            serverId: String?,
            serverUpdatedAt: Long?,
            syncState: String,
            isDirty: Boolean,
            lastError: String?
        ) = Unit

        override suspend fun updatePickupSync(
            entityId: String,
            serverId: String?,
            serverUpdatedAt: Long?,
            syncState: String,
            isDirty: Boolean,
            lastError: String?
        ) = Unit

        override suspend fun updateUserSync(
            entityId: String,
            serverId: String?,
            serverUpdatedAt: Long?,
            syncState: String,
            isDirty: Boolean,
            lastError: String?
        ) = Unit

        private fun refreshCounts() {
            pendingCount.value = operations.values.count {
                it.status == SyncOperationStatus.PENDING.name ||
                    it.status == SyncOperationStatus.FAILED.name ||
                    it.status == SyncOperationStatus.IN_PROGRESS.name
            }
            conflictCount.value = operations.values.count {
                it.status == SyncOperationStatus.CONFLICT.name
            }
        }
    }
}
