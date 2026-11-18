package com.sharemeal.app.data.pickup

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DistributionInput
import com.sharemeal.app.domain.model.DistributionRecord
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.PickupSchedule
import com.sharemeal.app.domain.model.WorkflowEvent
import com.sharemeal.app.domain.repository.PickupRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class UnavailablePickupRepository @Inject constructor() : PickupRepository {

    private val _pickups = MutableStateFlow<List<PickupRecord>>(emptyList())
    override val pickups: StateFlow<List<PickupRecord>> = _pickups.asStateFlow()

    private val _distributions = MutableStateFlow<List<DistributionRecord>>(emptyList())
    override val distributions: StateFlow<List<DistributionRecord>> = _distributions.asStateFlow()

    private val _events = MutableStateFlow<List<WorkflowEvent>>(emptyList())
    override val events: StateFlow<List<WorkflowEvent>> = _events.asStateFlow()

    override suspend fun getPickup(requestId: String): AppResult<PickupRecord> = backendUnavailable()
    override suspend fun getDistribution(requestId: String): AppResult<DistributionRecord> = backendUnavailable()
    override suspend fun getEvents(requestId: String): AppResult<List<WorkflowEvent>> = backendUnavailable()

    override suspend fun schedulePickup(
        requestId: String,
        schedule: PickupSchedule
    ): AppResult<PickupRecord> = backendUnavailable()

    override suspend fun confirmNgoReady(
        requestId: String,
        volunteerName: String,
        volunteerPhone: String,
        note: String?
    ): AppResult<PickupRecord> = backendUnavailable()

    override suspend fun confirmDonorReady(
        requestId: String,
        contactPerson: String,
        collectionInstructions: String,
        note: String?
    ): AppResult<PickupRecord> = backendUnavailable()

    override suspend fun confirmCollection(
        requestId: String,
        actualCollectedQuantity: Int,
        discrepancyReason: String?,
        note: String?
    ): AppResult<PickupRecord> = backendUnavailable()

    override suspend fun confirmDistribution(
        requestId: String,
        input: DistributionInput
    ): AppResult<PickupRecord> = backendUnavailable()

    override suspend fun complete(requestId: String): AppResult<PickupRecord> = backendUnavailable()

    override suspend fun recordException(
        requestId: String,
        reason: PickupExceptionReason,
        note: String?
    ): AppResult<PickupRecord> = backendUnavailable()

    private fun <T> backendUnavailable(): AppResult<T> = AppResult.Failure(
        AppError.Unknown("Pickup workflow backend is not configured for this build.")
    )
}
