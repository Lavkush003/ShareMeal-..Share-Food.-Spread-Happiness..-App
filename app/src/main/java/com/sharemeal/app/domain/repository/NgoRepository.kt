package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.NgoDashboardStats
import com.sharemeal.app.domain.model.NgoVerificationStatus
import kotlinx.coroutines.flow.StateFlow

interface NgoRepository {
    val donationRequests: StateFlow<List<DonationRequest>>
    suspend fun getDashboardStats(ngoId: String): AppResult<NgoDashboardStats>
    suspend fun requestDonation(donationId: String, requestedQuantity: Int, notes: String?): AppResult<DonationRequest>
    suspend fun updateRequestStatus(requestId: String, status: DonationRequestStatus): AppResult<DonationRequest>
    suspend fun toggleVerificationStatus(ngoId: String): AppResult<NgoVerificationStatus>
    suspend fun setVerificationStatus(ngoId: String, status: NgoVerificationStatus): AppResult<NgoVerificationStatus>
}
