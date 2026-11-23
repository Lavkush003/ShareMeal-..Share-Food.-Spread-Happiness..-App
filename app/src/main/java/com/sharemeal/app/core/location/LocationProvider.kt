package com.sharemeal.app.core.location

import com.sharemeal.app.domain.model.GeoPoint

interface LocationProvider {
    suspend fun hasPermission(): Boolean
    suspend fun isLocationAvailable(): Boolean
    suspend fun currentLocation(): GeoPoint?
    suspend fun geocode(address: String): GeoPoint?
    suspend fun reverseGeocode(point: GeoPoint): String?
}
