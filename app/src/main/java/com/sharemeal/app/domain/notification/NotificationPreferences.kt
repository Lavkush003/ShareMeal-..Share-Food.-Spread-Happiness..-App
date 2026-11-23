package com.sharemeal.app.domain.notification

import com.sharemeal.app.domain.model.AppNotificationType
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferences(
    val localNotificationsEnabled: Boolean = true,
    val newRequest: Boolean = true,
    val requestApproved: Boolean = true,
    val requestRejected: Boolean = true,
    val pickupReminder: Boolean = true,
    val pickupScheduled: Boolean = true,
    val collectionConfirmation: Boolean = true,
    val distributionConfirmation: Boolean = true,
    val ngoVerificationUpdate: Boolean = true,
    val safetyAlerts: Boolean = true
) {
    fun isEnabled(type: AppNotificationType): Boolean = when (type) {
        AppNotificationType.NEW_REQUEST -> newRequest
        AppNotificationType.REQUEST_APPROVED -> requestApproved
        AppNotificationType.REQUEST_REJECTED,
        AppNotificationType.REQUEST_CANCELLED -> requestRejected
        AppNotificationType.PICKUP_REMINDER -> pickupReminder
        AppNotificationType.PICKUP_SCHEDULED,
        AppNotificationType.PICKUP_NGO_READY,
        AppNotificationType.PICKUP_DONOR_READY,
        AppNotificationType.PICKUP_EXCEPTION -> pickupScheduled
        AppNotificationType.PICKUP_COLLECTED -> collectionConfirmation
        AppNotificationType.DISTRIBUTION_CONFIRMED,
        AppNotificationType.DONATION_COMPLETED -> distributionConfirmation
        AppNotificationType.NGO_VERIFICATION_UPDATE -> ngoVerificationUpdate
        AppNotificationType.SAFETY_ALERT -> safetyAlerts
    }

    fun withCategory(type: AppNotificationType, enabled: Boolean): NotificationPreferences = when (type) {
        AppNotificationType.NEW_REQUEST -> copy(newRequest = enabled)
        AppNotificationType.REQUEST_APPROVED -> copy(requestApproved = enabled)
        AppNotificationType.REQUEST_REJECTED,
        AppNotificationType.REQUEST_CANCELLED -> copy(requestRejected = enabled)
        AppNotificationType.PICKUP_REMINDER -> copy(pickupReminder = enabled)
        AppNotificationType.PICKUP_SCHEDULED,
        AppNotificationType.PICKUP_NGO_READY,
        AppNotificationType.PICKUP_DONOR_READY,
        AppNotificationType.PICKUP_EXCEPTION -> copy(pickupScheduled = enabled)
        AppNotificationType.PICKUP_COLLECTED -> copy(collectionConfirmation = enabled)
        AppNotificationType.DISTRIBUTION_CONFIRMED,
        AppNotificationType.DONATION_COMPLETED -> copy(distributionConfirmation = enabled)
        AppNotificationType.NGO_VERIFICATION_UPDATE -> copy(ngoVerificationUpdate = enabled)
        AppNotificationType.SAFETY_ALERT -> copy(safetyAlerts = enabled)
    }
}
