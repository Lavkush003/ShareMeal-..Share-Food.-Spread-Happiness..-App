package com.sharemeal.app.domain.location

import com.sharemeal.app.domain.model.GeoPoint
import kotlinx.serialization.Serializable

@Serializable
enum class SearchLocationSource {
    CURRENT_DEVICE,
    MANUAL_ADDRESS,
    CITY,
    PIN_CODE,
    MAP
}

@Serializable
data class SearchLocation(
    val point: GeoPoint,
    val label: String,
    val source: SearchLocationSource,
    val radiusKm: Double = 10.0
)
