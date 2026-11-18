package com.sharemeal.app.data.notification

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.local.MockDonationStore
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class MockNotificationRepository @Inject constructor(
    private val store: MockDonationStore
) : NotificationRepository {

    override val notifications: StateFlow<List<AppNotification>> = store.notifications

    override suspend fun getNotificationsFor(userId: String): AppResult<List<AppNotification>> = AppResult.Success(
        notifications.value.filter { it.recipientId == userId }.sortedByDescending { it.createdAt }
    )

    override suspend fun markRead(notificationId: String): AppResult<Unit> = try {
        store.markNotificationRead(notificationId)
        AppResult.Success(Unit)
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown("Could not update notification.", cause = error))
    }

    override suspend fun markAllRead(userId: String): AppResult<Unit> = try {
        store.markAllNotificationsRead(userId)
        AppResult.Success(Unit)
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown("Could not update notifications.", cause = error))
    }
}
