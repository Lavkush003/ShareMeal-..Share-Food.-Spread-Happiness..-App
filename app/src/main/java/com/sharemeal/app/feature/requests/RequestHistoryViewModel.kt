package com.sharemeal.app.feature.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.repository.DonationRequestRepository
import com.sharemeal.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestHistoryUiState(
    val isLoading: Boolean = true,
    val requests: List<DonationRequest> = emptyList()
)

@HiltViewModel
class RequestHistoryViewModel @Inject constructor(
    private val requestRepository: DonationRequestRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestHistoryUiState())
    val uiState: StateFlow<RequestHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userRepository.currentUser.value
                ?: (userRepository.getCurrentUser() as? AppResult.Success)?.data
            requestRepository.requests.collect { all ->
                val mine = when (user?.role) {
                    UserRole.NGO -> all.filter { it.ngoId == user.id }
                    UserRole.DONOR -> all.filter { it.donorId == user.id }
                    else -> emptyList()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        requests = mine.sortedByDescending { request -> request.requestedAt }
                    )
                }
            }
        }
    }
}
