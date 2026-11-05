package com.sharemeal.app.core.security

import kotlinx.coroutines.flow.StateFlow

interface SessionTokenStore {
    val accessToken: StateFlow<String?>
    fun update(accessToken: String?, refreshToken: String?)
    fun currentAccessToken(): String?
    fun clear()
}
