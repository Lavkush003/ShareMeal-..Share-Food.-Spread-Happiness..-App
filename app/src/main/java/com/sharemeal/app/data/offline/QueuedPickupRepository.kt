package com.sharemeal.app.data.offline

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.pickup.MockPickupRepository
import com.sharemeal.app.domain.model.DistributionInput
import com.sharemeal.app.domain.model.DistributionRecord
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.PickupSchedule
import com.sharemeal.app.domain.model.SyncOperationType
import com.sharemeal.app.domain.model.WorkflowEvent
import com.sharemeal.app.domain.repository.PickupRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class QueuedPickupRepository @Inject constructor(
    private val delegate: MockPickupRepository,
    private val operations: OfflineOperationQueue
) : PickupRepository {

    override val pickups: StateFlow<List<PickupRecord>> = delegate.pickups
    override val distributions: StateFlow<List<DistributionRecord>> = delegate.distributions
    override val events: StateFlow<List<WorkflowEvent>> = delegate.events

    override suspend fun getPickup(requestId: String): AppResult<PickupRecord> =
        delegate.getPickup(requestId)

    override suspend fun getDistribution(requestId: String): AppResult<DistributionRecord> =
        delegate.getDistribution(requestId)

    override suspend fun getEvents(requestId: String): AppResult<List<WorkflowEvent>> =
        delegate.getEvents(requestId)

    override suspend fun schedulePickup(
        requestId: String,
        schedule: PickupSchedule
    ): AppResult<PickupRecord> = enqueueResult(
        delegate.schedulePickup(requestId, schedule),
        SyncOperationType.SCHEDULE_PICKUP
    )

    override suspend fun confirmNgoReady(
        requestId: String,
        volunteerName: String,
        volunteerPhone: String,
        note: String?
    ): AppResult<PickupRecord> = enqueueResult(
        delegate.confirmNgoReady(requestId, volunteerName, volunteerPhone, note),
        SyncOperationType.NGO_READY
    )

    override suspend fun confirmDonorReady(
        requestId: String,
        contactPerson: String,
        collectionInstructions: String,
        note: String?
    ): AppResult<PickupRecord> = enqueueResult(
        delegate.confirmDonorReady(requestId, contactPerson, collectionInstructions, note),
        SyncOperationType.DONOR_READY
    )

    override suspend fun confirmCollection(
        requestId: String,
        actualCollectedQuantity: Int,
        discrepancyReason: String?,
        note: String?
    ): AppResult<PickupRecord> = enqueueResult(
        delegate.confirmCollection(
            requestId,
            actualCollectedQuantity,
            discrepancyReason,
            note
        ),
        SyncOperationType.COLLECT
    )

    override suspend fun confirmDistribution(
        requestId: String,
        input: DistributionInput
    ): AppResult<PickupRecord> = enqueueResult(
        delegate.confirmDistribution(requestId, input),
        SyncOperationType.DISTRIBUTE
    )

    override suspend fun complete(requestId: String): AppResult<PickupRecord> = enqueueResult(
        delegate.complete(requestId),
        SyncOperationType.COMPLETE_PICKUP
    )

    override suspend fun recordException(
        requestId: String,
        reason: PickupExceptionReason,
        note: String?
    ): AppResult<PickupRecord> = enqueueResult(
        delegate.recordException(requestId, reason, note),
        SyncOperationType.RECORD_EXCEPTION
    )

    private suspend fun enqueueResult(
        result: AppResult<PickupRecord>,
        type: SyncOperationType
    ): AppResult<PickupRecord> {
        if (result is AppResult.Success) {
            operations.enqueuePickup(result.data, type)
        }
        return result
    }
}
