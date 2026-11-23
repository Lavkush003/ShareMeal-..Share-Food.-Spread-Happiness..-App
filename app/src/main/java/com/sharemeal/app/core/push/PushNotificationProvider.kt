package com.sharemeal.app.core.push

import com.sharemeal.app.core.common.AppResult

interface PushNotificationProvider {
    val providerName: String
    val isConfigured: Boolean

    suspend fun register(): AppResult<String?>
    suspend fun unregister(): AppResult<Unit>
}
