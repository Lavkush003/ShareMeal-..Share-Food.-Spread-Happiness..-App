package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.model.FoodDonation
import kotlinx.coroutines.flow.StateFlow

interface FoodDiscoveryRepository {
    val discoveryItems: StateFlow<List<FoodDiscoveryItem>>
    suspend fun getDonationById(donationId: String): AppResult<FoodDiscoveryItem>
    suspend fun filterDonations(query: FoodDiscoveryQuery): List<FoodDiscoveryItem>
    suspend fun refresh(): AppResult<Unit>
}
