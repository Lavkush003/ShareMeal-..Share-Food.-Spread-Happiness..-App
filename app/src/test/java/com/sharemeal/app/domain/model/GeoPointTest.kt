package com.sharemeal.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPointTest {

    @Test
    fun acceptsValidCoordinates() {
        val point = GeoPoint(latitude = 12.9716, longitude = 77.5946)

        assertEquals(12.9716, point.latitude, 0.0)
        assertEquals(77.5946, point.longitude, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidLatitude() {
        GeoPoint(latitude = 91.0, longitude = 77.5946)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidLongitude() {
        GeoPoint(latitude = 12.9716, longitude = 181.0)
    }
}
