package com.sharemeal.app.data.user

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.session.SessionManager
import com.sharemeal.app.core.validation.AuthValidator
import com.sharemeal.app.data.auth.MockAccountLock
import com.sharemeal.app.data.auth.MockAccountRecord
import com.sharemeal.app.data.auth.MockAccountSeeder
import com.sharemeal.app.data.auth.MockAccountStore
import com.sharemeal.app.domain.model.DonorProfile
import com.sharemeal.app.domain.model.NgoProfile
import com.sharemeal.app.domain.model.ProfileUpdate
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock

@Singleton
class MockUserRepository @Inject constructor(
    private val seeder: MockAccountSeeder,
    private val accountStore: MockAccountStore,
    private val accountLock: MockAccountLock,
    private val sessionManager: SessionManager
) : UserRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun getCurrentUser(): AppResult<User> = safely {
        val session = sessionManager.session.value
            ?: sessionManager.restoreSession()
            ?: return@safely AppResult.Failure(AppError.Unauthorized())

        seeder.ensureSeeded()
        val account = accountLock.mutex.withLock {
            accountStore.load().firstOrNull { it.user.id == session.userId }
        } ?: return@safely AppResult.Failure(AppError.NotFound("Profile not found."))

        _currentUser.value = account.user
        AppResult.Success(account.user)
    }

    override suspend fun updateProfile(update: ProfileUpdate): AppResult<User> = safely {
        val session = sessionManager.session.value
            ?: return@safely AppResult.Failure(AppError.Unauthorized())

        seeder.ensureSeeded()
        accountLock.mutex.withLock {
            val accounts = accountStore.load()
            val index = accounts.indexOfFirst { it.user.id == session.userId }
            if (index < 0) {
                return@withLock AppResult.Failure(AppError.NotFound("Profile not found."))
            }

            val record = accounts[index]
            val updatedUser = record.user.updatedWith(update)
            val updatedAccounts = accounts.toMutableList().apply {
                this[index] = record.copy(user = updatedUser)
            }
            accountStore.save(updatedAccounts)
            _currentUser.value = updatedUser
            AppResult.Success(updatedUser)
        }
    }

    private fun User.updatedWith(update: ProfileUpdate): User {
        val normalizedMobile = AuthValidator.normalizeMobile(update.mobile)
        return when (role) {
            UserRole.DONOR -> {
                val currentProfile = donorProfile ?: return this
                copy(
                    mobile = normalizedMobile,
                    displayName = update.displayName.trim(),
                    donorProfile = currentProfile.copy(
                        fullName = update.displayName.trim(),
                        organizationName = update.organizationName
                            ?.trim()
                            ?.takeIf(String::isNotEmpty),
                        category = update.donorCategory ?: currentProfile.category,
                        address = update.address
                    )
                )
            }

            UserRole.NGO -> {
                val currentProfile = ngoProfile ?: return this
                copy(
                    mobile = normalizedMobile,
                    displayName = update.displayName.trim(),
                    ngoProfile = currentProfile.copy(
                        ngoName = update.displayName.trim(),
                        contactPersonName = update.contactPersonName
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                            ?: currentProfile.contactPersonName,
                        address = update.address,
                        operatingArea = update.operatingArea
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                            ?: currentProfile.operatingArea,
                        estimatedDistributionCapacity = update.estimatedDistributionCapacity
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                            ?: currentProfile.estimatedDistributionCapacity
                    )
                )
            }
        }
    }

    private suspend fun <T> safely(
        block: suspend () -> AppResult<T>
    ): AppResult<T> = try {
        block()
    } catch (error: AppError) {
        AppResult.Failure(error)
    } catch (error: Exception) {
        AppResult.Failure(
            AppError.Unknown("Mock profile operation failed.", cause = error)
        )
    }
}
