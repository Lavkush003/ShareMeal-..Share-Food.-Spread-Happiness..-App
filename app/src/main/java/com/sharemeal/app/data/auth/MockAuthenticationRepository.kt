package com.sharemeal.app.data.auth

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.session.SessionManager
import com.sharemeal.app.core.validation.AuthValidator
import com.sharemeal.app.domain.model.AuthSession
import com.sharemeal.app.domain.model.DonorProfile
import com.sharemeal.app.domain.model.DonorRegistrationRequest
import com.sharemeal.app.domain.model.NgoProfile
import com.sharemeal.app.domain.model.NgoRegistrationRequest
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.PasswordResetResult
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.repository.AuthenticationRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.withLock

@Singleton
class MockAuthenticationRepository @Inject constructor(
    private val seeder: MockAccountSeeder,
    private val accountStore: MockAccountStore,
    private val accountLock: MockAccountLock,
    private val passwordHasher: MockPasswordHasher,
    private val sessionManager: SessionManager
) : AuthenticationRepository {

    override val session = sessionManager.session
    override val isSessionRestored = sessionManager.isSessionRestored

    override suspend fun restoreSession(): AppResult<AuthSession?> = safely {
        val restored = sessionManager.restoreSession()
            ?: return@safely AppResult.Success(null)

        seeder.ensureSeeded()
        val account = accountLock.mutex.withLock {
            accountStore.load().firstOrNull { it.user.id == restored.userId }
        }

        if (account == null) {
            sessionManager.clearSession()
            AppResult.Success(null)
        } else {
            val actualSession = AuthSession(
                userId = account.user.id,
                role = account.user.role
            )
            if (actualSession != restored) {
                sessionManager.persistSession(actualSession, rememberSession = true)
            }
            AppResult.Success(actualSession)
        }
    }

    override suspend fun login(
        email: String,
        password: String,
        rememberSession: Boolean
    ): AppResult<AuthSession> = safely {
        val validation = AuthValidator.validateLogin(email, password)
        if (!validation.isValid) {
            return@safely AppResult.Failure(
                AppError.Validation(message = "Please check the information provided.")
            )
        }

        seeder.ensureSeeded()
        val normalizedEmail = AuthValidator.normalizeEmail(email)
        val account = accountLock.mutex.withLock {
            accountStore.load().firstOrNull {
                AuthValidator.normalizeEmail(it.user.email) == normalizedEmail
            }
        }

        if (account == null ||
            !passwordHasher.verify(password, account.passwordSalt, account.passwordHash)
        ) {
            return@safely AppResult.Failure(
                AppError.Unauthorized("Invalid email or password.")
            )
        }

        val session = AuthSession(
            userId = account.user.id,
            role = account.user.role
        )
        sessionManager.persistSession(session, rememberSession)
        AppResult.Success(session)
    }

    override suspend fun registerDonor(
        request: DonorRegistrationRequest
    ): AppResult<AuthSession> = safely {
        seeder.ensureSeeded()
        val normalizedEmail = AuthValidator.normalizeEmail(request.email)

        accountLock.mutex.withLock {
            val accounts = accountStore.load()
            if (accounts.any { AuthValidator.normalizeEmail(it.user.email) == normalizedEmail }) {
                return@withLock AppResult.Failure(
                    AppError.Conflict("An account with this email already exists.")
                )
            }

            val userId = UUID.randomUUID().toString()
            val user = User(
                id = userId,
                email = normalizedEmail,
                mobile = AuthValidator.normalizeMobile(request.mobile),
                role = com.sharemeal.app.domain.model.UserRole.DONOR,
                displayName = request.fullName.trim(),
                donorProfile = DonorProfile(
                    fullName = request.fullName.trim(),
                    organizationName = request.organizationName?.trim()?.takeIf(String::isNotEmpty),
                    category = request.category,
                    address = request.address
                ),
                createdAt = System.currentTimeMillis()
            )
            val salt = passwordHasher.newSalt()
            accountStore.save(
                accounts + MockAccountRecord(
                    user = user,
                    passwordSalt = salt,
                    passwordHash = passwordHasher.hash(request.password, salt)
                )
            )

            val session = AuthSession(userId = userId, role = user.role)
            sessionManager.persistSession(session, rememberSession = true)
            AppResult.Success(session)
        }
    }

    override suspend fun registerNgo(
        request: NgoRegistrationRequest
    ): AppResult<AuthSession> = safely {
        seeder.ensureSeeded()
        val normalizedEmail = AuthValidator.normalizeEmail(request.email)

        accountLock.mutex.withLock {
            val accounts = accountStore.load()
            if (accounts.any { AuthValidator.normalizeEmail(it.user.email) == normalizedEmail }) {
                return@withLock AppResult.Failure(
                    AppError.Conflict("An account with this email already exists.")
                )
            }

            val userId = UUID.randomUUID().toString()
            val user = User(
                id = userId,
                email = normalizedEmail,
                mobile = AuthValidator.normalizeMobile(request.mobile),
                role = com.sharemeal.app.domain.model.UserRole.NGO,
                displayName = request.ngoName.trim(),
                ngoProfile = NgoProfile(
                    ngoName = request.ngoName.trim(),
                    registrationNumber = request.registrationNumber
                        ?.trim()
                        ?.takeIf(String::isNotEmpty),
                    contactPersonName = request.contactPersonName.trim(),
                    address = request.address,
                    operatingArea = request.operatingArea.trim(),
                    estimatedDistributionCapacity = request.estimatedDistributionCapacity.trim(),
                    verificationDocumentInfo = request.verificationDocumentInfo.trim(),
                    verificationStatus = NgoVerificationStatus.PENDING
                ),
                createdAt = System.currentTimeMillis()
            )
            val salt = passwordHasher.newSalt()
            accountStore.save(
                accounts + MockAccountRecord(
                    user = user,
                    passwordSalt = salt,
                    passwordHash = passwordHasher.hash(request.password, salt)
                )
            )

            val session = AuthSession(userId = userId, role = user.role)
            sessionManager.persistSession(session, rememberSession = true)
            AppResult.Success(session)
        }
    }

    override suspend fun requestPasswordReset(
        email: String
    ): AppResult<PasswordResetResult> = safely {
        val validation = AuthValidator.validateForgotPassword(email)
        if (!validation.isValid) {
            return@safely AppResult.Failure(
                AppError.Validation(message = "Please enter a valid email address.")
            )
        }
        AppResult.Success(PasswordResetResult.BACKEND_REQUIRED)
    }

    override suspend fun logout(): AppResult<Unit> = safely {
        sessionManager.clearSession()
        AppResult.Success(Unit)
    }

    private suspend fun <T> safely(
        block: suspend () -> AppResult<T>
    ): AppResult<T> = try {
        block()
    } catch (error: AppError) {
        AppResult.Failure(error)
    } catch (error: Exception) {
        AppResult.Failure(
            AppError.Unknown("Mock authentication failed.", cause = error)
        )
    }
}
