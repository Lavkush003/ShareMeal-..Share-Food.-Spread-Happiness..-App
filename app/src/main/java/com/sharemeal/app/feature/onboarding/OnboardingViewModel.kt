package com.sharemeal.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.core.preferences.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val isCompleting: Boolean = false,
    val isCompleted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: AppPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun complete() {
        if (_uiState.value.isCompleting || _uiState.value.isCompleted) return
        viewModelScope.launch {
            _uiState.value = OnboardingUiState(isCompleting = true)
            preferences.setOnboardingCompleted()
            _uiState.value = OnboardingUiState(isCompleted = true)
        }
    }
}
