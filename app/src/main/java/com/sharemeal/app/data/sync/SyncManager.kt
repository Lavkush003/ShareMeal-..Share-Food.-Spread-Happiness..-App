package com.sharemeal.app.data.sync

import com.sharemeal.app.core.network.NetworkMonitor
import com.sharemeal.app.data.local.database.RoomEntityMapper
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.domain.model.PendingOperation
import com.sharemeal.app.domain.model.SyncOperationStatus
import com.sharemeal.app.domain.model.SyncRemoteResult
import com.sharemeal.app.domain.model.SyncRunOutcome
import com.sharemeal.app.domain.model.SyncState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val dao: ShareMealDao,
    private val remote: SyncRemoteDataSource,
    private val networkMonitor: NetworkMonitor,
    private val statusStore: SyncStatusStore,
    private val scheduler: SyncWorkScheduler
) {

    suspend fun enqueue(operation: PendingOperation): Boolean {
        if (dao.getPendingOperationByKey(operation.idempotencyKey) != null) {
            scheduleImmediateSync()
            return false
        }
        dao.insertPendingOperation(RoomEntityMapper.toEntity(operation))
        scheduleImmediateSync()
        return true
    }

    fun scheduleImmediateSync() {
        scheduler.scheduleImmediate()
    }

    fun schedulePeriodicSync() {
        scheduler.schedulePeriodic()
    }

    suspend fun syncPending(): SyncRunOutcome {
        if (!networkMonitor.isOnline.value) {
            statusStore.markFailure("Device is offline. Changes are saved on this device.")
            return SyncRunOutcome.OFFLINE
        }

        val operations = dao.getOperationsForRetry(System.currentTimeMillis(), MAX_BATCH_SIZE)
        if (operations.isEmpty()) {
            statusStore.markSuccess()
            return SyncRunOutcome.SUCCESS
        }

        var retryableFailure = false
        var permanentFailure = false
        var conflict = false

        operations.forEach { entity ->
            val operation = RoomEntityMapper.toDomain(entity)
            val attempt = operation.attemptCount + 1
            dao.updatePendingOperation(
                operationId = operation.operationId,
                status = SyncOperationStatus.IN_PROGRESS.name,
                attemptCount = attempt,
                nextRetryAt = null,
                lastError = null,
                serverSnapshotJson = operation.serverSnapshotJson
            )

            when (val result = remote.send(operation)) {
                is SyncRemoteResult.Success -> {
                    updateEntitySync(
                        entityType = operation.entityType,
                        entityId = operation.entityId,
                        state = SyncState.SYNCED,
                        serverId = result.serverId,
                        serverUpdatedAt = result.serverUpdatedAt,
                        error = null
                    )
                    dao.deletePendingOperation(operation.operationId)
                }

                is SyncRemoteResult.Conflict -> {
                    conflict = true
                    val message = result.conflict.message
                    dao.updatePendingOperation(
                        operationId = operation.operationId,
                        status = SyncOperationStatus.CONFLICT.name,
                        attemptCount = attempt,
                        nextRetryAt = null,
                        lastError = message,
                        serverSnapshotJson = result.conflict.serverSnapshotJson
                    )
                    updateEntitySync(
                        entityType = operation.entityType,
                        entityId = operation.entityId,
                        state = SyncState.CONFLICT,
                        serverId = null,
                        serverUpdatedAt = null,
                        error = "${result.conflict.code}: $message"
                    )
                }

                is SyncRemoteResult.Failure -> {
                    if (result.retryable) {
                        retryableFailure = true
                    } else {
                        permanentFailure = true
                    }
                    dao.updatePendingOperation(
                        operationId = operation.operationId,
                        status = SyncOperationStatus.FAILED.name,
                        attemptCount = attempt,
                        nextRetryAt = if (result.retryable) {
                            System.currentTimeMillis() + retryDelay(attempt)
                        } else {
                            null
                        },
                        lastError = result.message,
                        serverSnapshotJson = operation.serverSnapshotJson
                    )
                    if (!result.retryable) {
                        updateEntitySync(
                            entityType = operation.entityType,
                            entityId = operation.entityId,
                            state = SyncState.FAILED,
                            serverId = null,
                            serverUpdatedAt = null,
                            error = result.message
                        )
                    }
                }
            }
        }

        statusStore.markFailure(
            when {
                conflict -> "Some changes need review because the server data has changed."
                permanentFailure -> "Some changes could not be synchronized."
                retryableFailure -> "Connection problem. ShareMeal will retry automatically."
                else -> null
            }
        )
        if (!retryableFailure && !permanentFailure && !conflict) {
            statusStore.markSuccess()
        }

        return when {
            retryableFailure -> SyncRunOutcome.RETRY
            conflict || permanentFailure -> SyncRunOutcome.FAILED
            else -> SyncRunOutcome.SUCCESS
        }
    }

    private suspend fun updateEntitySync(
        entityType: String,
        entityId: String,
        state: SyncState,
        serverId: String?,
        serverUpdatedAt: Long?,
        error: String?
    ) {
        val syncState = state.name
        val isDirty = state != SyncState.SYNCED
        when (entityType) {
            "donation" -> dao.updateDonationSync(
                entityId, serverId, serverUpdatedAt, syncState, isDirty, error
            )

            "request" -> dao.updateRequestSync(
                entityId, serverId, serverUpdatedAt, syncState, isDirty, error
            )

            "pickup" -> dao.updatePickupSync(
                entityId, serverId, serverUpdatedAt, syncState, isDirty, error
            )

            "profile" -> dao.updateUserSync(
                entityId, serverId, serverUpdatedAt, syncState, isDirty, error
            )
        }
    }

    private fun retryDelay(attempt: Int): Long {
        val exponent = (attempt - 1).coerceIn(0, 5)
        return (30_000L shl exponent).coerceAtMost(15 * 60_000L)
    }

    companion object {
        const val WORK_NAME = "sharemeal_sync_now"
        const val PERIODIC_WORK_NAME = "sharemeal_periodic_sync"
        const val MAX_BATCH_SIZE = 20
    }
}
