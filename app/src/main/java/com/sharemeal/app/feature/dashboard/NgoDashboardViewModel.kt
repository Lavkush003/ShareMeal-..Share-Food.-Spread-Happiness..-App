package com.sharemeal.app.feature.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.R
import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.NgoDashboardStats
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.repository.NgoRepository
import com.sharemeal.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NgoDashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val user: User? = null,
    val stats: NgoDashboardStats? = null,
    @StringRes val errorMessageRes: Int? = null,
    @StringRes val infoMessageRes: Int? = null
)

@HiltViewModel
class NgoDashboardViewModel @Inject constructor(
    private val ngoRepository: NgoRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NgoDashboardUiState())
    val uiState: StateFlow<NgoDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.update { it.copy(user = user) }
                    fetchStats(user.id)
                }
            }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
            val userResult = userRepository.getCurrentUser()
            if (userResult is AppResult.Success) {
                val user = userResult.data
                _uiState.update { it.copy(user = user) }
                fetchStats(user.id)
            } else if (userResult is AppResult.Failure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessageRes = userResult.error.messageRes()
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessageRes = null) }
            val user = _uiState.value.user
            if (user != null) {
                fetchStats(user.id)
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleVerificationStatus() {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            when (val result = ngoRepository.toggleVerificationStatus(user.id)) {
                is AppResult.Success -> {
                    val status = result.data
                    val info = if (status == NgoVerificationStatus.VERIFIED) {
                        R.string.ngo_dev_verified_notice
                    } else {
                        R.string.ngo_dev_pending_notice
                    }
                    _uiState.update { it.copy(infoMessageRes = info) }
                    userRepository.getCurrentUser()
                }

                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessageRes = result.error.messageRes()) }
                }
            }
        }
    }

    private suspend fun fetchStats(userId: String) {
        when (val result = ngoRepository.getDashboardStats(userId)) {
            is AppResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats = result.data,
                        errorMessageRes = null
                    )
                }
            }

            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessageRes = result.error.messageRes()
                    )
                }
            }
        }
    }

    private fun AppError.messageRes(): Int = when (this) {
        is AppError.Unauthorized -> R.string.error_session_expired
        is AppError.Network -> R.string.error_network
        else -> R.string.error_generic
    }
}
