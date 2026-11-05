package com.sharemeal.app.feature.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.R
import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.core.validation.AuthValidator
import com.sharemeal.app.domain.model.Address
import com.sharemeal.app.domain.model.DonorCategory
import com.sharemeal.app.domain.model.ProfileUpdate
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.repository.AuthenticationRepository
import com.sharemeal.app.domain.repository.UserRepository
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

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val user: User? = null,
    val displayName: String = "",
    val mobile: String = "",
    val organizationName: String = "",
    val contactPersonName: String = "",
    val donorCategory: DonorCategory? = null,
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = "",
    val operatingArea: String = "",
    val capacity: String = "",
    val fieldErrors: Map<AuthField, Int> = emptyMap(),
    @StringRes val errorMessageRes: Int? = null,
    @StringRes val successMessageRes: Int? = null
)

sealed interface ProfileEvent {
    data object LoggedOut : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessageRes = null, successMessageRes = null)
            }
            when (val result = userRepository.getCurrentUser()) {
                is AppResult.Success -> {
                    _uiState.update { it.withUser(result.data) }
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = result.error.profileMessageRes()
                        )
                    }
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(
                isEditing = true,
                fieldErrors = emptyMap(),
                errorMessageRes = null,
                successMessageRes = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { state ->
            val user = state.user
            if (user == null) {
                state.copy(isEditing = false, fieldErrors = emptyMap())
            } else {
                state.withUser(user).copy(
                    isEditing = false,
                    fieldErrors = emptyMap(),
                    errorMessageRes = null
                )
            }
        }
    }

    fun onFieldChange(field: AuthField, value: String) {
        _uiState.update { state ->
            val updated = when (field) {
                AuthField.FULL_NAME -> state.copy(displayName = value)
                AuthField.ORGANIZATION_NAME -> state.copy(organizationName = value)
                AuthField.MOBILE -> state.copy(mobile = value)
                AuthField.CONTACT_PERSON -> state.copy(contactPersonName = value)
                AuthField.ADDRESS_LINE -> state.copy(addressLine = value)
                AuthField.CITY -> state.copy(city = value)
                AuthField.STATE -> state.copy(state = value)
                AuthField.PIN_CODE -> state.copy(pinCode = value)
                AuthField.OPERATING_AREA -> state.copy(operatingArea = value)
                AuthField.CAPACITY -> state.copy(capacity = value)
                else -> state
            }
            updated.copy(
                fieldErrors = updated.fieldErrors - field,
                errorMessageRes = null,
                successMessageRes = null
            )
        }
    }

    fun onDonorCategoryChange(category: DonorCategory) {
        _uiState.update {
            it.copy(
                donorCategory = category,
                fieldErrors = it.fieldErrors - AuthField.DONOR_CATEGORY,
                errorMessageRes = null,
                successMessageRes = null
            )
        }
    }

    fun saveProfile() {
        val state = _uiState.value
        val user = state.user ?: return
        if (state.isSaving) return

        val address = Address(
            line1 = state.addressLine.trim(),
            city = state.city.trim(),
            state = state.state.trim(),
            pinCode = state.pinCode.trim()
        )
        val validation = AuthValidator.validateProfile(
            displayName = state.displayName,
            mobile = state.mobile,
            address = address,
            role = user.role,
            donorCategory = state.donorCategory,
            operatingArea = state.operatingArea,
            capacity = state.capacity
        )
        if (!validation.isValid) {
            _uiState.update { it.copy(fieldErrors = validation.fieldErrors) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    fieldErrors = emptyMap(),
                    errorMessageRes = null,
                    successMessageRes = null
                )
            }
            val update = ProfileUpdate(
                displayName = state.displayName.trim(),
                mobile = state.mobile,
                organizationName = state.organizationName.trim().takeIf(String::isNotEmpty),
                contactPersonName = state.contactPersonName.trim().takeIf(String::isNotEmpty),
                donorCategory = state.donorCategory,
                operatingArea = state.operatingArea.trim().takeIf(String::isNotEmpty),
                estimatedDistributionCapacity = state.capacity.trim().takeIf(String::isNotEmpty),
                address = address
            )
            when (val result = userRepository.updateProfile(update)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.withUser(result.data).copy(
                            isSaving = false,
                            isEditing = false,
                            successMessageRes = R.string.profile_update_success
                        )
                    }
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessageRes = result.error.profileMessageRes()
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authenticationRepository.logout()
            _events.emit(ProfileEvent.LoggedOut)
        }
    }

    private fun ProfileUiState.withUser(user: User): ProfileUiState {
        val address = user.address
        return copy(
            isLoading = false,
            user = user,
            displayName = user.displayName,
            mobile = user.mobile,
            organizationName = user.organizationName.orEmpty(),
            contactPersonName = user.ngoProfile?.contactPersonName.orEmpty(),
            donorCategory = user.donorProfile?.category,
            addressLine = address?.line1.orEmpty(),
            city = address?.city.orEmpty(),
            state = address?.state.orEmpty(),
            pinCode = address?.pinCode.orEmpty(),
            operatingArea = user.ngoProfile?.operatingArea.orEmpty(),
            capacity = user.ngoProfile?.estimatedDistributionCapacity.orEmpty(),
            fieldErrors = emptyMap(),
            errorMessageRes = null
        )
    }

    private fun AppError.profileMessageRes(): Int = when (this) {
        is AppError.Unauthorized -> R.string.error_session_expired
        is AppError.Network -> R.string.error_network
        is AppError.NotFound -> R.string.error_profile_not_found
        is AppError.Validation -> R.string.error_validation_summary
        else -> R.string.error_generic
    }

    private val User.isDonor: Boolean
        get() = role == UserRole.DONOR
}
