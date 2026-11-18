package com.sharemeal.app.data.request

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationQuantitySummary
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.repository.DonationRequestRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class UnavailableDonationRequestRepository @Inject constructor() : DonationRequestRepository {

    private val _requests = MutableStateFlow<List<DonationRequest>>(emptyList())
    override val requests: StateFlow<List<DonationRequest>> = _requests.asStateFlow()

    override suspend fun getRequest(requestId: String): AppResult<DonationRequest> = backendUnavailable()
    override suspend fun getQuantitySummary(donationId: String): AppResult<DonationQuantitySummary> = backendUnavailable()
    override suspend fun getUserRequests(ngoId: String): AppResult<List<DonationRequest>> = backendUnavailable()
    override suspend fun getIncomingRequests(donorId: String): AppResult<List<DonationRequest>> = backendUnavailable()

    override suspend fun submitRequest(
        donationId: String,
        requestedQuantity: Int,
        peopleToServe: Int,
        expectedPickupAt: Long,
        notes: String?,
        idempotencyKey: String?
    ): AppResult<DonationRequest> = backendUnavailable()

    override suspend fun approveRequest(
        requestId: String,
        approvedQuantity: Int,
        pickupScheduledTime: Long?
    ): AppResult<DonationRequest> = backendUnavailable()

    override suspend fun rejectRequest(requestId: String, reason: String?): AppResult<DonationRequest> = backendUnavailable()
    override suspend fun cancelRequest(requestId: String): AppResult<DonationRequest> = backendUnavailable()
    override suspend fun schedulePickup(requestId: String, pickupTime: Long): AppResult<DonationRequest> = backendUnavailable()
    override suspend fun markCollected(requestId: String): AppResult<DonationRequest> = backendUnavailable()
    override suspend fun markCompleted(requestId: String): AppResult<DonationRequest> = backendUnavailable()
    override suspend fun expireOverdueRequests(): AppResult<Int> = backendUnavailable()

    private fun <T> backendUnavailable(): AppResult<T> = AppResult.Failure(
        AppError.Unknown("Donation request backend is not configured for this build.")
    )
}
