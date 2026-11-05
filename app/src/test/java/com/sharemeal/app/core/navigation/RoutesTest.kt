package com.sharemeal.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RoutesTest {

    private val routes = listOf(
        Routes.SPLASH,
        Routes.ONBOARDING,
        Routes.WELCOME,
        Routes.LOGIN,
        Routes.FORGOT_PASSWORD,
        Routes.REGISTER,
        Routes.REGISTER_DONOR,
        Routes.REGISTER_NGO,
        Routes.DONOR_DASHBOARD,
        Routes.DONOR_ADD_DONATION,
        Routes.DONOR_DONATIONS,
        Routes.DONOR_HISTORY,
        Routes.NGO_DASHBOARD,
        Routes.NGO_DISCOVERY,
        Routes.NGO_REQUESTS,
        Routes.NGO_PICKUPS,
        Routes.NGO_HISTORY,
        Routes.NOTIFICATIONS,
        Routes.PROFILE,
        Routes.SETTINGS
    )

    @Test
    fun routesAreUnique() {
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun routesAreNotBlank() {
        assertFalse(routes.any { it.isBlank() })
    }
}
