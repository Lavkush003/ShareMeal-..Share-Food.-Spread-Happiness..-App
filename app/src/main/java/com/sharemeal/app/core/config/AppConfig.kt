package com.sharemeal.app.core.config

import com.sharemeal.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig @Inject constructor() {
    val apiBaseUrl: String = BuildConfig.API_BASE_URL
    val isMockMode: Boolean = BuildConfig.USE_MOCK_DATA
    val isDebug: Boolean = BuildConfig.DEBUG
}
