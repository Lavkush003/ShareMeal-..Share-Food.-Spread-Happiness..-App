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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
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
import com.sharemeal.app.core.ui.components.DonorCategorySelector
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.PasswordTextField
import com.sharemeal.app.core.ui.components.SectionHeader
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealTextField
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.domain.model.UserRole

@Composable
fun DonorRegistrationScreen(
    onRegistered: () -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form = state.donor

    LaunchedEffect(Unit) {
        viewModel.setRole(UserRole.DONOR)
        viewModel.events.collect {
            onRegistered()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.donor_register_title),
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
                    text = stringResource(R.string.donor_register_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.errorMessageRes?.let { messageRes ->
                item {
                    FeedbackBanner(
                        message = stringResource(messageRes),
                        type = FeedbackType.ERROR
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.donor_register_section_personal)) }
            item {
                ShareMealTextField(
                    value = form.fullName,
                    onValueChange = { viewModel.onDonorChange(AuthField.FULL_NAME, it) },
                    label = stringResource(R.string.field_full_name),
                    leadingIcon = Icons.Rounded.Person,
                    isError = state.donorFieldErrors.containsKey(AuthField.FULL_NAME),
                    errorMessage = state.donorFieldErrors[AuthField.FULL_NAME]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.organizationName,
                    onValueChange = { viewModel.onDonorChange(AuthField.ORGANIZATION_NAME, it) },
                    label = stringResource(R.string.field_organization_name_optional),
                    leadingIcon = Icons.Rounded.Business,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.email,
                    onValueChange = { viewModel.onDonorChange(AuthField.EMAIL, it) },
                    label = stringResource(R.string.field_email),
                    leadingIcon = Icons.Rounded.Email,
                    isError = state.donorFieldErrors.containsKey(AuthField.EMAIL),
                    errorMessage = state.donorFieldErrors[AuthField.EMAIL]
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
                    onValueChange = { viewModel.onDonorChange(AuthField.MOBILE, it) },
                    label = stringResource(R.string.field_mobile),
                    leadingIcon = Icons.Rounded.Phone,
                    isError = state.donorFieldErrors.containsKey(AuthField.MOBILE),
                    errorMessage = state.donorFieldErrors[AuthField.MOBILE]
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
                    onValueChange = { viewModel.onDonorChange(AuthField.PASSWORD, it) },
                    label = stringResource(R.string.field_password),
                    isVisible = state.isDonorPasswordVisible,
                    onToggleVisibility = viewModel::toggleDonorPasswordVisibility,
                    errorMessage = state.donorFieldErrors[AuthField.PASSWORD]
                        ?.let { stringResource(it) },
                    enabled = !state.isSubmitting
                )
            }
            item {
                PasswordTextField(
                    value = form.confirmPassword,
                    onValueChange = { viewModel.onDonorChange(AuthField.CONFIRM_PASSWORD, it) },
                    label = stringResource(R.string.field_confirm_password),
                    isVisible = state.isDonorConfirmPasswordVisible,
                    onToggleVisibility = viewModel::toggleDonorConfirmPasswordVisibility,
                    errorMessage = state.donorFieldErrors[AuthField.CONFIRM_PASSWORD]
                        ?.let { stringResource(it) },
                    enabled = !state.isSubmitting,
                    imeAction = ImeAction.Next
                )
            }

            item { SectionHeader(title = stringResource(R.string.donor_register_section_business)) }
            item {
                DonorCategorySelector(
                    selected = form.category,
                    onSelect = viewModel::onDonorCategoryChange,
                    errorMessage = state.donorFieldErrors[AuthField.DONOR_CATEGORY]
                        ?.let { stringResource(it) },
                    enabled = !state.isSubmitting
                )
            }

            item { SectionHeader(title = stringResource(R.string.donor_register_section_address)) }
            item {
                ShareMealTextField(
                    value = form.addressLine,
                    onValueChange = { viewModel.onDonorChange(AuthField.ADDRESS_LINE, it) },
                    label = stringResource(R.string.field_address_line),
                    leadingIcon = Icons.Rounded.Place,
                    isError = state.donorFieldErrors.containsKey(AuthField.ADDRESS_LINE),
                    errorMessage = state.donorFieldErrors[AuthField.ADDRESS_LINE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.city,
                    onValueChange = { viewModel.onDonorChange(AuthField.CITY, it) },
                    label = stringResource(R.string.field_city),
                    isError = state.donorFieldErrors.containsKey(AuthField.CITY),
                    errorMessage = state.donorFieldErrors[AuthField.CITY]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.state,
                    onValueChange = { viewModel.onDonorChange(AuthField.STATE, it) },
                    label = stringResource(R.string.field_state),
                    isError = state.donorFieldErrors.containsKey(AuthField.STATE),
                    errorMessage = state.donorFieldErrors[AuthField.STATE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealTextField(
                    value = form.pinCode,
                    onValueChange = { viewModel.onDonorChange(AuthField.PIN_CODE, it) },
                    label = stringResource(R.string.field_pin_code),
                    isError = state.donorFieldErrors.containsKey(AuthField.PIN_CODE),
                    errorMessage = state.donorFieldErrors[AuthField.PIN_CODE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !state.isSubmitting
                )
            }
            item {
                ShareMealButton(
                    text = stringResource(R.string.donor_register_submit),
                    onClick = viewModel::registerDonor,
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
                        Text(text = stringResource(R.string.donor_register_login))
                    }
                }
            }
        }
    }
}
