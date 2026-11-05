package com.sharemeal.app.core.session

import com.sharemeal.app.core.preferences.AppPreferencesRepository
import com.sharemeal.app.core.security.SessionTokenStore
import com.sharemeal.app.domain.model.AuthSession
import com.sharemeal.app.domain.model.SavedSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SessionManager @Inject constructor(
    private val preferences: AppPreferencesRepository,
    private val tokenStore: SessionTokenStore
) {
    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val _isSessionRestored = MutableStateFlow(false)
    val isSessionRestored: StateFlow<Boolean> = _isSessionRestored.asStateFlow()

    private val mutex = Mutex()

    suspend fun restoreSession(): AuthSession? = mutex.withLock {
        if (!_isSessionRestored.value) {
            val saved = preferences.savedSession()
            if (saved != null) {
                val session = AuthSession(userId = saved.userId, role = saved.role)
                _session.value = session
                tokenStore.update(accessTokenFor(session.userId), null)
            }
            _isSessionRestored.value = true
        }
        _session.value
    }

    suspend fun persistSession(
        session: AuthSession,
        rememberSession: Boolean
    ) = mutex.withLock {
        _session.value = session
        tokenStore.update(accessTokenFor(session.userId), null)
        if (rememberSession) {
            preferences.saveSession(SavedSession(userId = session.userId, role = session.role))
        } else {
            preferences.clearSession()
        }
        _isSessionRestored.value = true
    }

    suspend fun clearSession() = mutex.withLock {
        _session.value = null
        tokenStore.clear()
        preferences.clearSession()
        _isSessionRestored.value = true
    }

    private fun accessTokenFor(userId: String): String = "mock-access-token-$userId"
}
