package com.sharemeal.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sharemeal.app.domain.model.SyncRunOutcome
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = try {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        when (entryPoint.syncManager().syncPending()) {
            SyncRunOutcome.SUCCESS -> Result.success()
            SyncRunOutcome.OFFLINE, SyncRunOutcome.RETRY -> Result.retry()
            SyncRunOutcome.FAILED -> Result.failure()
        }
    } catch (error: Exception) {
        Result.retry()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun syncManager(): SyncManager
    }
}
