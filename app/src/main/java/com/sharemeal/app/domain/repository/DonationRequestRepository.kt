package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationQuantitySummary
import com.sharemeal.app.domain.model.DonationRequest
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for donation requests and their quantity allocation.
 *
 * Implementations must execute every mutation atomically: checking availability,
 * writing the request, updating the donation allocation and writing notifications
 * must succeed or fail together. Local mock implementations can only guarantee
 * atomicity inside one process; they are NOT a cross-device reservation.
 */
interface DonationRequestRepository {
    val requests: StateFlow<List<DonationRequest>>

    suspend fun getRequest(requestId: String): AppResult<DonationRequest>
    suspend fun getQuantitySummary(donationId: String): AppResult<DonationQuantitySummary>
    suspend fun getUserRequests(ngoId: String): AppResult<List<DonationRequest>>
    suspend fun getIncomingRequests(donorId: String): AppResult<List<DonationRequest>>

    suspend fun submitRequest(
        donationId: String,
        requestedQuantity: Int,
        peopleToServe: Int,
        expectedPickupAt: Long,
        notes: String?,
        idempotencyKey: String? = null
    ): AppResult<DonationRequest>

    suspend fun approveRequest(
        requestId: String,
        approvedQuantity: Int,
        pickupScheduledTime: Long? = null
    ): AppResult<DonationRequest>

    suspend fun rejectRequest(requestId: String, reason: String?): AppResult<DonationRequest>
    suspend fun cancelRequest(requestId: String): AppResult<DonationRequest>
    suspend fun schedulePickup(requestId: String, pickupTime: Long): AppResult<DonationRequest>
    suspend fun markCollected(requestId: String): AppResult<DonationRequest>
    suspend fun markCompleted(requestId: String): AppResult<DonationRequest>
    suspend fun expireOverdueRequests(): AppResult<Int>
}
