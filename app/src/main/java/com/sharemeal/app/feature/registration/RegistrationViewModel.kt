package com.sharemeal.app.feature.registration

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
import com.sharemeal.app.domain.model.DonorRegistrationRequest
import com.sharemeal.app.domain.model.NgoRegistrationRequest
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.repository.AuthenticationRepository
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

data class DonorRegistrationForm(
    val fullName: String = "",
    val organizationName: String = "",
    val email: String = "",
    val mobile: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val category: DonorCategory? = null,
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = ""
) {
    fun address(): Address = Address(
        line1 = addressLine.trim(),
        city = city.trim(),
        state = state.trim(),
        pinCode = pinCode.trim()
    )
}

data class NgoRegistrationForm(
    val ngoName: String = "",
    val registrationNumber: String = "",
    val contactPersonName: String = "",
    val email: String = "",
    val mobile: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = "",
    val operatingArea: String = "",
    val capacity: String = "",
    val documentInfo: String = ""
) {
    fun address(): Address = Address(
        line1 = addressLine.trim(),
        city = city.trim(),
        state = state.trim(),
        pinCode = pinCode.trim()
    )
}

data class RegistrationUiState(
    val selectedRole: UserRole? = null,
    val donor: DonorRegistrationForm = DonorRegistrationForm(),
    val ngo: NgoRegistrationForm = NgoRegistrationForm(),
    val donorFieldErrors: Map<AuthField, Int> = emptyMap(),
    val ngoFieldErrors: Map<AuthField, Int> = emptyMap(),
    val isSubmitting: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    val isDonorPasswordVisible: Boolean = false,
    val isDonorConfirmPasswordVisible: Boolean = false,
    val isNgoPasswordVisible: Boolean = false,
    val isNgoConfirmPasswordVisible: Boolean = false
)

sealed interface RegistrationEvent {
    data class Registered(val role: UserRole) : RegistrationEvent
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RegistrationEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RegistrationEvent> = _events.asSharedFlow()

    fun setRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun onDonorChange(field: AuthField, value: String) {
        _uiState.update { state ->
            val form = when (field) {
                AuthField.FULL_NAME -> state.donor.copy(fullName = value)
                AuthField.ORGANIZATION_NAME -> state.donor.copy(organizationName = value)
                AuthField.EMAIL -> state.donor.copy(email = value)
                AuthField.MOBILE -> state.donor.copy(mobile = value)
                AuthField.PASSWORD -> state.donor.copy(password = value)
                AuthField.CONFIRM_PASSWORD -> state.donor.copy(confirmPassword = value)
                AuthField.ADDRESS_LINE -> state.donor.copy(addressLine = value)
                AuthField.CITY -> state.donor.copy(city = value)
                AuthField.STATE -> state.donor.copy(state = value)
                AuthField.PIN_CODE -> state.donor.copy(pinCode = value)
                else -> state.donor
            }
            state.copy(
                donor = form,
                donorFieldErrors = state.donorFieldErrors - field,
                errorMessageRes = null
            )
        }
    }

    fun onDonorCategoryChange(category: DonorCategory) {
        _uiState.update {
            it.copy(
                donor = it.donor.copy(category = category),
                donorFieldErrors = it.donorFieldErrors - AuthField.DONOR_CATEGORY,
                errorMessageRes = null
            )
        }
    }

    fun onNgoChange(field: AuthField, value: String) {
        _uiState.update { state ->
            val form = when (field) {
                AuthField.NGO_NAME -> state.ngo.copy(ngoName = value)
                AuthField.REGISTRATION_NUMBER -> state.ngo.copy(registrationNumber = value)
                AuthField.CONTACT_PERSON -> state.ngo.copy(contactPersonName = value)
                AuthField.EMAIL -> state.ngo.copy(email = value)
                AuthField.MOBILE -> state.ngo.copy(mobile = value)
                AuthField.PASSWORD -> state.ngo.copy(password = value)
                AuthField.CONFIRM_PASSWORD -> state.ngo.copy(confirmPassword = value)
                AuthField.ADDRESS_LINE -> state.ngo.copy(addressLine = value)
                AuthField.CITY -> state.ngo.copy(city = value)
                AuthField.STATE -> state.ngo.copy(state = value)
                AuthField.PIN_CODE -> state.ngo.copy(pinCode = value)
                AuthField.OPERATING_AREA -> state.ngo.copy(operatingArea = value)
                AuthField.CAPACITY -> state.ngo.copy(capacity = value)
                AuthField.DOCUMENT_INFO -> state.ngo.copy(documentInfo = value)
                else -> state.ngo
            }
            state.copy(
                ngo = form,
                ngoFieldErrors = state.ngoFieldErrors - field,
                errorMessageRes = null
            )
        }
    }

