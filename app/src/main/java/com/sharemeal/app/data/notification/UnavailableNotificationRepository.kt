package com.sharemeal.app.data.notification

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.AppNotificationType
import com.sharemeal.app.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class UnavailableNotificationRepository @Inject constructor() : NotificationRepository {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    override suspend fun getNotificationsFor(userId: String): AppResult<List<AppNotification>> = backendUnavailable()
    override suspend fun markRead(notificationId: String): AppResult<Unit> = backendUnavailable()
    override suspend fun markAllRead(userId: String): AppResult<Unit> = backendUnavailable()
    override suspend fun postLocalNotification(
        recipientId: String,
        type: AppNotificationType,
        title: String,
        body: String,
        requestId: String?,
        donationId: String?
    ): AppResult<AppNotification> = backendUnavailable()

    private fun <T> backendUnavailable(): AppResult<T> = AppResult.Failure(
        AppError.Unknown("Notification backend is not configured for this build.")
    )
}
