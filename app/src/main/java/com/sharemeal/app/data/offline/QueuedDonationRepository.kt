package com.sharemeal.app.data.offline

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.donation.MockDonationRepository
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.repository.DonationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class QueuedDonationRepository @Inject constructor(
    private val delegate: MockDonationRepository,
    private val operations: OfflineOperationQueue
) : DonationRepository {

    override val donations: StateFlow<List<FoodDonation>> = delegate.donations

    override suspend fun publishDonation(draft: DonationDraft): AppResult<FoodDonation> =
        when (val result = delegate.publishDonation(draft)) {
            is AppResult.Success -> {
                operations.enqueueDonationCreated(result.data)
                result
            }

            is AppResult.Failure -> result
        }

    override suspend fun getDonationById(donationId: String): AppResult<FoodDonation> =
        delegate.getDonationById(donationId)

    override suspend fun updateDonationAllocation(
        donationId: String,
        remainingQuantity: Int,
        status: DonationStatus
    ): AppResult<FoodDonation> = delegate.updateDonationAllocation(
        donationId,
        remainingQuantity,
        status
    )

    override suspend fun refreshDonations(): AppResult<Unit> = delegate.refreshDonations()
}
