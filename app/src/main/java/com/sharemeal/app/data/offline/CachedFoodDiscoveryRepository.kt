package com.sharemeal.app.data.offline

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.domain.location.NearbyDiscovery
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.repository.FoodDiscoveryRepository
import com.sharemeal.app.domain.repository.SearchLocationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

@Singleton
class CachedFoodDiscoveryRepository @Inject constructor(
    private val cachedData: RoomCachedDataRepository,
    private val dao: ShareMealDao,
    private val searchLocationRepository: SearchLocationRepository
) : FoodDiscoveryRepository {

    override val discoveryItems: StateFlow<List<FoodDiscoveryItem>> = combine(
        cachedData.observeActiveDonations(),
        dao.observeUsers(),
        searchLocationRepository.selectedLocation
    ) { donations, users, location ->
        donations.map { donation ->
            FoodDiscoveryItem(
                donation = donation,
                donorName = users.firstOrNull { it.id == donation.donorId }?.displayName
                    ?: donation.contactPerson.ifBlank { "Cached Donor" },
                distanceKm = NearbyDiscovery.distanceKm(donation, location?.point),
                mapLocation = NearbyDiscovery.mapLocation(donation)
            )
        }
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    override suspend fun getDonationById(donationId: String): AppResult<FoodDiscoveryItem> {
        val donation = cachedData.getDonation(donationId)
            ?: return AppResult.Failure(AppError.NotFound("Donation listing is not cached."))
        return AppResult.Success(toItem(donation))
    }

    override suspend fun filterDonations(query: FoodDiscoveryQuery): List<FoodDiscoveryItem> =
        NearbyDiscovery.filter(discoveryItems.value, query)

    override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)

    private suspend fun toItem(donation: FoodDonation): FoodDiscoveryItem {
        val donorName = dao.getUser(donation.donorId)?.displayName
            ?: donation.contactPerson.ifBlank { "Cached Donor" }
        return FoodDiscoveryItem(
            donation = donation,
            donorName = donorName,
            distanceKm = NearbyDiscovery.distanceKm(
                donation,
                searchLocationRepository.selectedLocation.value?.point
            ),
            mapLocation = NearbyDiscovery.mapLocation(donation)
        )
    }
}
