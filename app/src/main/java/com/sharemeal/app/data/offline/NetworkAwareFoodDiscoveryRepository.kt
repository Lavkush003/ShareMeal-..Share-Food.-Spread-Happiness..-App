package com.sharemeal.app.data.offline

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.network.NetworkMonitor
import com.sharemeal.app.data.discovery.MockFoodDiscoveryRepository
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.repository.FoodDiscoveryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NetworkAwareFoodDiscoveryRepository @Inject constructor(
    private val remote: MockFoodDiscoveryRepository,
    private val cached: CachedFoodDiscoveryRepository,
    private val networkMonitor: NetworkMonitor
) : FoodDiscoveryRepository {

    override val discoveryItems: StateFlow<List<FoodDiscoveryItem>> = networkMonitor.isOnline
        .flatMapLatest { online ->
            if (online) remote.discoveryItems else cached.discoveryItems
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    override suspend fun getDonationById(donationId: String): AppResult<FoodDiscoveryItem> =
        if (networkMonitor.isOnline.value) {
            remote.getDonationById(donationId)
        } else {
            cached.getDonationById(donationId)
        }

    override suspend fun filterDonations(query: FoodDiscoveryQuery): List<FoodDiscoveryItem> =
        if (networkMonitor.isOnline.value) {
            remote.filterDonations(query)
        } else {
            cached.filterDonations(query)
        }

    override suspend fun refresh(): AppResult<Unit> =
        if (networkMonitor.isOnline.value) remote.refresh() else cached.refresh()
}
