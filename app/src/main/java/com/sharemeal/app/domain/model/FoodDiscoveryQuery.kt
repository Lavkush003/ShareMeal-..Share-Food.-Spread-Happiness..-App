package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodDiscoveryQuery(
    val searchQuery: String = "",
    val category: FoodCategory? = null,
    val classification: FoodClassification? = null,
    val minQuantity: Int? = null,
    val maxDistanceKm: Double? = null,
    val maxDeadlineHours: Int? = null,
    val center: GeoPoint? = null,
    val sortBy: FoodSortOrder = FoodSortOrder.NEWEST
)
