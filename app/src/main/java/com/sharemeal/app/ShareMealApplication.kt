package com.sharemeal.app

import android.app.Application
import com.sharemeal.app.data.offline.OfflineCacheCoordinator
import com.sharemeal.app.data.sync.SyncManager
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
        entryPoint.offlineCacheCoordinator().start()
        entryPoint.syncManager().schedulePeriodicSync()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface OfflineEntryPoint {
        fun offlineCacheCoordinator(): OfflineCacheCoordinator
        fun syncManager(): SyncManager
    }
}
