package com.sharemeal.app.feature.discovery

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.R
import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.repository.DonationRequestRepository
import com.sharemeal.app.domain.repository.FoodDiscoveryRepository
import com.sharemeal.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DonationDetailUiState(
    val isLoading: Boolean = true,
    val item: FoodDiscoveryItem? = null,
    val currentUser: User? = null,
    val isVerified: Boolean = false,
    val verificationStatus: NgoVerificationStatus = NgoVerificationStatus.PENDING,
    val isSubmittingRequest: Boolean = false,
    val requestSubmitted: Boolean = false,
    val submittedRequest: DonationRequest? = null,
    @StringRes val errorMessageRes: Int? = null,
    val errorText: String? = null
)

@HiltViewModel
class DonationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodDiscoveryRepository: FoodDiscoveryRepository,
    private val requestRepository: DonationRequestRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val donationId: String = checkNotNull(savedStateHandle["donationId"])

    private val _uiState = MutableStateFlow(DonationDetailUiState())
    val uiState: StateFlow<DonationDetailUiState> = _uiState.asStateFlow()

    init {
        loadDonationDetails()
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                if (user != null) {
                    val status = user.ngoProfile?.verificationStatus ?: NgoVerificationStatus.PENDING
                    _uiState.update {
                        it.copy(
                            currentUser = user,
                            verificationStatus = status,
                            isVerified = status == NgoVerificationStatus.VERIFIED
                        )
                    }
                }
            }
        }
    }

    fun loadDonationDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null, errorText = null) }
            val userResult = userRepository.getCurrentUser()
            if (userResult is AppResult.Success) {
                val user = userResult.data
                val status = user.ngoProfile?.verificationStatus ?: NgoVerificationStatus.PENDING
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        verificationStatus = status,
                        isVerified = status == NgoVerificationStatus.VERIFIED
                    )
                }
            }

            when (val result = foodDiscoveryRepository.getDonationById(donationId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            item = result.data,
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
    }

    fun submitRequest(
        requestedQuantity: Int,
        peopleToServe: Int,
        expectedPickupAt: Long,
        notes: String?
    ) {
        val state = _uiState.value
        val donation = state.item?.donation ?: return

        if (!state.isVerified) {
            _uiState.update {
                it.copy(
                    errorText = "Verification Pending: Only verified NGOs can submit donation requests."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRequest = true, errorText = null, errorMessageRes = null) }
            when (
                val result = requestRepository.submitRequest(
                    donationId = donation.id,
                    requestedQuantity = requestedQuantity,
                    peopleToServe = peopleToServe,
                    expectedPickupAt = expectedPickupAt,
                    notes = notes,
                    idempotencyKey = "${state.currentUser?.id}-${donation.id}-$expectedPickupAt"
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingRequest = false,
                            requestSubmitted = true,
                            submittedRequest = result.data
                        )
                    }
                }

                is AppResult.Failure -> {
                    val msg = if (result.error is AppError.Forbidden) {
                        result.error.message
                    } else null
                    _uiState.update {
                        it.copy(
                            isSubmittingRequest = false,
                            errorText = msg,
                            errorMessageRes = if (msg == null) result.error.messageRes() else null
                        )
                    }
                }
            }
        }
    }

    private fun AppError.messageRes(): Int = when (this) {
        is AppError.NotFound -> R.string.error_generic
        is AppError.Network -> R.string.error_network
        else -> R.string.error_generic
    }
}
