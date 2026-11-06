package com.sharemeal.app.feature.donation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.R
import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.media.ImageReference
import com.sharemeal.app.core.media.ImageRepository
import com.sharemeal.app.core.validation.DonationField
import com.sharemeal.app.core.validation.DonationForm
import com.sharemeal.app.core.validation.DonationValidator
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.repository.DonationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DonationFormUiState(
    val form: DonationForm = DonationForm(),
    val fieldErrors: Map<DonationField, Int> = emptyMap(),
    val isPublishing: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

sealed interface DonationFormEvent {
    data class Published(val donation: FoodDonation) : DonationFormEvent
}

@HiltViewModel
class DonationFormViewModel @Inject constructor(
    private val donationRepository: DonationRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DonationFormUiState())
    val uiState: StateFlow<DonationFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DonationFormEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DonationFormEvent> = _events.asSharedFlow()

    fun onFieldChange(field: DonationField, value: String) {
        _uiState.update { state ->
            val form = when (field) {
                DonationField.TITLE -> state.form.copy(title = value)
                DonationField.DESCRIPTION -> state.form.copy(description = value)
                DonationField.QUANTITY -> state.form.copy(quantity = value)
                DonationField.CUSTOM_QUANTITY_UNIT ->
                    state.form.copy(customQuantityUnit = value)
                DonationField.STORAGE_CONDITIONS ->
                    state.form.copy(storageConditions = value)
                DonationField.PACKAGING -> state.form.copy(packaging = value)
                DonationField.ALLERGENS -> state.form.copy(allergenInformation = value)
                DonationField.PICKUP_ADDRESS -> state.form.copy(pickupAddress = value)
                DonationField.CITY -> state.form.copy(city = value)
                DonationField.STATE -> state.form.copy(state = value)
                DonationField.PIN_CODE -> state.form.copy(pinCode = value)
                DonationField.CONTACT_PERSON -> state.form.copy(contactPerson = value)
                DonationField.CONTACT_PHONE -> state.form.copy(contactPhone = value)
                else -> state.form
            }
            state.copy(
                form = form,
                fieldErrors = state.fieldErrors - field,
                errorMessageRes = null
            )
        }
    }

    fun onSpecialInstructionsChange(value: String) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(specialInstructions = value),
                errorMessageRes = null
            )
        }
    }

    fun onCategoryChange(category: FoodCategory) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(category = category),
                fieldErrors = state.fieldErrors - DonationField.CATEGORY,
                errorMessageRes = null
            )
        }
    }

    fun onQuantityUnitChange(unit: DonationQuantityUnit) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(quantityUnit = unit),
                fieldErrors = state.fieldErrors -
                    DonationField.QUANTITY_UNIT -
                    DonationField.CUSTOM_QUANTITY_UNIT,
                errorMessageRes = null
            )
        }
    }

    fun onClassificationChange(classification: FoodClassification) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(classification = classification),
                fieldErrors = state.fieldErrors - DonationField.CLASSIFICATION,
                errorMessageRes = null
            )
        }
    }

    fun onImagesSelected(localUris: List<String>) {
        if (localUris.isEmpty()) return

        _uiState.update { state ->
            val remainingSlots = MAX_IMAGES - state.form.images.size
            if (remainingSlots <= 0) {
                return@update state
            }

            val existingUris = state.form.images
                .mapNotNull(ImageReference::localUri)
                .toSet()
            val additions = localUris
                .asSequence()
                .filter(String::isNotBlank)
                .distinct()
                .filterNot(existingUris::contains)
                .take(remainingSlots)
                .map(imageRepository::createLocalPreview)
                .toList()

            if (additions.isEmpty()) {
                state
            } else {
                state.copy(
                    form = state.form.copy(images = state.form.images + additions),
                    fieldErrors = state.fieldErrors - DonationField.IMAGE,
                    errorMessageRes = null
                )
            }
        }
    }

    fun onImageReplaced(index: Int, localUri: String) {
        if (localUri.isBlank()) return

        _uiState.update { state ->
            if (index !in state.form.images.indices) {
                state
            } else {
                val images = state.form.images.toMutableList().apply {
                    this[index] = imageRepository.createLocalPreview(localUri)
                }
                state.copy(
                    form = state.form.copy(images = images),
                    fieldErrors = state.fieldErrors - DonationField.IMAGE,
                    errorMessageRes = null
                )
            }
        }
    }

    fun onImageRemoved(imageId: String) {
        _uiState.update { state ->
            val images = state.form.images.filterNot { it.id == imageId }
            if (images.size == state.form.images.size) {
                state
            } else {
                state.copy(
                    form = state.form.copy(images = images),
                    fieldErrors = state.fieldErrors - DonationField.IMAGE,
                    errorMessageRes = null
                )
            }
        }
    }

    fun imageDisplayUri(image: ImageReference): String = imageRepository.displayUrl(image)

    fun onPreparationDateSelected(dateMillis: Long) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(preparationDateMillis = dateMillis),
                fieldErrors = state.fieldErrors - DonationField.PREPARATION_DATE,
                errorMessageRes = null
            )
        }
    }

    fun onPreparationTimeSelected(minutes: Int) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(preparationTimeMinutes = minutes),
                fieldErrors = state.fieldErrors - DonationField.PREPARATION_TIME,
                errorMessageRes = null
            )
        }
    }

    fun onPickupStartSelected(minutes: Int) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(pickupStartMinutes = minutes),
                fieldErrors = state.fieldErrors -
                    DonationField.PICKUP_START -
                    DonationField.PICKUP_DEADLINE,
                errorMessageRes = null
            )
        }
    }

    fun onPickupDeadlineSelected(minutes: Int) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(pickupDeadlineMinutes = minutes),
                fieldErrors = state.fieldErrors -
                    DonationField.PICKUP_START -
                    DonationField.PICKUP_DEADLINE,
                errorMessageRes = null
            )
        }
    }

    fun onConditionDeclaredChange(declared: Boolean) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(conditionDeclared = declared),
                fieldErrors = state.fieldErrors - DonationField.CONDITION_DECLARATION,
                errorMessageRes = null
            )
        }
    }

    fun publishDonation() {
        val state = _uiState.value
        if (state.isPublishing) return

        val validation = DonationValidator.validate(state.form)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(fieldErrors = validation.fieldErrors, errorMessageRes = null)
            }
            return
        }

        val form = state.form
        val preparationDate = requireNotNull(form.preparationDateMillis)
        val preparationTime = requireNotNull(form.preparationTimeMinutes)
        val pickupStart = requireNotNull(form.pickupStartMinutes)
        val pickupDeadline = requireNotNull(form.pickupDeadlineMinutes)

        val draft = DonationDraft(
            title = form.title.trim(),
            description = form.description.trim(),
            category = requireNotNull(form.category),
            images = form.images,
            quantity = form.quantity.trim().toInt(),
            quantityUnit = requireNotNull(form.quantityUnit),
            customQuantityUnit = form.customQuantityUnit
                .trim()
                .takeIf { form.quantityUnit == DonationQuantityUnit.OTHER && it.isNotEmpty() },
            classification = requireNotNull(form.classification),
            preparedAt = combineDateAndTime(preparationDate, preparationTime),
            pickupWindowStart = combineDateAndTime(preparationDate, pickupStart),
            pickupDeadline = combineDateAndTime(preparationDate, pickupDeadline),
            storageConditions = form.storageConditions.trim(),
            packaging = form.packaging.trim(),
            allergenInformation = form.allergenInformation.trim(),
            foodConditionDeclared = form.conditionDeclared,
            pickupAddress = form.pickupAddress.trim(),
            city = form.city.trim(),
            state = form.state.trim(),
            pinCode = form.pinCode.trim(),
            contactPerson = form.contactPerson.trim(),
            contactPhone = form.contactPhone.trim(),
            specialInstructions = form.specialInstructions.trim().takeIf(String::isNotEmpty)
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPublishing = true,
                    fieldErrors = emptyMap(),
                    errorMessageRes = null
                )
            }
            when (val result = donationRepository.publishDonation(draft)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isPublishing = false) }
                    _events.emit(DonationFormEvent.Published(result.data))
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isPublishing = false,
                            errorMessageRes = result.error.publishMessageRes()
                        )
                    }
                }
            }
        }
    }

    private fun combineDateAndTime(dateMillis: Long, minutes: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun AppError.publishMessageRes(): Int = when (this) {
        is AppError.Network -> R.string.error_network
        is AppError.Unauthorized -> R.string.error_session_expired
        else -> R.string.error_generic
    }

    companion object {
        const val MAX_IMAGES = 5
    }
}
