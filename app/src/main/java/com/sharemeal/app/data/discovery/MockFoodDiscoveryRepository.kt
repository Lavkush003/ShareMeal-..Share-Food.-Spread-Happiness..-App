package com.sharemeal.app.data.discovery

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.location.NearbyDiscovery
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.GeoPoint
import com.sharemeal.app.domain.repository.DonationRepository
import com.sharemeal.app.domain.repository.FoodDiscoveryRepository
import com.sharemeal.app.domain.repository.SearchLocationRepository
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
    private val userRepository: UserRepository,
    private val searchLocationRepository: SearchLocationRepository
) : FoodDiscoveryRepository {

    override val discoveryItems: StateFlow<List<FoodDiscoveryItem>> = combine(
        donationRepository.donations,
        userRepository.currentUser,
        searchLocationRepository.selectedLocation
    ) { donations, user, location ->
        donations
            .filter { it.isDiscoverable() }
            .map { donation ->
                val distance = calculateDistanceKm(
                    donation = donation,
                    center = location?.point,
                    userCity = user?.address?.city
                )
                FoodDiscoveryItem(
                    donation = donation,
                    donorName = donation.contactPerson.ifBlank { "Verified Donor" },
                    distanceKm = distance,
                    mapLocation = NearbyDiscovery.mapLocation(donation)
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

        val center = searchLocationRepository.selectedLocation.value?.point
        val distance = calculateDistanceKm(donation, center, user?.address?.city)
        val item = FoodDiscoveryItem(
            donation = donation,
            donorName = donation.contactPerson.ifBlank { "Verified Donor" },
            distanceKm = distance,
            mapLocation = NearbyDiscovery.mapLocation(donation)
        )
        return AppResult.Success(item)
    }

    override suspend fun filterDonations(query: FoodDiscoveryQuery): List<FoodDiscoveryItem> {
        val user = userRepository.currentUser.value
        val center = query.center ?: searchLocationRepository.selectedLocation.value?.point
        val allPublished = donationRepository.donations.value
            .filter { it.isDiscoverable() }
            .map { donation ->
                FoodDiscoveryItem(
                    donation = donation,
                    donorName = donation.contactPerson.ifBlank { "Verified Donor" },
                    distanceKm = calculateDistanceKm(donation, center, user?.address?.city),
                    mapLocation = NearbyDiscovery.mapLocation(donation)
                )
            }
        return NearbyDiscovery.filter(allPublished, query)
    }

    override suspend fun refresh(): AppResult<Unit> = donationRepository.refreshDonations()

    private fun FoodDonation.isDiscoverable(): Boolean =
        (status == DonationStatus.PUBLISHED || status == DonationStatus.PARTIALLY_ALLOCATED) &&
            remainingQuantity > 0 && pickupDeadline > System.currentTimeMillis()

    private fun calculateDistanceKm(
        donation: FoodDonation,
        center: GeoPoint?,
        userCity: String?
    ): Double {
        NearbyDiscovery.distanceKm(donation, center)?.let { return it }
        if (userCity != null && !donation.city.equals(userCity, ignoreCase = true)) {
            return 12.5 + (donation.id.hashCode() % 15).coerceAtLeast(0)
        }
        val offset = (Math.abs(donation.id.hashCode()) % 80) / 10.0 + 0.5
        return Math.round(offset * 10.0) / 10.0
    }
}