    fun toggleDonorPasswordVisibility() {
        _uiState.update { it.copy(isDonorPasswordVisible = !it.isDonorPasswordVisible) }
    }

    fun toggleDonorConfirmPasswordVisibility() {
        _uiState.update {
            it.copy(isDonorConfirmPasswordVisible = !it.isDonorConfirmPasswordVisible)
        }
    }

    fun toggleNgoPasswordVisibility() {
        _uiState.update { it.copy(isNgoPasswordVisible = !it.isNgoPasswordVisible) }
    }

    fun toggleNgoConfirmPasswordVisibility() {
        _uiState.update {
            it.copy(isNgoConfirmPasswordVisible = !it.isNgoConfirmPasswordVisible)
        }
    }

    fun registerDonor() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val form = state.donor
        val validation = AuthValidator.validateDonorRegistration(
            fullName = form.fullName,
            organizationName = form.organizationName,
            email = form.email,
            mobile = form.mobile,
            password = form.password,
            confirmPassword = form.confirmPassword,
            category = form.category,
            address = form.address()
        )
        if (!validation.isValid) {
            _uiState.update {
                it.copy(donorFieldErrors = validation.fieldErrors, errorMessageRes = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true, donorFieldErrors = emptyMap(), errorMessageRes = null)
            }
            val request = DonorRegistrationRequest(
                fullName = form.fullName.trim(),
                organizationName = form.organizationName.trim().takeIf(String::isNotEmpty),
                email = form.email,
                mobile = form.mobile,
                password = form.password,
                category = requireNotNull(form.category),
                address = form.address()
            )
            when (val result = authenticationRepository.registerDonor(request)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.emit(RegistrationEvent.Registered(result.data.role))
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessageRes = result.error.registrationMessageRes()
                        )
                    }
                }
            }
        }
    }

    fun registerNgo() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val form = state.ngo
        val validation = AuthValidator.validateNgoRegistration(
            ngoName = form.ngoName,
            registrationNumber = form.registrationNumber,
            contactPersonName = form.contactPersonName,
            email = form.email,
            mobile = form.mobile,
            password = form.password,
            confirmPassword = form.confirmPassword,
            address = form.address(),
            operatingArea = form.operatingArea,
            capacity = form.capacity,
            documentInfo = form.documentInfo
        )
        if (!validation.isValid) {
            _uiState.update {
                it.copy(ngoFieldErrors = validation.fieldErrors, errorMessageRes = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true, ngoFieldErrors = emptyMap(), errorMessageRes = null)
            }
            val request = NgoRegistrationRequest(
                ngoName = form.ngoName.trim(),
                registrationNumber = form.registrationNumber.trim().takeIf(String::isNotEmpty),
                contactPersonName = form.contactPersonName.trim(),
                email = form.email,
                mobile = form.mobile,
                password = form.password,
                address = form.address(),
                operatingArea = form.operatingArea.trim(),
                estimatedDistributionCapacity = form.capacity.trim(),
                verificationDocumentInfo = form.documentInfo.trim()
            )
            when (val result = authenticationRepository.registerNgo(request)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.emit(RegistrationEvent.Registered(result.data.role))
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessageRes = result.error.registrationMessageRes()
                        )
                    }
                }
            }
        }
    }

    private fun AppError.registrationMessageRes(): Int = when (this) {
        is AppError.Conflict -> R.string.error_email_in_use
        is AppError.Network -> R.string.error_network
        is AppError.Validation -> R.string.error_validation_summary
        else -> R.string.error_generic
    }
}
