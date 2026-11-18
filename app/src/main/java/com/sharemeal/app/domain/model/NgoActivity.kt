package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NgoActivityType {
    REQUEST_SUBMITTED,
    REQUEST_APPROVED,
    PICKUP_SCHEDULED,
    DONATION_COLLECTED,
    DISTRIBUTION_COMPLETED,
    VERIFICATION_UPDATE
}

@Serializable
data class NgoActivity(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: NgoActivityType
)
