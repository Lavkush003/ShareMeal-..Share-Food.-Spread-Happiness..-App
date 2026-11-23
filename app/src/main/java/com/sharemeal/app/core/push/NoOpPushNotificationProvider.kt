package com.sharemeal.app.core.push

import com.sharemeal.app.core.common.AppResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpPushNotificationProvider @Inject constructor() : PushNotificationProvider {
    override val providerName: String = "NONE"
    override val isConfigured: Boolean = false

    override suspend fun register(): AppResult<String?> = AppResult.Success(null)

    override suspend fun unregister(): AppResult<Unit> = AppResult.Success(Unit)
}
