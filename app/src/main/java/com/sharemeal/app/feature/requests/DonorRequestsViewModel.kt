package com.sharemeal.app.feature.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.User
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

data class DonorRequestsUiState(
    val isLoading: Boolean = true,
    val requests: List<com.sharemeal.app.domain.model.DonationRequest> = emptyList(),
    val ngoProfiles: Map<String, User> = emptyMap(),
    val errorText: String? = null,
    val infoText: String? = null
)

@HiltViewModel
class DonorRequestsViewModel @Inject constructor(
    private val requestRepository: DonationRequestRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DonorRequestsUiState())
    val uiState: StateFlow<DonorRequestsUiState> = _uiState.asStateFlow()

    init {
        observeRequests()
    }

    private fun observeRequests() {
        viewModelScope.launch {
            val user = userRepository.currentUser.value
                ?: (userRepository.getCurrentUser() as? AppResult.Success)?.data
            requestRepository.requests.collect { all ->
                val incoming = if (user == null || user.role != UserRole.DONOR) emptyList()
                else all.filter { it.donorId == user.id }.sortedByDescending { it.requestedAt }
                _uiState.update {
                    it.copy(isLoading = false, requests = incoming)
                }
                incoming.map { it.ngoId }.distinct().forEach { ngoId ->
                    if (_uiState.value.ngoProfiles.containsKey(ngoId)) return@forEach
                    when (val result = userRepository.getUser(ngoId)) {
                        is AppResult.Success -> _uiState.update { state ->
                            state.copy(ngoProfiles = state.ngoProfiles + (ngoId to result.data))
                        }
                        is AppResult.Failure -> Unit
                    }
                }
            }
        }
    }

    fun approve(requestId: String, approvedQuantity: Int, pickupTime: Long?) = mutate(
        action = { requestRepository.approveRequest(requestId, approvedQuantity, pickupTime) },
        successMessage = "Request approved. Quantity has been allocated.",
        pendingMessage = "Approval queued for synchronization. Server confirmation is pending."
    )

    fun reject(requestId: String, reason: String?) = mutate(
        action = { requestRepository.rejectRequest(requestId, reason) },
        successMessage = "Request rejected."
    )

    fun schedulePickup(requestId: String, pickupTime: Long) = mutate(
        action = { requestRepository.schedulePickup(requestId, pickupTime) },
        successMessage = "Pickup scheduled. NGO has been notified."
    )

    fun complete(requestId: String) = mutate(
        action = { requestRepository.markCompleted(requestId) },
        successMessage = "Distribution marked complete."
    )

    fun clearMessages() {
        _uiState.update { it.copy(errorText = null, infoText = null) }
    }

    private fun mutate(
        action: suspend () -> AppResult<com.sharemeal.app.domain.model.DonationRequest>,
        successMessage: String,
        pendingMessage: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorText = null, infoText = null) }
            when (val result = action()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        infoText = if (result.data.syncPending && pendingMessage != null) {
                            pendingMessage
                        } else {
                            successMessage
                        }
                    )
                }

                is AppResult.Failure -> _uiState.update { it.copy(errorText = result.error.message) }
            }
        }
    }
}
