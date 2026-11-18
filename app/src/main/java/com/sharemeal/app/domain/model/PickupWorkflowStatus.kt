package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PickupWorkflowStatus {
    SCHEDULED,
    NGO_READY,
    DONOR_READY,
    COLLECTED,
    DISTRIBUTED,
    COMPLETED,
    MISSED,
    CANCELLED,
    DONOR_UNAVAILABLE,
    NGO_UNABLE_TO_COLLECT,
    SAFETY_REJECTED;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == MISSED || this == CANCELLED ||
            this == DONOR_UNAVAILABLE || this == NGO_UNABLE_TO_COLLECT || this == SAFETY_REJECTED

    val isCollectionReady: Boolean
        get() = this == DONOR_READY

    val isActive: Boolean
        get() = this == SCHEDULED || this == NGO_READY || this == DONOR_READY ||
            this == COLLECTED || this == DISTRIBUTED
}
