package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DonationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    PICKUP_SCHEDULED,
    COLLECTED,
    DISTRIBUTED,
    COMPLETED,
    EXPIRED;

    val isTerminal: Boolean
        get() = this == REJECTED || this == CANCELLED || this == COMPLETED || this == EXPIRED

    val allocatesQuantity: Boolean
        get() = this == APPROVED || this == PICKUP_SCHEDULED || this == COLLECTED ||
            this == DISTRIBUTED || this == COMPLETED

    val isActive: Boolean
        get() = this == PENDING || this == APPROVED || this == PICKUP_SCHEDULED

    companion object {
        fun canTransition(from: DonationRequestStatus, to: DonationRequestStatus): Boolean = when (from) {
            PENDING -> to == APPROVED || to == REJECTED || to == CANCELLED || to == EXPIRED
            APPROVED -> to == PICKUP_SCHEDULED || to == COLLECTED || to == CANCELLED || to == EXPIRED
            PICKUP_SCHEDULED -> to == COLLECTED || to == CANCELLED || to == EXPIRED
            COLLECTED -> to == COMPLETED || to == DISTRIBUTED
            DISTRIBUTED -> to == COMPLETED
            else -> false
        }
    }
}
