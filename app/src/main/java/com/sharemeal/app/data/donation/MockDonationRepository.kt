package com.sharemeal.app.data.donation

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.local.MockDonationSeed
import com.sharemeal.app.data.local.MockDonationStore
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.repository.DonationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class MockDonationRepository @Inject constructor(
    private val store: MockDonationStore
) : DonationRepository {

    constructor() : this(MockDonationStore())

    override val donations: StateFlow<List<FoodDonation>> = store.donations

    override suspend fun publishDonation(draft: DonationDraft): AppResult<FoodDonation> = try {
        AppResult.Success(store.publish(draft, MockDonationSeed.DemoDonorId))
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown("Could not publish donation.", cause = error))
    }

    override suspend fun getDonationById(donationId: String): AppResult<FoodDonation> = try {
        val donation = store.getDonation(donationId)
            ?: return AppResult.Failure(AppError.NotFound("Donation listing not found."))
        AppResult.Success(donation)
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown("Could not load donation.", cause = error))
    }

    override suspend fun updateDonationAllocation(
        donationId: String,
        remainingQuantity: Int,
        status: DonationStatus
    ): AppResult<FoodDonation> = try {
        AppResult.Success(store.setAllocation(donationId, remainingQuantity, status))
    } catch (error: IllegalArgumentException) {
        AppResult.Failure(AppError.Validation(message = error.message ?: "Invalid allocation."))
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown("Could not update allocation.", cause = error))
    }

    override suspend fun refreshDonations(): AppResult<Unit> = try {
        store.refresh()
        AppResult.Success(Unit)
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown("Could not refresh donations.", cause = error))
    }
}
