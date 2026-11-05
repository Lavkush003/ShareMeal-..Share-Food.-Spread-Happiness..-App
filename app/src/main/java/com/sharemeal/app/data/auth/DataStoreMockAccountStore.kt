package com.sharemeal.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class DataStoreMockAccountStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : MockAccountStore {

    override suspend fun load(): List<MockAccountRecord> {
        val raw = try {
            dataStore.data.first()[KeyMockAccounts]
        } catch (_: IOException) {
            null
        } ?: return emptyList()

        return try {
            json.decodeFromString<List<MockAccountRecord>>(raw)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    override suspend fun save(accounts: List<MockAccountRecord>) {
        dataStore.edit { preferences ->
            preferences[KeyMockAccounts] = json.encodeToString(accounts)
        }
    }

    private companion object {
        val KeyMockAccounts = stringPreferencesKey("mock_accounts_v1")
    }
}
