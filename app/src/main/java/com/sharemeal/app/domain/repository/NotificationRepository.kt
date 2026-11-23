package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.AppNotificationType
import kotlinx.coroutines.flow.StateFlow

/** Local/in-app development notifications. No remote push backend is configured. */
interface NotificationRepository {
    val notifications: StateFlow<List<AppNotification>>
    suspend fun getNotificationsFor(userId: String): AppResult<List<AppNotification>>
    suspend fun markRead(notificationId: String): AppResult<Unit>
    suspend fun markAllRead(userId: String): AppResult<Unit>
    suspend fun postLocalNotification(
        recipientId: String,
        type: AppNotificationType,
        title: String,
        body: String,
        requestId: String? = null,
        donationId: String? = null
    ): AppResult<AppNotification>
}
