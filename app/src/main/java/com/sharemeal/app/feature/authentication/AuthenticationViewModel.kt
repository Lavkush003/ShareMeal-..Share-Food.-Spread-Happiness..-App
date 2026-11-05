package com.sharemeal.app.feature.authentication

import androidx.annotation.StringRes
import com.sharemeal.app.R
import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.config.AppConfig
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.core.validation.AuthValidator
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.repository.AuthenticationRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthenticationUiState(
    val email: String = "",
    val password: String = "",
    val rememberSession: Boolean = true,
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val fieldErrors: Map<AuthField, Int> = emptyMap(),
    @StringRes val errorMessageRes: Int? = null,
    val resetEmail: String = "",
    val isRequestingReset: Boolean = false,
    @StringRes val resetFieldErrorRes: Int? = null,
    @StringRes val resetMessageRes: Int? = null
)

sealed interface AuthenticationEvent {
    data class Authenticated(val role: UserRole) : AuthenticationEvent
    data object LoggedOut : AuthenticationEvent
}

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    appConfig: AppConfig
) : ViewModel() {

    val isMockMode: Boolean = appConfig.isMockMode

    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState: StateFlow<AuthenticationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthenticationEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthenticationEvent> = _events.asSharedFlow()

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                fieldErrors = it.fieldErrors - AuthField.EMAIL,
                errorMessageRes = null
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                fieldErrors = it.fieldErrors - AuthField.PASSWORD,
                errorMessageRes = null
            )
        }
    }

    fun onRememberSessionChange(value: Boolean) {
        _uiState.update { it.copy(rememberSession = value) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val validation = AuthValidator.validateLogin(state.email, state.password)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(fieldErrors = validation.fieldErrors, errorMessageRes = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true, fieldErrors = emptyMap(), errorMessageRes = null)
            }
            when (
                val result = authenticationRepository.login(
                    email = state.email,
                    password = state.password,
                    rememberSession = state.rememberSession
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, password = "") }
                    _events.emit(AuthenticationEvent.Authenticated(result.data.role))
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessageRes = result.error.loginMessageRes()
                        )
                    }
                }
            }
        }
    }

    fun onResetEmailChange(value: String) {
        _uiState.update {
            it.copy(
                resetEmail = value,
                resetFieldErrorRes = null,
                resetMessageRes = null
            )
        }
    }

    fun requestPasswordReset() {
        val state = _uiState.value
        if (state.isRequestingReset) return

        val validation = AuthValidator.validateForgotPassword(state.resetEmail)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    resetFieldErrorRes = validation.fieldErrors[AuthField.EMAIL],
                    resetMessageRes = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isRequestingReset = true, resetFieldErrorRes = null, resetMessageRes = null)
            }
            when (authenticationRepository.requestPasswordReset(state.resetEmail)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRequestingReset = false,
                            resetMessageRes = R.string.forgot_password_backend_required
                        )
                    }
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isRequestingReset = false,
                            resetFieldErrorRes = R.string.error_email_invalid
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authenticationRepository.logout()
            _events.emit(AuthenticationEvent.LoggedOut)
        }
    }

    private fun AppError.loginMessageRes(): Int = when (this) {
        is AppError.Unauthorized -> R.string.error_invalid_credentials
        is AppError.Network -> R.string.error_network
        is AppError.Conflict -> R.string.error_email_in_use
        is AppError.Validation -> R.string.error_validation_summary
        else -> R.string.error_generic
    }
}
