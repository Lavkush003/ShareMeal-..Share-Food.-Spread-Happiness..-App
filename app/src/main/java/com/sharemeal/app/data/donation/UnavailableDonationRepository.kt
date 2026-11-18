package com.sharemeal.app.data.donation

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.repository.DonationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class UnavailableDonationRepository @Inject constructor() : DonationRepository {

    private val _donations = MutableStateFlow<List<FoodDonation>>(emptyList())
    override val donations: StateFlow<List<FoodDonation>> = _donations.asStateFlow()

    override suspend fun publishDonation(draft: DonationDraft): AppResult<FoodDonation> =
        backendUnavailable()

    override suspend fun getDonationById(donationId: String): AppResult<FoodDonation> =
        backendUnavailable()

    override suspend fun updateDonationAllocation(
        donationId: String,
        remainingQuantity: Int,
        status: DonationStatus
    ): AppResult<FoodDonation> = backendUnavailable()

    override suspend fun refreshDonations(): AppResult<Unit> = backendUnavailable()

    private fun <T> backendUnavailable(): AppResult<T> = AppResult.Failure(
        AppError.Unknown("Donation backend is not configured for this build.")
    )
}
