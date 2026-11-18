package com.sharemeal.app.data.offline

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.FoodSortOrder
import com.sharemeal.app.domain.repository.FoodDiscoveryRepository
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
    private val dao: ShareMealDao
) : FoodDiscoveryRepository {

    override val discoveryItems: StateFlow<List<FoodDiscoveryItem>> = combine(
        cachedData.observeActiveDonations(),
        dao.observeUsers()
    ) { donations, users ->
        donations.map { donation ->
            FoodDiscoveryItem(
                donation = donation,
                donorName = users.firstOrNull { it.id == donation.donorId }?.displayName
                    ?: donation.contactPerson.ifBlank { "Cached Donor" }
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

    override suspend fun filterDonations(query: FoodDiscoveryQuery): List<FoodDiscoveryItem> {
        val now = System.currentTimeMillis()
        val filtered = discoveryItems.value.filter { item ->
            val donation = item.donation
            val matchesSearch = query.searchQuery.isBlank() ||
                donation.title.contains(query.searchQuery, ignoreCase = true) ||
                donation.description.contains(query.searchQuery, ignoreCase = true) ||
                donation.category.name.contains(query.searchQuery, ignoreCase = true) ||
                donation.city.contains(query.searchQuery, ignoreCase = true) ||
                item.donorName.contains(query.searchQuery, ignoreCase = true)
            val matchesCategory = query.category == null || donation.category == query.category
            val matchesClassification =
                query.classification == null || donation.classification == query.classification
            val matchesMinQuantity =
                query.minQuantity == null || donation.quantity >= query.minQuantity
            val matchesDistance = query.maxDistanceKm == null ||
                (item.distanceKm != null && item.distanceKm <= query.maxDistanceKm)
            val matchesDeadline = query.maxDeadlineHours == null ||
                donation.pickupDeadline <= now + query.maxDeadlineHours * 3600_000L

            matchesSearch && matchesCategory && matchesClassification &&
                matchesMinQuantity && matchesDistance && matchesDeadline
        }

        return when (query.sortBy) {
            FoodSortOrder.NEAREST -> filtered.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            FoodSortOrder.EARLIEST_DEADLINE -> filtered.sortedBy { it.donation.pickupDeadline }
            FoodSortOrder.NEWEST -> filtered.sortedByDescending { it.donation.createdAt }
        }
    }

    override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)

    private suspend fun toItem(donation: FoodDonation): FoodDiscoveryItem {
        val donorName = dao.getUser(donation.donorId)?.displayName
            ?: donation.contactPerson.ifBlank { "Cached Donor" }
        return FoodDiscoveryItem(donation = donation, donorName = donorName)
    }
}
