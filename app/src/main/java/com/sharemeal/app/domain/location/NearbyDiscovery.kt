package com.sharemeal.app.domain.location

import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.FoodSortOrder

object NearbyDiscovery {
    fun distanceKm(donation: FoodDonation, center: com.sharemeal.app.domain.model.GeoPoint?): Double? {
        val location = donation.location ?: return null
        val origin = center ?: return null
        return DistanceCalculator.distanceKm(origin, location)
    }

    fun mapLocation(donation: FoodDonation) = donation.location?.let(DistanceCalculator::approximate)

    fun filter(items: List<FoodDiscoveryItem>, query: FoodDiscoveryQuery): List<FoodDiscoveryItem> {
        val now = System.currentTimeMillis()
        val filtered = items.filter { item ->
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
            val matchesMinQuantity = query.minQuantity == null || donation.quantity >= query.minQuantity
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
}
