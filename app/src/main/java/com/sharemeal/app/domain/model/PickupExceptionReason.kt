package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PickupExceptionReason {
    MISSED_PICKUP,
    CANCELLED_PICKUP,
    DONOR_UNAVAILABLE,
    NGO_UNABLE_TO_COLLECT,
    SAFETY_REJECTED
}
