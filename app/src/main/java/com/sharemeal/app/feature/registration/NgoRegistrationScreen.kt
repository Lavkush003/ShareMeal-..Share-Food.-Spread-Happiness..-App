package com.sharemeal.app.feature.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.InfoBanner
import com.sharemeal.app.core.ui.components.PasswordTextField
import com.sharemeal.app.core.ui.components.SectionHeader
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealTextField
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.domain.model.UserRole

@Composable
fun NgoRegistrationScreen(
    onRegistered: () -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form = state.ngo

    LaunchedEffect(Unit) {
        viewModel.setRole(UserRole.NGO)
        viewModel.events.collect {
            onRegistered()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.ngo_register_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                Text(
                    text = stringResource(R.string.ngo_register_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                InfoBanner(message = stringResource(R.string.ngo_register_pending_note))
            }

            state.errorMessageRes?.let { messageRes ->
                item {
                    FeedbackBanner(
                        message = stringResource(messageRes),
                        type = FeedbackType.ERROR
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.ngo_register_section_organization)) }
            item {
                ShareMealTextField(
                    value = form.ngoName,
                    onValueChange = { viewModel.onNgoChange(AuthField.NGO_NAME, it) },
                    label = stringResource(R.string.field_ngo_name),
                    leadingIcon = Icons.Rounded.Business,
                    isError = state.ngoFieldErrors.containsKey(AuthField.NGO_NAME),
                    errorMessage = state.ngoFieldErrors[AuthField.NGO_NAME]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.registrationNumber,
                    onValueChange = {
                        viewModel.onNgoChange(AuthField.REGISTRATION_NUMBER, it)
                    },
                    label = stringResource(R.string.field_registration_number_optional),
                    leadingIcon = Icons.Rounded.Description,
                    isError = state.ngoFieldErrors.containsKey(AuthField.REGISTRATION_NUMBER),
                    errorMessage = state.ngoFieldErrors[AuthField.REGISTRATION_NUMBER]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }

            item { SectionHeader(title = stringResource(R.string.ngo_register_section_contact)) }
            item {
                ShareMealTextField(
                    value = form.contactPersonName,
                    onValueChange = { viewModel.onNgoChange(AuthField.CONTACT_PERSON, it) },
                    label = stringResource(R.string.field_contact_person),
                    leadingIcon = Icons.Rounded.Person,
                    isError = state.ngoFieldErrors.containsKey(AuthField.CONTACT_PERSON),
                    errorMessage = state.ngoFieldErrors[AuthField.CONTACT_PERSON]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.email,
                    onValueChange = { viewModel.onNgoChange(AuthField.EMAIL, it) },
                    label = stringResource(R.string.field_email),
                    leadingIcon = Icons.Rounded.Email,
                    isError = state.ngoFieldErrors.containsKey(AuthField.EMAIL),
                    errorMessage = state.ngoFieldErrors[AuthField.EMAIL]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.mobile,
                    onValueChange = { viewModel.onNgoChange(AuthField.MOBILE, it) },
                    label = stringResource(R.string.field_mobile),
                    leadingIcon = Icons.Rounded.Phone,
                    isError = state.ngoFieldErrors.containsKey(AuthField.MOBILE),
                    errorMessage = state.ngoFieldErrors[AuthField.MOBILE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !state.isSubmitting
                )
            }
            item {
                PasswordTextField(
                    value = form.password,
                    onValueChange = { viewModel.onNgoChange(AuthField.PASSWORD, it) },
                    label = stringResource(R.string.field_password),
                    isVisible = state.isNgoPasswordVisible,
                    onToggleVisibility = viewModel::toggleNgoPasswordVisibility,
                    errorMessage = state.ngoFieldErrors[AuthField.PASSWORD]
                        ?.let { stringResource(it) },
                    enabled = !state.isSubmitting
                )
            }
            item {
                PasswordTextField(
                    value = form.confirmPassword,
                    onValueChange = { viewModel.onNgoChange(AuthField.CONFIRM_PASSWORD, it) },
                    label = stringResource(R.string.field_confirm_password),
                    isVisible = state.isNgoConfirmPasswordVisible,
                    onToggleVisibility = viewModel::toggleNgoConfirmPasswordVisibility,
                    errorMessage = state.ngoFieldErrors[AuthField.CONFIRM_PASSWORD]
                        ?.let { stringResource(it) },
                    enabled = !state.isSubmitting,
                    imeAction = ImeAction.Next
                )
            }

            item { SectionHeader(title = stringResource(R.string.ngo_register_section_address)) }
            item {
                ShareMealTextField(
                    value = form.addressLine,
                    onValueChange = { viewModel.onNgoChange(AuthField.ADDRESS_LINE, it) },
                    label = stringResource(R.string.field_address_line),
                    leadingIcon = Icons.Rounded.Place,
                    isError = state.ngoFieldErrors.containsKey(AuthField.ADDRESS_LINE),
                    errorMessage = state.ngoFieldErrors[AuthField.ADDRESS_LINE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.city,
                    onValueChange = { viewModel.onNgoChange(AuthField.CITY, it) },
                    label = stringResource(R.string.field_city),
                    isError = state.ngoFieldErrors.containsKey(AuthField.CITY),
                    errorMessage = state.ngoFieldErrors[AuthField.CITY]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.state,
                    onValueChange = { viewModel.onNgoChange(AuthField.STATE, it) },
                    label = stringResource(R.string.field_state),
                    isError = state.ngoFieldErrors.containsKey(AuthField.STATE),
                    errorMessage = state.ngoFieldErrors[AuthField.STATE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.pinCode,
                    onValueChange = { viewModel.onNgoChange(AuthField.PIN_CODE, it) },
                    label = stringResource(R.string.field_pin_code),
                    isError = state.ngoFieldErrors.containsKey(AuthField.PIN_CODE),
                    errorMessage = state.ngoFieldErrors[AuthField.PIN_CODE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !state.isSubmitting
                )
            }

            item { SectionHeader(title = stringResource(R.string.ngo_register_section_verification)) }
            item {
                ShareMealTextField(
                    value = form.operatingArea,
                    onValueChange = { viewModel.onNgoChange(AuthField.OPERATING_AREA, it) },
                    label = stringResource(R.string.field_operating_area),
                    leadingIcon = Icons.Rounded.Place,
                    isError = state.ngoFieldErrors.containsKey(AuthField.OPERATING_AREA),
                    errorMessage = state.ngoFieldErrors[AuthField.OPERATING_AREA]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.capacity,
                    onValueChange = { viewModel.onNgoChange(AuthField.CAPACITY, it) },
                    label = stringResource(R.string.field_capacity),
                    placeholder = stringResource(R.string.field_capacity_hint),
                    leadingIcon = Icons.Rounded.Restaurant,
                    isError = state.ngoFieldErrors.containsKey(AuthField.CAPACITY),
                    errorMessage = state.ngoFieldErrors[AuthField.CAPACITY]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                Text(
                    text = stringResource(R.string.ngo_register_document_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ShareMealTextField(
                    value = form.documentInfo,
                    onValueChange = { viewModel.onNgoChange(AuthField.DOCUMENT_INFO, it) },
                    label = stringResource(R.string.field_document_info),
                    leadingIcon = Icons.Rounded.Description,
                    isError = state.ngoFieldErrors.containsKey(AuthField.DOCUMENT_INFO),
                    errorMessage = state.ngoFieldErrors[AuthField.DOCUMENT_INFO]
                        ?.let { stringResource(it) },
                    singleLine = false,
                    minLines = 3,
                    enabled = !state.isSubmitting
                )
            }

            item {
                ShareMealButton(
                    text = stringResource(R.string.ngo_register_submit),
                    onClick = viewModel::registerNgo,
                    isLoading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = onLogin,
                        enabled = !state.isSubmitting
                    ) {
                        Text(text = stringResource(R.string.ngo_register_login))
                    }
                }
            }
        }
    }
}
