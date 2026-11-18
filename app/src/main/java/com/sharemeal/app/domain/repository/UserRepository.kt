package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.ProfileUpdate
import com.sharemeal.app.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val currentUser: StateFlow<User?>

    suspend fun getCurrentUser(): AppResult<User>

    suspend fun getUser(userId: String): AppResult<User>

    suspend fun updateProfile(update: ProfileUpdate): AppResult<User>

    suspend fun setNgoVerificationStatus(userId: String, status: NgoVerificationStatus): AppResult<User>
}
