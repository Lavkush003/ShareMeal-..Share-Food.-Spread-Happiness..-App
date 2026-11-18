package com.sharemeal.app.data.request

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.local.MockDonationStore
import com.sharemeal.app.domain.model.DonationQuantityCalculator
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.repository.DonationRequestRepository
import com.sharemeal.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class MockDonationRequestRepository @Inject constructor(
    private val store: MockDonationStore,
    private val userRepository: UserRepository
) : DonationRequestRepository {

    override val requests: StateFlow<List<DonationRequest>> = store.requests

    override suspend fun getRequest(requestId: String): AppResult<DonationRequest> = runCatching {
        store.requests.value.firstOrNull { it.id == requestId }
            ?: throw AppError.NotFound("Request not found.")
    }.toAppResult()

    override suspend fun getQuantitySummary(donationId: String) = runCatching {
        val donation = store.getDonation(donationId)
            ?: throw AppError.NotFound("Donation listing not found.")
        DonationQuantityCalculator.calculate(donation, store.requests.value)
    }.toAppResult()

    override suspend fun getUserRequests(ngoId: String): AppResult<List<DonationRequest>> = AppResult.Success(
        store.requests.value.filter { it.ngoId == ngoId }.sortedByDescending { it.requestedAt }
    )

    override suspend fun getIncomingRequests(donorId: String): AppResult<List<DonationRequest>> = AppResult.Success(
        store.requests.value.filter { it.donorId == donorId }.sortedByDescending { it.requestedAt }
    )

    override suspend fun submitRequest(
        donationId: String,
        requestedQuantity: Int,
        peopleToServe: Int,
        expectedPickupAt: Long,
        notes: String?,
        idempotencyKey: String?
    ): AppResult<DonationRequest> = runCatching {
        val user = currentUser()
        store.submitRequest(
            user = user,
            donationId = donationId,
            requestedQuantity = requestedQuantity,
            peopleToServe = peopleToServe,
            expectedPickupAt = expectedPickupAt,
            notes = notes,
            idempotencyKey = idempotencyKey
        )
    }.toAppResult()

    override suspend fun approveRequest(
        requestId: String,
        approvedQuantity: Int,
        pickupScheduledTime: Long?
    ): AppResult<DonationRequest> = runCatching {
        store.approveRequest(currentUser().id, requestId, approvedQuantity, pickupScheduledTime)
    }.toAppResult()

    override suspend fun rejectRequest(
        requestId: String,
        reason: String?
    ): AppResult<DonationRequest> = runCatching {
        store.rejectRequest(currentUser().id, requestId, reason)
    }.toAppResult()

    override suspend fun cancelRequest(requestId: String): AppResult<DonationRequest> = runCatching {
        store.cancelRequest(currentUser().id, requestId)
    }.toAppResult()

    override suspend fun schedulePickup(
        requestId: String,
        pickupTime: Long
    ): AppResult<DonationRequest> = runCatching {
        store.schedulePickup(currentUser().id, requestId, pickupTime)
    }.toAppResult()

    override suspend fun markCollected(requestId: String): AppResult<DonationRequest> = runCatching {
        store.markCollected(currentUser().id, requestId)
    }.toAppResult()

    override suspend fun markCompleted(requestId: String): AppResult<DonationRequest> = runCatching {
        store.markCompleted(currentUser().id, requestId)
    }.toAppResult()

    override suspend fun expireOverdueRequests(): AppResult<Int> = runCatching {
        store.expireOverdueRequests()
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
                is IllegalArgumentException -> AppResult.Failure(AppError.Validation(message = error.message ?: "Invalid value."))
                else -> AppResult.Failure(AppError.Unknown("Request operation failed.", cause = error))
            }
        }
    )
}
