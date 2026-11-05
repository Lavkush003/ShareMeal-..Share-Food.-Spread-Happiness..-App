package com.sharemeal.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sharemeal.app.domain.model.SavedSession
import com.sharemeal.app.domain.model.UserRole
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreAppPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppPreferencesRepository {

    override val onboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences[KeyOnboardingCompleted] ?: false }

    override suspend fun isOnboardingCompleted(): Boolean = safePreferences()[KeyOnboardingCompleted] ?: false

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[KeyOnboardingCompleted] = true
        }
    }

    override suspend fun savedSession(): SavedSession? {
        val preferences = safePreferences()
        val userId = preferences[KeySessionUserId] ?: return null
        val role = preferences[KeySessionRole]
            ?.let { storedRole -> UserRole.entries.firstOrNull { it.name == storedRole } }
            ?: return null
        return SavedSession(userId = userId, role = role)
    }

    override suspend fun saveSession(session: SavedSession) {
        dataStore.edit { preferences ->
            preferences[KeySessionUserId] = session.userId
            preferences[KeySessionRole] = session.role.name
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(KeySessionUserId)
            preferences.remove(KeySessionRole)
        }
    }

    private suspend fun safePreferences(): Preferences {
        return try {
            dataStore.data.first()
        } catch (_: IOException) {
            emptyPreferences()
        }
    }

    private companion object {
        val KeyOnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val KeySessionUserId = stringPreferencesKey("session_user_id")
        val KeySessionRole = stringPreferencesKey("session_role")
    }
}
