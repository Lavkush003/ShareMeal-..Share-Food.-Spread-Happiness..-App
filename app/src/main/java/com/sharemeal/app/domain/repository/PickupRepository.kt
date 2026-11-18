package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DistributionInput
import com.sharemeal.app.domain.model.DistributionRecord
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.PickupSchedule
import com.sharemeal.app.domain.model.WorkflowEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinates the physical pickup and distribution workflow.
 *
 * Every transition is validated and audited. Implementations must enforce
 * authorization and prevent duplicate confirmations; a production backend must
 * perform the same checks atomically.
 */
interface PickupRepository {
    val pickups: StateFlow<List<PickupRecord>>
    val distributions: StateFlow<List<DistributionRecord>>
    val events: StateFlow<List<WorkflowEvent>>

    suspend fun getPickup(requestId: String): AppResult<PickupRecord>
    suspend fun getDistribution(requestId: String): AppResult<DistributionRecord>
    suspend fun getEvents(requestId: String): AppResult<List<WorkflowEvent>>

    suspend fun schedulePickup(requestId: String, schedule: PickupSchedule): AppResult<PickupRecord>
    suspend fun confirmNgoReady(
        requestId: String,
        volunteerName: String,
        volunteerPhone: String,
        note: String?
    ): AppResult<PickupRecord>
    suspend fun confirmDonorReady(
        requestId: String,
        contactPerson: String,
        collectionInstructions: String,
        note: String?
    ): AppResult<PickupRecord>
    suspend fun confirmCollection(
        requestId: String,
        actualCollectedQuantity: Int,
        discrepancyReason: String?,
        note: String?
    ): AppResult<PickupRecord>
    suspend fun confirmDistribution(
        requestId: String,
        input: DistributionInput
    ): AppResult<PickupRecord>
    suspend fun complete(requestId: String): AppResult<PickupRecord>
    suspend fun recordException(
        requestId: String,
        reason: PickupExceptionReason,
        note: String?
    ): AppResult<PickupRecord>
}
