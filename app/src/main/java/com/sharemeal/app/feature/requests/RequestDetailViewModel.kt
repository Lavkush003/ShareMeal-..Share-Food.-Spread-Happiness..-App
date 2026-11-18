package com.sharemeal.app.feature.requests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.navigation.Routes.ARG_REQUEST_ID
import com.sharemeal.app.domain.model.DonationQuantitySummary
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.repository.DonationRepository
import com.sharemeal.app.domain.repository.DonationRequestRepository
import com.sharemeal.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestDetailUiState(
    val isLoading: Boolean = true,
    val request: DonationRequest? = null,
    val donation: FoodDonation? = null,
    val ngo: User? = null,
    val summary: DonationQuantitySummary? = null,
    val isDonor: Boolean = false,
    val ngoVerification: NgoVerificationStatus = NgoVerificationStatus.PENDING,
    val errorText: String? = null,
    val infoText: String? = null
)

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val requestRepository: DonationRequestRepository,
    private val donationRepository: DonationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val requestId: String? = savedStateHandle[ARG_REQUEST_ID]

    private val _uiState = MutableStateFlow(RequestDetailUiState())
    val uiState: StateFlow<RequestDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val id = requestId
        if (id.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, errorText = "Request reference is missing.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorText = null) }
            val request = when (val result = requestRepository.getRequest(id)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorText = result.error.message) }
                    return@launch
                }
            }
            val currentUser = userRepository.currentUser.value
                ?: (userRepository.getCurrentUser() as? AppResult.Success)?.data
            val donation = (donationRepository.getDonationById(request.donationId) as? AppResult.Success)?.data
            val ngo = (userRepository.getUser(request.ngoId) as? AppResult.Success)?.data
            val summary = (requestRepository.getQuantitySummary(request.donationId) as? AppResult.Success)?.data
            _uiState.update {
                it.copy(
                    isLoading = false,
                    request = request,
                    donation = donation,
                    ngo = ngo,
                    summary = summary,
                    isDonor = currentUser?.role == UserRole.DONOR,
                    ngoVerification = ngo?.ngoProfile?.verificationStatus
                        ?: NgoVerificationStatus.PENDING
                )
            }
        }
    }

    fun approve(approvedQuantity: Int, pickupTime: Long?) = mutate(
        action = { requestId?.let { requestRepository.approveRequest(it, approvedQuantity, pickupTime) }
            ?: missingRequest() },
        successMessage = "Request approved and quantity allocated.",
        pendingMessage = "Approval queued for synchronization. Server confirmation is pending."
    )

    fun reject(reason: String?) = mutate(
        action = { requestId?.let { requestRepository.rejectRequest(it, reason) } ?: missingRequest() },
        successMessage = "Request rejected."
    )

    fun cancel() = mutate(
        action = { requestId?.let { requestRepository.cancelRequest(it) } ?: missingRequest() },
        successMessage = "Request cancelled and allocation released."
    )

    fun schedulePickup(pickupTime: Long) = mutate(
        action = { requestId?.let { requestRepository.schedulePickup(it, pickupTime) } ?: missingRequest() },
        successMessage = "Pickup scheduled."
    )

    fun confirmCollected() = mutate(
        action = { requestId?.let { requestRepository.markCollected(it) } ?: missingRequest() },
        successMessage = "Collection confirmed."
    )

    fun complete() = mutate(
        action = { requestId?.let { requestRepository.markCompleted(it) } ?: missingRequest() },
        successMessage = "Distribution completed."
    )

    fun clearMessages() {
        _uiState.update { it.copy(errorText = null, infoText = null) }
    }

    private fun missingRequest(): AppResult<DonationRequest> = AppResult.Failure(
        AppError.Validation(message = "Missing request reference.")
    )

    private fun mutate(
        action: suspend () -> AppResult<DonationRequest>,
        successMessage: String,
        pendingMessage: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorText = null, infoText = null) }
            when (val result = action()) {
                is AppResult.Success -> {
                    val message = if (result.data.syncPending && pendingMessage != null) {
                        pendingMessage
                    } else {
                        successMessage
                    }
                    _uiState.update { it.copy(infoText = message) }
                    load()
                }
                is AppResult.Failure -> _uiState.update { it.copy(errorText = result.error.message) }
            }
        }
    }
}
