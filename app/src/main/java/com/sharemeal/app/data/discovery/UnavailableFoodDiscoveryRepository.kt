package com.sharemeal.app.data.discovery

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.repository.FoodDiscoveryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class UnavailableFoodDiscoveryRepository @Inject constructor() : FoodDiscoveryRepository {

    private val _discoveryItems = MutableStateFlow<List<FoodDiscoveryItem>>(emptyList())
    override val discoveryItems: StateFlow<List<FoodDiscoveryItem>> = _discoveryItems.asStateFlow()

    override suspend fun getDonationById(donationId: String): AppResult<FoodDiscoveryItem> = backendUnavailable()

    override suspend fun filterDonations(query: FoodDiscoveryQuery): List<FoodDiscoveryItem> = emptyList()

    override suspend fun refresh(): AppResult<Unit> = backendUnavailable()

    private fun <T> backendUnavailable(): AppResult<T> = AppResult.Failure(
        AppError.Unknown("Food Discovery backend is not configured for this build.")
    )
}
