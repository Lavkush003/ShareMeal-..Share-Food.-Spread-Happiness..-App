package com.sharemeal.app.core.notification

import com.sharemeal.app.core.common.DispatcherProvider
import com.sharemeal.app.core.config.AppConfig
import com.sharemeal.app.domain.repository.NotificationPreferencesRepository
import com.sharemeal.app.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Singleton
class LocalNotificationDispatcher @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: NotificationPreferencesRepository,
    private val localNotifier: LocalNotifier,
    private val appConfig: AppConfig,
    private val dispatchers: DispatcherProvider
) {
    private var job: Job? = null
    private val seenIds = mutableSetOf<String>()
    private var initialized = false

    fun start() {
        if (!appConfig.isMockMode || job != null) return
        job = CoroutineScope(SupervisorJob() + dispatchers.io).launch {
            notificationRepository.notifications.collect { notifications ->
                if (!initialized) {
                    seenIds.addAll(notifications.map { it.id })
                    initialized = true
                    return@collect
                }
                val fresh = notifications.filterNot { it.id in seenIds }
                fresh.forEach { notification ->
                    if (seenIds.add(notification.id)) {
                        localNotifier.show(notification)
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        seenIds.clear()
        initialized = false
    }
}
