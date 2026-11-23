package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppNotificationType {
    NEW_REQUEST,
    REQUEST_APPROVED,
    REQUEST_REJECTED,
    REQUEST_CANCELLED,
    PICKUP_SCHEDULED,
    PICKUP_NGO_READY,
    PICKUP_DONOR_READY,
    PICKUP_COLLECTED,
    DISTRIBUTION_CONFIRMED,
    PICKUP_EXCEPTION,
    DONATION_COMPLETED,
    PICKUP_REMINDER,
    NGO_VERIFICATION_UPDATE,
    SAFETY_ALERT
}

@Serializable
data class AppNotification(
    val id: String,
    val recipientId: String,
    val type: AppNotificationType,
    val title: String,
    val body: String,
    val requestId: String? = null,
    val donationId: String? = null,
    val createdAt: Long,
    val isRead: Boolean = false
)
