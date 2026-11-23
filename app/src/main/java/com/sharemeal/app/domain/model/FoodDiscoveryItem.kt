package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodDiscoveryItem(
    val donation: FoodDonation,
    val donorName: String,
    val distanceKm: Double? = null,
    val mapLocation: GeoPoint? = null,
    val availableQuantity: Int = donation.remainingQuantity
)
