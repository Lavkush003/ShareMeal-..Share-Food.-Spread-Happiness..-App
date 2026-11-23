package com.sharemeal.app.core.navigation

import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.AppNotificationType
import java.net.URLDecoder

object NotificationTargets {
    fun routeFor(notification: AppNotification): String = when (notification.type) {
        AppNotificationType.NEW_REQUEST,
        AppNotificationType.REQUEST_APPROVED,
        AppNotificationType.REQUEST_REJECTED,
        AppNotificationType.REQUEST_CANCELLED,
        AppNotificationType.DISTRIBUTION_CONFIRMED,
        AppNotificationType.DONATION_COMPLETED -> notification.requestId
            ?.let(Routes::requestDetailRoute)
            ?: Routes.NOTIFICATIONS
        AppNotificationType.PICKUP_REMINDER,
        AppNotificationType.PICKUP_SCHEDULED,
        AppNotificationType.PICKUP_NGO_READY,
        AppNotificationType.PICKUP_DONOR_READY,
        AppNotificationType.PICKUP_COLLECTED,
        AppNotificationType.PICKUP_EXCEPTION -> notification.requestId
            ?.let(Routes::pickupDetailRoute)
            ?: Routes.NOTIFICATIONS
        AppNotificationType.NGO_VERIFICATION_UPDATE -> Routes.PROFILE
        AppNotificationType.SAFETY_ALERT -> Routes.NOTIFICATIONS
    }
}

object NotificationDeepLink {
    const val SCHEME = "sharemeal"
    const val HOST = "sharemeal.app"
    const val PATH = "/notification"
    const val BASE_URI = "$SCHEME://$HOST$PATH"

    fun uriStringFor(notification: AppNotification): String = buildString {
        append(BASE_URI)
        append("?type=").append(notification.type.name)
        notification.requestId?.let { append("&requestId=").append(it) }
        notification.donationId?.let { append("&donationId=").append(it) }
    }

    fun routeFromUriString(value: String?): String? {
        if (value == null || !value.startsWith(BASE_URI)) return null
        val query = value.substringAfter('?', "")
        val parameters = query.split('&').mapNotNull { part ->
            val name = part.substringBefore('=', "")
            val rawValue = part.substringAfter('=', "")
            if (name.isBlank()) null else name to decode(rawValue)
        }.toMap()
        val type = runCatching {
            AppNotificationType.valueOf(parameters["type"] ?: AppNotificationType.SAFETY_ALERT.name)
        }.getOrDefault(AppNotificationType.SAFETY_ALERT)
        val notification = AppNotification(
            id = "deep-link",
            recipientId = "",
            type = type,
            title = "",
            body = "",
            requestId = parameters["requestId"],
            donationId = parameters["donationId"],
            createdAt = 0L
        )
        return NotificationTargets.routeFor(notification)
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)
}
