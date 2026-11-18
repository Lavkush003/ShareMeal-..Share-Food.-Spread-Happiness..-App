package com.sharemeal.app.data.offline

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.user.MockUserRepository
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.ProfileUpdate
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class QueuedUserRepository @Inject constructor(
    private val delegate: MockUserRepository,
    private val operations: OfflineOperationQueue
) : UserRepository {

    override val currentUser: StateFlow<User?> = delegate.currentUser

    override suspend fun getCurrentUser(): AppResult<User> = delegate.getCurrentUser()

    override suspend fun getUser(userId: String): AppResult<User> = delegate.getUser(userId)

    override suspend fun updateProfile(update: ProfileUpdate): AppResult<User> =
        when (val result = delegate.updateProfile(update)) {
            is AppResult.Success -> {
                operations.enqueueProfile(result.data)
                result
            }

            is AppResult.Failure -> result
        }

    override suspend fun setNgoVerificationStatus(
        userId: String,
        status: NgoVerificationStatus
    ): AppResult<User> = when (val result = delegate.setNgoVerificationStatus(userId, status)) {
        is AppResult.Success -> {
            operations.enqueueProfile(result.data)
            result
        }

        is AppResult.Failure -> result
    }
}
