package com.sharemeal.app.core.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val WELCOME = "welcome"
    const val LOGIN = "auth/login"
    const val FORGOT_PASSWORD = "auth/forgot-password"
    const val REGISTER = "auth/register"
    const val REGISTER_DONOR = "auth/register/donor"
    const val REGISTER_NGO = "auth/register/ngo"

    const val DONOR_DASHBOARD = "donor/dashboard"
    const val DONOR_ADD_DONATION = "donor/donations/add"
    const val DONOR_DONATIONS = "donor/donations"
    const val DONOR_REQUESTS = "donor/requests"
    const val DONOR_PICKUPS = "donor/pickups"
    const val DONOR_COMPLETED = "donor/completed"
    const val DONOR_HISTORY = "donor/history"

    const val NGO_DASHBOARD = "ngo/dashboard"
    const val NGO_DISCOVERY = "ngo/discovery"
    const val NGO_REQUESTS = "ngo/requests"
    const val NGO_PICKUPS = "ngo/pickups"
    const val NGO_COMPLETED = "ngo/completed"
    const val NGO_HISTORY = "ngo/history"

    const val ARG_DONATION_ID = "donationId"
    const val DONATION_DETAIL = "ngo/discovery/{$ARG_DONATION_ID}"

    const val ARG_REQUEST_ID = "requestId"
    const val REQUEST_DETAIL = "request/{$ARG_REQUEST_ID}"
    const val REQUEST_HISTORY = "request/history"

    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    const val ARG_PICKUP_MODE = "pickupMode"
    const val ARG_PICKUP_ACTION = "pickupAction"
    const val PICKUP_LIST = "pickup/list/{$ARG_PICKUP_MODE}"
    const val PICKUP_DETAIL = "pickup/detail/{$ARG_REQUEST_ID}"
    const val PICKUP_ACTION = "pickup/action/{$ARG_PICKUP_ACTION}/{$ARG_REQUEST_ID}"
    const val PICKUP_HISTORY = "pickup/history"

    fun donationDetailRoute(donationId: String): String =
        "ngo/discovery/$donationId"

    fun requestDetailRoute(requestId: String): String =
        "request/$requestId"

    fun pickupListRoute(mode: String): String = "pickup/list/$mode"

    fun pickupDetailRoute(requestId: String): String = "pickup/detail/$requestId"

    fun pickupActionRoute(action: String, requestId: String): String =
        "pickup/action/$action/$requestId"
}
