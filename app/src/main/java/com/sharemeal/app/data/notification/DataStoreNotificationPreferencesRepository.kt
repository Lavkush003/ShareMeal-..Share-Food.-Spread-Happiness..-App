package com.sharemeal.app.data.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.sharemeal.app.domain.model.AppNotificationType
import com.sharemeal.app.domain.notification.NotificationPreferences
import com.sharemeal.app.domain.repository.NotificationPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class DataStoreNotificationPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : NotificationPreferencesRepository {

    private val scope = CoroutineScope(SupervisorJob())

    override val preferences: StateFlow<NotificationPreferences> = dataStore.data
        .map { stored ->
            NotificationPreferences(
                localNotificationsEnabled = stored[LOCAL_NOTIFICATIONS] ?: true,
                newRequest = stored[NEW_REQUEST] ?: true,
                requestApproved = stored[REQUEST_APPROVED] ?: true,
                requestRejected = stored[REQUEST_REJECTED] ?: true,
                pickupReminder = stored[PICKUP_REMINDER] ?: true,
                pickupScheduled = stored[PICKUP_SCHEDULED] ?: true,
                collectionConfirmation = stored[COLLECTION_CONFIRMATION] ?: true,
                distributionConfirmation = stored[DISTRIBUTION_CONFIRMATION] ?: true,
                ngoVerificationUpdate = stored[NGO_VERIFICATION_UPDATE] ?: true,
                safetyAlerts = stored[SAFETY_ALERTS] ?: true
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, NotificationPreferences())

    override suspend fun setLocalNotificationsEnabled(enabled: Boolean) {
        runCatching { dataStore.edit { it[LOCAL_NOTIFICATIONS] = enabled } }
    }

    override suspend fun setCategoryEnabled(type: AppNotificationType, enabled: Boolean) {
        val key = keyFor(type)
        runCatching { dataStore.edit { it[key] = enabled } }
    }

    private fun keyFor(type: AppNotificationType) = when (type) {
        AppNotificationType.NEW_REQUEST -> NEW_REQUEST
        AppNotificationType.REQUEST_APPROVED -> REQUEST_APPROVED
        AppNotificationType.REQUEST_REJECTED,
        AppNotificationType.REQUEST_CANCELLED -> REQUEST_REJECTED
        AppNotificationType.PICKUP_REMINDER -> PICKUP_REMINDER
        AppNotificationType.PICKUP_SCHEDULED,
        AppNotificationType.PICKUP_NGO_READY,
        AppNotificationType.PICKUP_DONOR_READY,
        AppNotificationType.PICKUP_EXCEPTION -> PICKUP_SCHEDULED
        AppNotificationType.PICKUP_COLLECTED -> COLLECTION_CONFIRMATION
        AppNotificationType.DISTRIBUTION_CONFIRMED,
        AppNotificationType.DONATION_COMPLETED -> DISTRIBUTION_CONFIRMATION
        AppNotificationType.NGO_VERIFICATION_UPDATE -> NGO_VERIFICATION_UPDATE
        AppNotificationType.SAFETY_ALERT -> SAFETY_ALERTS
    }

    private companion object {
        val LOCAL_NOTIFICATIONS = booleanPreferencesKey("notify_local_enabled")
        val NEW_REQUEST = booleanPreferencesKey("notify_new_request")
        val REQUEST_APPROVED = booleanPreferencesKey("notify_request_approved")
        val REQUEST_REJECTED = booleanPreferencesKey("notify_request_rejected")
        val PICKUP_REMINDER = booleanPreferencesKey("notify_pickup_reminder")
        val PICKUP_SCHEDULED = booleanPreferencesKey("notify_pickup_scheduled")
        val COLLECTION_CONFIRMATION = booleanPreferencesKey("notify_collection_confirmation")
        val DISTRIBUTION_CONFIRMATION = booleanPreferencesKey("notify_distribution_confirmation")
        val NGO_VERIFICATION_UPDATE = booleanPreferencesKey("notify_ngo_verification")
        val SAFETY_ALERTS = booleanPreferencesKey("notify_safety_alerts")
    }
}
