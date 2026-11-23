package com.sharemeal.app.core.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.sharemeal.app.MainActivity
import com.sharemeal.app.core.navigation.NotificationDeepLink
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.AppNotificationType
import com.sharemeal.app.domain.repository.NotificationPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLocalNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: NotificationPreferencesRepository
) : LocalNotifier {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun show(notification: AppNotification) {
        val preferences = preferencesRepository.preferences.value
        if (!preferences.localNotificationsEnabled || !preferences.isEnabled(notification.type)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = channelId(notification.type)
        createChannel(channelId, notification.type)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(NotificationDeepLink.uriStringFor(notification)), context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val published = builder
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(Notification.BigTextStyle().bigText(notification.body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(notification.id.hashCode(), published)
    }

    private fun createChannel(channelId: String, type: AppNotificationType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(channelId) != null) return
        val importance = if (type == AppNotificationType.SAFETY_ALERT) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }
        val channel = NotificationChannel(channelId, channelLabel(type), importance)
        channel.description = channelLabel(type)
        manager.createNotificationChannel(channel)
    }

    private fun channelId(type: AppNotificationType): String = "sharemeal_${type.name.lowercase()}"

    private fun channelLabel(type: AppNotificationType): String = when (type) {
        AppNotificationType.NEW_REQUEST -> "New donation requests"
        AppNotificationType.REQUEST_APPROVED -> "Request approved"
        AppNotificationType.REQUEST_REJECTED,
        AppNotificationType.REQUEST_CANCELLED -> "Request rejected"
        AppNotificationType.PICKUP_REMINDER -> "Pickup reminders"
        AppNotificationType.PICKUP_SCHEDULED,
        AppNotificationType.PICKUP_NGO_READY,
        AppNotificationType.PICKUP_DONOR_READY,
        AppNotificationType.PICKUP_EXCEPTION -> "Pickup updates"
        AppNotificationType.PICKUP_COLLECTED -> "Collection confirmations"
        AppNotificationType.DISTRIBUTION_CONFIRMED,
        AppNotificationType.DONATION_COMPLETED -> "Distribution confirmations"
        AppNotificationType.NGO_VERIFICATION_UPDATE -> "NGO verification updates"
        AppNotificationType.SAFETY_ALERT -> "Important safety alerts"
    }
}
