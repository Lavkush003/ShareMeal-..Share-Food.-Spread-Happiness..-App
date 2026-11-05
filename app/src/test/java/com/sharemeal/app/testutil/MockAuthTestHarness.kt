package com.sharemeal.app.testutil

import com.sharemeal.app.core.preferences.AppPreferencesRepository
import com.sharemeal.app.core.security.InMemorySessionTokenStore
import com.sharemeal.app.core.session.SessionManager
import com.sharemeal.app.data.auth.MockAccountLock
import com.sharemeal.app.data.auth.MockAccountRecord
import com.sharemeal.app.data.auth.MockAccountSeeder
import com.sharemeal.app.data.auth.MockAccountStore
import com.sharemeal.app.data.auth.MockAuthenticationRepository
import com.sharemeal.app.data.auth.MockPasswordHasher
import com.sharemeal.app.data.user.MockUserRepository
import com.sharemeal.app.domain.model.SavedSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryMockAccountStore : MockAccountStore {
    private val accounts = mutableListOf<MockAccountRecord>()

    override suspend fun load(): List<MockAccountRecord> = accounts.toList()

    override suspend fun save(accounts: List<MockAccountRecord>) {
        this.accounts.clear()
        this.accounts.addAll(accounts)
    }
}

class InMemoryAppPreferencesRepository : AppPreferencesRepository {
    private val onboardingState = MutableStateFlow(false)
    private var saved: SavedSession? = null

    override val onboardingCompleted: Flow<Boolean> = onboardingState

    override suspend fun isOnboardingCompleted(): Boolean = onboardingState.value

    override suspend fun setOnboardingCompleted() {
        onboardingState.value = true
    }

    override suspend fun savedSession(): SavedSession? = saved

    override suspend fun saveSession(session: SavedSession) {
        saved = session
    }

    override suspend fun clearSession() {
        saved = null
    }
}

class MockAuthTestHarness(
    val accountStore: InMemoryMockAccountStore = InMemoryMockAccountStore(),
    val preferences: InMemoryAppPreferencesRepository = InMemoryAppPreferencesRepository()
) {
    private val tokenStore = InMemorySessionTokenStore()
    val sessionManager = SessionManager(preferences, tokenStore)
    private val accountLock = MockAccountLock()
    private val passwordHasher = MockPasswordHasher()
    private val seeder = MockAccountSeeder(accountStore, accountLock, passwordHasher)
    val authenticationRepository = MockAuthenticationRepository(
        seeder = seeder,
        accountStore = accountStore,
        accountLock = accountLock,
        passwordHasher = passwordHasher,
        sessionManager = sessionManager
    )
    val userRepository = MockUserRepository(
        seeder = seeder,
        accountStore = accountStore,
        accountLock = accountLock,
        sessionManager = sessionManager
    )
}

fun newHarnessWithSharedData(existing: MockAuthTestHarness): MockAuthTestHarness {
    return MockAuthTestHarness(
        accountStore = existing.accountStore,
        preferences = existing.preferences
    )
}
