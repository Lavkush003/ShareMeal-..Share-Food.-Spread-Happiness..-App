package com.sharemeal.app.domain.location

import com.sharemeal.app.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceCalculator {
    private const val EARTH_RADIUS_KM = 6371.0088

    fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
        val latDelta = toRadians(to.latitude - from.latitude)
        val lngDelta = toRadians(to.longitude - from.longitude)
        val a = sin(latDelta / 2) * sin(latDelta / 2) +
            cos(toRadians(from.latitude)) * cos(toRadians(to.latitude)) *
            sin(lngDelta / 2) * sin(lngDelta / 2)
        return EARTH_RADIUS_KM * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun approximate(point: GeoPoint, cellSizeKm: Double = 2.0): GeoPoint {
        val safeCell = cellSizeKm.coerceIn(0.1, 20.0)
        val latStep = safeCell / 111.0
        val longitudeScale = max(0.01, cos(toRadians(point.latitude)))
        val lngStep = safeCell / (111.0 * longitudeScale)
        val latitude = (round(point.latitude / latStep) * latStep).coerceIn(-90.0, 90.0)
        val longitude = ((round(point.longitude / lngStep) * lngStep) % 360.0).let {
            if (it > 180.0) it - 360.0 else if (it < -180.0) it + 360.0 else it
        }
        return GeoPoint(min(latitude, 90.0), max(longitude, -180.0))
    }

    private fun toRadians(degrees: Double): Double = degrees * Math.PI / 180.0
}
