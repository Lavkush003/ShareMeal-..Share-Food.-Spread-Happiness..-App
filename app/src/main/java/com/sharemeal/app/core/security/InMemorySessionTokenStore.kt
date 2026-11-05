package com.sharemeal.app.core.security

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class InMemorySessionTokenStore @Inject constructor() : SessionTokenStore {

    private val _accessToken = MutableStateFlow<String?>(null)
    override val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private var refreshToken: String? = null

    override fun update(accessToken: String?, refreshToken: String?) {
        _accessToken.value = accessToken
        this.refreshToken = refreshToken
    }

    override fun currentAccessToken(): String? = _accessToken.value

    override fun clear() {
        _accessToken.value = null
        refreshToken = null
    }
}
