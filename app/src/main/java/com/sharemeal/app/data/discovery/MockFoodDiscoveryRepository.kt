package com.sharemeal.app.data.discovery

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.FoodSortOrder
import com.sharemeal.app.domain.repository.DonationRepository
import com.sharemeal.app.domain.repository.FoodDiscoveryRepository
import com.sharemeal.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Singleton
class MockFoodDiscoveryRepository @Inject constructor(
    private val donationRepository: DonationRepository,
    private val userRepository: UserRepository
) : FoodDiscoveryRepository {

    override val discoveryItems: StateFlow<List<FoodDiscoveryItem>> = combine(
        donationRepository.donations,
        userRepository.currentUser
    ) { donations, user ->
        donations
            .filter { it.isDiscoverable() }
            .map { donation ->
                val distance = calculateDistanceKm(donation, user?.address?.city)
                val donorName = donation.contactPerson.ifBlank { "Verified Donor" }
                FoodDiscoveryItem(
                    donation = donation,
                    donorName = donorName,
                    distanceKm = distance
                )
            }
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    override suspend fun getDonationById(donationId: String): AppResult<FoodDiscoveryItem> {
        val user = userRepository.currentUser.value
        val donation = donationRepository.donations.value.firstOrNull { it.id == donationId }
            ?: return AppResult.Failure(AppError.NotFound("Food donation listing not found."))

        val distance = calculateDistanceKm(donation, user?.address?.city)
        val item = FoodDiscoveryItem(
            donation = donation,
            donorName = donation.contactPerson.ifBlank { "Verified Donor" },
            distanceKm = distance
        )
        return AppResult.Success(item)
    }

    override suspend fun filterDonations(query: FoodDiscoveryQuery): List<FoodDiscoveryItem> {
        val user = userRepository.currentUser.value
        val allPublished = donationRepository.donations.value
            .filter { it.isDiscoverable() }
            .map { donation ->
                val distance = calculateDistanceKm(donation, user?.address?.city)
                FoodDiscoveryItem(
                    donation = donation,
                    donorName = donation.contactPerson.ifBlank { "Verified Donor" },
                    distanceKm = distance
                )
            }

        val now = System.currentTimeMillis()

        val filtered = allPublished.filter { item ->
            val donation = item.donation

            val matchesSearch = query.searchQuery.isBlank() ||
                donation.title.contains(query.searchQuery, ignoreCase = true) ||
                donation.description.contains(query.searchQuery, ignoreCase = true) ||
                donation.category.name.contains(query.searchQuery, ignoreCase = true) ||
                donation.city.contains(query.searchQuery, ignoreCase = true) ||
                item.donorName.contains(query.searchQuery, ignoreCase = true)

            val matchesCategory = query.category == null || donation.category == query.category

            val matchesClassification = query.classification == null || donation.classification == query.classification

            val matchesMinQuantity = query.minQuantity == null || donation.quantity >= query.minQuantity

            val matchesDistance = query.maxDistanceKm == null ||
                (item.distanceKm != null && item.distanceKm <= query.maxDistanceKm)

            val matchesDeadline = query.maxDeadlineHours == null ||
                (donation.pickupDeadline <= now + query.maxDeadlineHours * 3600_000L)

            matchesSearch && matchesCategory && matchesClassification && matchesMinQuantity && matchesDistance && matchesDeadline
        }

        return when (query.sortBy) {
            FoodSortOrder.NEAREST -> filtered.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            FoodSortOrder.EARLIEST_DEADLINE -> filtered.sortedBy { it.donation.pickupDeadline }
            FoodSortOrder.NEWEST -> filtered.sortedByDescending { it.donation.createdAt }
        }
    }

    override suspend fun refresh(): AppResult<Unit> = donationRepository.refreshDonations()

    private fun FoodDonation.isDiscoverable(): Boolean =
        (status == DonationStatus.PUBLISHED || status == DonationStatus.PARTIALLY_ALLOCATED) &&
            remainingQuantity > 0 && pickupDeadline > System.currentTimeMillis()

    private fun calculateDistanceKm(donation: FoodDonation, userCity: String?): Double {
        // Deterministic distance calculation for mock listings based on ID hash and city comparison
        if (userCity != null && !donation.city.equals(userCity, ignoreCase = true)) {
            return 12.5 + (donation.id.hashCode() % 15).coerceAtLeast(0)
        }
        val offset = (Math.abs(donation.id.hashCode()) % 80) / 10.0 + 0.5
        return Math.round(offset * 10.0) / 10.0
    }
}
