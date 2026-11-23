package com.sharemeal.app.core.location

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.sharemeal.app.core.common.DispatcherProvider
import com.sharemeal.app.domain.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionChecker: LocationPermissionChecker,
    private val dispatchers: DispatcherProvider
) : LocationProvider {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    override suspend fun hasPermission(): Boolean = withContext(dispatchers.io) {
        permissionChecker.hasLocationPermission()
    }

    override suspend fun isLocationAvailable(): Boolean = withContext(dispatchers.io) {
        try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            manager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                manager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun currentLocation(): GeoPoint? = withContext(dispatchers.io) {
        if (!permissionChecker.hasLocationPermission()) return@withContext null
        try {
            val location = runCatching { Tasks.await(fusedLocationClient.lastLocation) }.getOrNull()
                ?: Tasks.await(
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    )
                )
            location?.toGeoPoint()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun geocode(address: String): GeoPoint? = withContext(dispatchers.io) {
        if (address.isBlank() || !Geocoder.isPresent()) return@withContext null
        try {
            @Suppress("DEPRECATION")
            val results = Geocoder(context, Locale.getDefault()).getFromLocationName(address, 5)
            results?.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun reverseGeocode(point: GeoPoint): String? = withContext(dispatchers.io) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            @Suppress("DEPRECATION")
            val results = Geocoder(context, Locale.getDefault())
                .getFromLocation(point.latitude, point.longitude, 1)
            val address = results?.firstOrNull() ?: return@withContext null
            address.getAddressLine(0)
                ?: listOfNotNull(address.locality, address.adminArea, address.countryName)
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun Location.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
}
