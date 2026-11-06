package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.FoodDonation
import kotlinx.coroutines.flow.StateFlow

interface DonationRepository {
    val donations: StateFlow<List<FoodDonation>>
    suspend fun publishDonation(draft: DonationDraft): AppResult<FoodDonation>
}
