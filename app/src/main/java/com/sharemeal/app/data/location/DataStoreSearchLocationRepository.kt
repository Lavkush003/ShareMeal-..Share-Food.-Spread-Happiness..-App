package com.sharemeal.app.data.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.common.DispatcherProvider
import com.sharemeal.app.core.location.LocationProvider
import com.sharemeal.app.domain.location.SearchLocation
import com.sharemeal.app.domain.location.SearchLocationSource
import com.sharemeal.app.domain.model.GeoPoint
import com.sharemeal.app.domain.repository.SearchLocationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@Singleton
class DataStoreSearchLocationRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val locationProvider: LocationProvider,
    private val dispatchers: DispatcherProvider
) : SearchLocationRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    override val searchRadiusKm: StateFlow<Double> = dataStore.data
        .map { preferences -> preferences[RADIUS_KM] ?: DEFAULT_RADIUS_KM }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_RADIUS_KM)

    override val selectedLocation: StateFlow<SearchLocation?> = dataStore.data
        .map { preferences -> readLocation(preferences) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun useCurrentLocation(): AppResult<SearchLocation> = withContext(dispatchers.io) {
        if (!locationProvider.hasPermission()) {
            return@withContext AppResult.Failure(AppError.LocationPermissionDenied())
        }
        if (!locationProvider.isLocationAvailable()) {
            return@withContext AppResult.Failure(AppError.LocationUnavailable())
        }
        val point = locationProvider.currentLocation()
            ?: return@withContext AppResult.Failure(AppError.LocationUnavailable())
        val label = locationProvider.reverseGeocode(point) ?: "Current location"
        save(SearchLocation(point, label, SearchLocationSource.CURRENT_DEVICE, searchRadiusKm.value))
    }

    override suspend fun searchLocation(
        query: String,
        source: SearchLocationSource
    ): AppResult<SearchLocation> = withContext(dispatchers.io) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext AppResult.Failure(
                AppError.Validation(message = "Enter an address, city, or PIN code.")
            )
        }
        val point = locationProvider.geocode(trimmed)
            ?: return@withContext AppResult.Failure(
                AppError.NotFound("No matching address, city, or PIN code found.")
            )
        save(SearchLocation(point, trimmed, source, searchRadiusKm.value))
    }

    override suspend fun selectMapPoint(
        point: GeoPoint,
        label: String?
    ): AppResult<SearchLocation> = withContext(dispatchers.io) {
        val resolvedLabel = label?.takeIf { it.isNotBlank() }
            ?: locationProvider.reverseGeocode(point)
            ?: "Selected map location"
        save(SearchLocation(point, resolvedLabel, SearchLocationSource.MAP, searchRadiusKm.value))
    }

    override suspend fun selectLocation(location: SearchLocation): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            dataStore.edit { preferences ->
                preferences[LATITUDE] = location.point.latitude
                preferences[LONGITUDE] = location.point.longitude
                preferences[LABEL] = location.label
                preferences[SOURCE] = location.source.name
                preferences[RADIUS_KM] = location.radiusKm.coerceIn(MIN_RADIUS_KM, MAX_RADIUS_KM)
            }
            AppResult.Success(Unit)
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unknown("Could not save the selected location.", error))
        }
    }

    override suspend fun updateRadius(radiusKm: Double): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            val radius = radiusKm.coerceIn(MIN_RADIUS_KM, MAX_RADIUS_KM)
            dataStore.edit { preferences -> preferences[RADIUS_KM] = radius }
            AppResult.Success(Unit)
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unknown("Could not save the search radius.", error))
        }
    }

    private suspend fun save(location: SearchLocation): AppResult<SearchLocation> =
        when (val result = selectLocation(location)) {
            is AppResult.Success -> AppResult.Success(location)
            is AppResult.Failure -> AppResult.Failure(result.error)
        }

    private fun readLocation(preferences: Preferences): SearchLocation? {
        val latitude = preferences[LATITUDE] ?: return null
        val longitude = preferences[LONGITUDE] ?: return null
        val point = runCatching { GeoPoint(latitude, longitude) }.getOrNull() ?: return null
        val source = runCatching {
            SearchLocationSource.valueOf(preferences[SOURCE] ?: SearchLocationSource.MANUAL_ADDRESS.name)
        }.getOrDefault(SearchLocationSource.MANUAL_ADDRESS)
        return SearchLocation(
            point = point,
            label = preferences[LABEL] ?: "Selected location",
            source = source,
            radiusKm = preferences[RADIUS_KM] ?: DEFAULT_RADIUS_KM
        )
    }

    private companion object {
        const val DEFAULT_RADIUS_KM = 10.0
        const val MIN_RADIUS_KM = 0.5
        const val MAX_RADIUS_KM = 100.0
        val LATITUDE = doublePreferencesKey("search_latitude")
        val LONGITUDE = doublePreferencesKey("search_longitude")
        val RADIUS_KM = doublePreferencesKey("search_radius_km")
        val LABEL = stringPreferencesKey("search_location_label")
        val SOURCE = stringPreferencesKey("search_location_source")
    }
}
