package com.sharemeal.app.core.preferences

import com.sharemeal.app.domain.model.SavedSession
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val onboardingCompleted: Flow<Boolean>

    suspend fun isOnboardingCompleted(): Boolean

    suspend fun setOnboardingCompleted()

    suspend fun savedSession(): SavedSession?

    suspend fun saveSession(session: SavedSession)

    suspend fun clearSession()
}
