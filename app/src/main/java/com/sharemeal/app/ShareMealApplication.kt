package com.sharemeal.app

import android.app.Application
import com.sharemeal.app.core.notification.LocalNotificationDispatcher
import com.sharemeal.app.data.offline.OfflineCacheCoordinator
import com.sharemeal.app.core.config.AppConfig
import com.sharemeal.app.data.sync.SyncManager
import com.mapbox.common.MapboxOptions
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class ShareMealApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            this,
            OfflineEntryPoint::class.java
        )
        val appConfig = entryPoint.appConfig()
        if (appConfig.useMapbox) {
            MapboxOptions.accessToken = appConfig.mapboxPublicToken
        }
        entryPoint.offlineCacheCoordinator().start()
        entryPoint.syncManager().schedulePeriodicSync()
        entryPoint.localNotificationDispatcher().start()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface OfflineEntryPoint {
        fun offlineCacheCoordinator(): OfflineCacheCoordinator
        fun syncManager(): SyncManager
        fun appConfig(): AppConfig
        fun localNotificationDispatcher(): LocalNotificationDispatcher
    }
}
