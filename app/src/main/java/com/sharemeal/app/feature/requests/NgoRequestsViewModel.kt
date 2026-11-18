package com.sharemeal.app.feature.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.repository.DonationRequestRepository
import com.sharemeal.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NgoRequestsUiState(
    val isLoading: Boolean = true,
    val requests: List<DonationRequest> = emptyList(),
    val errorText: String? = null,
    val infoText: String? = null
)

@HiltViewModel
class NgoRequestsViewModel @Inject constructor(
    private val requestRepository: DonationRequestRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NgoRequestsUiState())
    val uiState: StateFlow<NgoRequestsUiState> = _uiState.asStateFlow()

    init {
        observeRequests()
    }

    private fun observeRequests() {
        viewModelScope.launch {
            val user = userRepository.currentUser.value
                ?: (userRepository.getCurrentUser() as? AppResult.Success)?.data
            requestRepository.requests.collect { all ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        requests = if (user == null) emptyList()
                        else all.filter { request -> request.ngoId == user.id }
                            .sortedByDescending { request -> request.requestedAt }
                    )
                }
            }
        }
    }

    fun cancelRequest(requestId: String) = mutate(
        action = { requestRepository.cancelRequest(requestId) },
        successMessage = "Request cancelled. Allocated quantity has been released."
    )

    fun confirmCollected(requestId: String) = mutate(
        action = { requestRepository.markCollected(requestId) },
        successMessage = "Collection confirmed."
    )

    fun clearMessages() {
        _uiState.update { it.copy(errorText = null, infoText = null) }
    }

    private fun mutate(
        action: suspend () -> AppResult<DonationRequest>,
        successMessage: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorText = null, infoText = null) }
            when (val result = action()) {
                is AppResult.Success -> _uiState.update { it.copy(infoText = successMessage) }
                is AppResult.Failure -> _uiState.update { it.copy(errorText = result.error.message) }
            }
        }
    }
}
