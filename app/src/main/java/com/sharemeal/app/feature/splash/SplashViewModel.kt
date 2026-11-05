package com.sharemeal.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.preferences.AppPreferencesRepository
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SplashDestination {
    ONBOARDING,
    WELCOME,
    DONOR_DASHBOARD,
    NGO_DASHBOARD
}

data class SplashUiState(
    val destination: SplashDestination? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val preferences: AppPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = when (val result = authenticationRepository.restoreSession()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> null
            }
            val onboardingCompleted = runCatching {
                preferences.isOnboardingCompleted()
            }.getOrDefault(false)

            val destination = when {
                session?.role == UserRole.DONOR -> SplashDestination.DONOR_DASHBOARD
                session?.role == UserRole.NGO -> SplashDestination.NGO_DASHBOARD
                onboardingCompleted -> SplashDestination.WELCOME
                else -> SplashDestination.ONBOARDING
            }
            _uiState.value = SplashUiState(destination = destination)
        }
    }
}
