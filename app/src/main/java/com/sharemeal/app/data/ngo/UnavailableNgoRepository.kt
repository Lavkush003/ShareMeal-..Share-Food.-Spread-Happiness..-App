package com.sharemeal.app.data.ngo

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.NgoDashboardStats
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.repository.NgoRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class UnavailableNgoRepository @Inject constructor() : NgoRepository {

    private val _donationRequests = MutableStateFlow<List<DonationRequest>>(emptyList())
    override val donationRequests: StateFlow<List<DonationRequest>> = _donationRequests.asStateFlow()

    override suspend fun getDashboardStats(ngoId: String): AppResult<NgoDashboardStats> = backendUnavailable()

    override suspend fun requestDonation(
        donationId: String,
        requestedQuantity: Int,
        notes: String?
    ): AppResult<DonationRequest> = backendUnavailable()

    override suspend fun updateRequestStatus(
        requestId: String,
        status: DonationRequestStatus
    ): AppResult<DonationRequest> = backendUnavailable()

    override suspend fun toggleVerificationStatus(ngoId: String): AppResult<NgoVerificationStatus> = backendUnavailable()

    override suspend fun setVerificationStatus(
        ngoId: String,
        status: NgoVerificationStatus
    ): AppResult<NgoVerificationStatus> = backendUnavailable()

    private fun <T> backendUnavailable(): AppResult<T> = AppResult.Failure(
        AppError.Unknown("NGO backend is not configured for this build.")
    )
}
