package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.AuthSession
import com.sharemeal.app.domain.model.DonorRegistrationRequest
import com.sharemeal.app.domain.model.NgoRegistrationRequest
import com.sharemeal.app.domain.model.PasswordResetResult
import kotlinx.coroutines.flow.StateFlow

interface AuthenticationRepository {
    val session: StateFlow<AuthSession?>
    val isSessionRestored: StateFlow<Boolean>

    suspend fun restoreSession(): AppResult<AuthSession?>

    suspend fun login(
        email: String,
        password: String,
        rememberSession: Boolean
    ): AppResult<AuthSession>

    suspend fun registerDonor(request: DonorRegistrationRequest): AppResult<AuthSession>

    suspend fun registerNgo(request: NgoRegistrationRequest): AppResult<AuthSession>

    suspend fun requestPasswordReset(email: String): AppResult<PasswordResetResult>

    suspend fun logout(): AppResult<Unit>
}
