package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorkflowStage {
    DONATION_PUBLISHED,
    NGO_REQUESTED,
    DONOR_APPROVED,
    PICKUP_SCHEDULED,
    NGO_READY,
    DONOR_READY,
    FOOD_COLLECTED,
    DISTRIBUTION_CONFIRMED,
    COMPLETED,
    MISSED,
    CANCELLED,
    DONOR_UNAVAILABLE,
    NGO_UNABLE_TO_COLLECT,
    SAFETY_REJECTED;

    val isException: Boolean
        get() = this == MISSED || this == CANCELLED || this == DONOR_UNAVAILABLE ||
            this == NGO_UNABLE_TO_COLLECT || this == SAFETY_REJECTED
}
