package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DonationStatus {
    DRAFT,
    PUBLISHED,
    PARTIALLY_ALLOCATED,
    FULLY_ALLOCATED,
    PICKUP_SCHEDULED,
    COLLECTED,
    DISTRIBUTED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    SAFETY_HOLD
}
