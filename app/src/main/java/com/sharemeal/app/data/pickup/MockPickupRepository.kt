package com.sharemeal.app.data.pickup

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.local.MockDonationStore
import com.sharemeal.app.domain.model.DistributionInput
import com.sharemeal.app.domain.model.DistributionRecord
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.PickupSchedule
import com.sharemeal.app.domain.model.WorkflowEvent
import com.sharemeal.app.domain.repository.PickupRepository
import com.sharemeal.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class MockPickupRepository @Inject constructor(
    private val store: MockDonationStore,
    private val userRepository: UserRepository
) : PickupRepository {

    override val pickups: StateFlow<List<PickupRecord>> = store.pickups
    override val distributions: StateFlow<List<DistributionRecord>> = store.distributions
    override val events: StateFlow<List<WorkflowEvent>> = store.events

    override suspend fun getPickup(requestId: String): AppResult<PickupRecord> = runCatching {
        pickups.value.firstOrNull { it.requestId == requestId }
            ?: throw AppError.NotFound("Pickup record not found.")
    }.toAppResult()

    override suspend fun getDistribution(requestId: String): AppResult<DistributionRecord> = runCatching {
        distributions.value.firstOrNull { it.requestId == requestId }
            ?: throw AppError.NotFound("Distribution record not found.")
    }.toAppResult()

    override suspend fun getEvents(requestId: String): AppResult<List<WorkflowEvent>> = AppResult.Success(
        events.value.filter { it.requestId == requestId }.sortedBy { it.createdAt }
    )

    override suspend fun schedulePickup(
        requestId: String,
        schedule: PickupSchedule
    ): AppResult<PickupRecord> = runCatching {
        store.schedulePickupWorkflow(currentUser(), requestId, schedule)
    }.toAppResult()

    override suspend fun confirmNgoReady(
        requestId: String,
        volunteerName: String,
        volunteerPhone: String,
        note: String?
    ): AppResult<PickupRecord> = runCatching {
        store.confirmNgoReady(currentUser(), requestId, volunteerName, volunteerPhone, note)
    }.toAppResult()

    override suspend fun confirmDonorReady(
        requestId: String,
        contactPerson: String,
        collectionInstructions: String,
        note: String?
    ): AppResult<PickupRecord> = runCatching {
        store.confirmDonorReady(currentUser(), requestId, contactPerson, collectionInstructions, note)
    }.toAppResult()

    override suspend fun confirmCollection(
        requestId: String,
        actualCollectedQuantity: Int,
        discrepancyReason: String?,
        note: String?
    ): AppResult<PickupRecord> = runCatching {
        store.confirmCollection(currentUser(), requestId, actualCollectedQuantity, discrepancyReason, note)
    }.toAppResult()

    override suspend fun confirmDistribution(
        requestId: String,
        input: DistributionInput
    ): AppResult<PickupRecord> = runCatching {
        store.confirmDistribution(currentUser(), requestId, input)
    }.toAppResult()

    override suspend fun complete(requestId: String): AppResult<PickupRecord> = runCatching {
        store.completePickup(currentUser(), requestId)
    }.toAppResult()

    override suspend fun recordException(
        requestId: String,
        reason: PickupExceptionReason,
        note: String?
    ): AppResult<PickupRecord> = runCatching {
        store.recordPickupException(currentUser(), requestId, reason, note)
    }.toAppResult()

    private suspend fun currentUser() = userRepository.currentUser.value
        ?: when (val result = userRepository.getCurrentUser()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> throw AppError.Unauthorized()
        }

    private fun <T> Result<T>.toAppResult(): AppResult<T> = fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { error ->
            when (error) {
                is AppError -> AppResult.Failure(error)
                is IllegalArgumentException -> AppResult.Failure(
                    AppError.Validation(message = error.message ?: "Invalid value.")
                )
                else -> AppResult.Failure(AppError.Unknown("Pickup operation failed.", cause = error))
            }
        }
    )
}
