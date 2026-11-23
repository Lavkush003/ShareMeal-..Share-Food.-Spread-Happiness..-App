package com.sharemeal.app.domain.repository

import com.sharemeal.app.domain.model.AppNotificationType
import com.sharemeal.app.domain.notification.NotificationPreferences
import kotlinx.coroutines.flow.StateFlow

interface NotificationPreferencesRepository {
    val preferences: StateFlow<NotificationPreferences>

    suspend fun setLocalNotificationsEnabled(enabled: Boolean)
    suspend fun setCategoryEnabled(type: AppNotificationType, enabled: Boolean)
}
