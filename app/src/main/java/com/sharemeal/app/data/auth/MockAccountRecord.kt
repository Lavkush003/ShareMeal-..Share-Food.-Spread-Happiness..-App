package com.sharemeal.app.data.auth

import com.sharemeal.app.domain.model.User
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MockAccountRecord(
    val user: User,
    val passwordSalt: String,
    val passwordHash: String
)

interface MockAccountStore {
    suspend fun load(): List<MockAccountRecord>

    suspend fun save(accounts: List<MockAccountRecord>)
}

@Singleton
class MockAccountLock @Inject constructor() {
    val mutex = Mutex()
}
