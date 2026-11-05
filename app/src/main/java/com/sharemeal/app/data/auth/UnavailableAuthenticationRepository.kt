package com.sharemeal.app.data.auth

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.session.SessionManager
import com.sharemeal.app.domain.model.AuthSession
import com.sharemeal.app.domain.model.DonorRegistrationRequest
import com.sharemeal.app.domain.model.NgoRegistrationRequest
import com.sharemeal.app.domain.model.PasswordResetResult
import com.sharemeal.app.domain.repository.AuthenticationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnavailableAuthenticationRepository @Inject constructor(
    private val sessionManager: SessionManager
) : AuthenticationRepository {

    override val session = sessionManager.session
    override val isSessionRestored = sessionManager.isSessionRestored

    override suspend fun restoreSession(): AppResult<AuthSession?> = AppResult.Success(null)

    override suspend fun login(
        email: String,
        password: String,
        rememberSession: Boolean
    ): AppResult<AuthSession> = backendUnavailable()

    override suspend fun registerDonor(
        request: DonorRegistrationRequest
    ): AppResult<AuthSession> = backendUnavailable()

    override suspend fun registerNgo(
        request: NgoRegistrationRequest
    ): AppResult<AuthSession> = backendUnavailable()

    override suspend fun requestPasswordReset(
        email: String
    ): AppResult<PasswordResetResult> = backendUnavailable()

    override suspend fun logout(): AppResult<Unit> {
        sessionManager.clearSession()
        return AppResult.Success(Unit)
    }

    private fun <T> backendUnavailable(): AppResult<T> = AppResult.Failure(
        AppError.Unknown("Authentication backend is not configured for this build.")
    )
}
