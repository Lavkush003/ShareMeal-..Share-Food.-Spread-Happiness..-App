package com.sharemeal.app.core.location

import com.sharemeal.app.domain.model.GeoPoint

interface LocationProvider {
    suspend fun currentLocation(): GeoPoint?
    suspend fun geocode(address: String): GeoPoint?
    suspend fun reverseGeocode(point: GeoPoint): String?
}
