package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppNotificationType {
    NEW_REQUEST,
    REQUEST_APPROVED,
    REQUEST_REJECTED,
    REQUEST_CANCELLED,
    PICKUP_SCHEDULED
}

@Serializable
data class AppNotification(
    val id: String,
    val recipientId: String,
    val type: AppNotificationType,
    val title: String,
    val body: String,
    val requestId: String,
    val donationId: String? = null,
    val createdAt: Long,
    val isRead: Boolean = false
)
