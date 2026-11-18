package com.sharemeal.app.data.offline

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.data.request.MockDonationRequestRepository
import com.sharemeal.app.domain.model.DonationQuantitySummary
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.SyncOperationStatus
import com.sharemeal.app.domain.model.SyncOperationType
import com.sharemeal.app.domain.repository.DonationRequestRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

@Singleton
class QueuedDonationRequestRepository @Inject constructor(
    private val delegate: MockDonationRequestRepository,
    private val operations: OfflineOperationQueue,
    private val dao: ShareMealDao
) : DonationRequestRepository {

    override val requests: StateFlow<List<DonationRequest>> = combine(
        delegate.requests,
        dao.observePendingOperations()
    ) { requests, pendingOperations ->
        val pendingIds = pendingOperations
            .filter { it.entityType == "request" && it.status != SyncOperationStatus.SYNCED.name }
            .map { it.entityId }
            .toSet()
        requests.map { it.copy(syncPending = it.id in pendingIds) }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    override suspend fun getRequest(requestId: String): AppResult<DonationRequest> =
        markPendingRequest(delegate.getRequest(requestId))

    override suspend fun getQuantitySummary(donationId: String): AppResult<DonationQuantitySummary> =
        delegate.getQuantitySummary(donationId)

    override suspend fun getUserRequests(ngoId: String): AppResult<List<DonationRequest>> =
        markPendingRequests(delegate.getUserRequests(ngoId))

    override suspend fun getIncomingRequests(donorId: String): AppResult<List<DonationRequest>> =
        markPendingRequests(delegate.getIncomingRequests(donorId))

    override suspend fun submitRequest(
        donationId: String,
        requestedQuantity: Int,
        peopleToServe: Int,
        expectedPickupAt: Long,
        notes: String?,
        idempotencyKey: String?
    ): AppResult<DonationRequest> = enqueueResult(
        delegate.submitRequest(
            donationId,
            requestedQuantity,
            peopleToServe,
            expectedPickupAt,
            notes,
            idempotencyKey
        ),
        SyncOperationType.SUBMIT_REQUEST
    )

    override suspend fun approveRequest(
        requestId: String,
        approvedQuantity: Int,
        pickupScheduledTime: Long?
    ): AppResult<DonationRequest> = enqueueResult(
        delegate.approveRequest(requestId, approvedQuantity, pickupScheduledTime),
        SyncOperationType.APPROVE_REQUEST
    )

    override suspend fun rejectRequest(
        requestId: String,
        reason: String?
    ): AppResult<DonationRequest> = enqueueResult(
        delegate.rejectRequest(requestId, reason),
        SyncOperationType.REJECT_REQUEST
    )

    override suspend fun cancelRequest(requestId: String): AppResult<DonationRequest> =
        enqueueResult(delegate.cancelRequest(requestId), SyncOperationType.CANCEL_REQUEST)

    override suspend fun schedulePickup(
        requestId: String,
        pickupTime: Long
    ): AppResult<DonationRequest> = enqueueResult(
        delegate.schedulePickup(requestId, pickupTime),
        SyncOperationType.SCHEDULE_PICKUP
    )

    override suspend fun markCollected(requestId: String): AppResult<DonationRequest> =
        enqueueResult(delegate.markCollected(requestId), SyncOperationType.COLLECT)

    override suspend fun markCompleted(requestId: String): AppResult<DonationRequest> =
        enqueueResult(delegate.markCompleted(requestId), SyncOperationType.COMPLETE_PICKUP)

    override suspend fun expireOverdueRequests(): AppResult<Int> = delegate.expireOverdueRequests()

    private suspend fun enqueueResult(
        result: AppResult<DonationRequest>,
        type: SyncOperationType
    ): AppResult<DonationRequest> = when (result) {
        is AppResult.Success -> {
            operations.enqueueRequest(result.data, type)
            AppResult.Success(result.data.copy(syncPending = true))
        }

        is AppResult.Failure -> result
    }

    private suspend fun markPendingRequest(
        result: AppResult<DonationRequest>
    ): AppResult<DonationRequest> = when (result) {
        is AppResult.Success -> AppResult.Success(
            result.data.copy(syncPending = isPending(result.data.id))
        )

        is AppResult.Failure -> result
    }

    private suspend fun markPendingRequests(
        result: AppResult<List<DonationRequest>>
    ): AppResult<List<DonationRequest>> = when (result) {
        is AppResult.Success -> {
            val pendingIds = pendingRequestIds()
            AppResult.Success(result.data.map { it.copy(syncPending = it.id in pendingIds) })
        }

        is AppResult.Failure -> result
    }

    private suspend fun isPending(requestId: String): Boolean =
        dao.observePendingOperations().first().any {
            it.entityType == "request" &&
                it.entityId == requestId &&
                it.status != SyncOperationStatus.SYNCED.name
        }

    private suspend fun pendingRequestIds(): Set<String> = dao.observePendingOperations()
        .first()
        .filter { it.entityType == "request" && it.status != SyncOperationStatus.SYNCED.name }
        .map { it.entityId }
        .toSet()
}
