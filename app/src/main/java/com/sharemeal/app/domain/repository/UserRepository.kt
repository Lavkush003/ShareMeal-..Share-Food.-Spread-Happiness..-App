package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.ProfileUpdate
import com.sharemeal.app.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val currentUser: StateFlow<User?>

    suspend fun getCurrentUser(): AppResult<User>

    suspend fun updateProfile(update: ProfileUpdate): AppResult<User>
}
