package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.location.SearchLocation
import com.sharemeal.app.domain.location.SearchLocationSource
import com.sharemeal.app.domain.model.GeoPoint
import kotlinx.coroutines.flow.StateFlow

interface SearchLocationRepository {
    val selectedLocation: StateFlow<SearchLocation?>
    val searchRadiusKm: StateFlow<Double>

    suspend fun useCurrentLocation(): AppResult<SearchLocation>
    suspend fun searchLocation(query: String, source: SearchLocationSource): AppResult<SearchLocation>
    suspend fun selectMapPoint(point: GeoPoint, label: String? = null): AppResult<SearchLocation>
    suspend fun selectLocation(location: SearchLocation): AppResult<Unit>
    suspend fun updateRadius(radiusKm: Double): AppResult<Unit>
}
