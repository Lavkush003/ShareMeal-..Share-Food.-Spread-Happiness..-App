package com.sharemeal.app.data.user

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.ProfileUpdate
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class UnavailableUserRepository @Inject constructor() : UserRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun getCurrentUser(): AppResult<User> = backendUnavailable()

    override suspend fun updateProfile(update: ProfileUpdate): AppResult<User> = backendUnavailable()

    private fun <T> backendUnavailable(): AppResult<T> = AppResult.Failure(
        AppError.Unknown("Profile backend is not configured for this build.")
    )
}
