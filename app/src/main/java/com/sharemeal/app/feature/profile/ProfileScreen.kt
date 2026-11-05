package com.sharemeal.app.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.sharemeal.app.core.ui.components.ErrorState
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.InfoBanner
import com.sharemeal.app.core.ui.components.LoadingState
import com.sharemeal.app.core.ui.components.SectionHeader
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealOutlinedButton
import com.sharemeal.app.core.ui.components.ShareMealTextField
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.ui.components.StatusChip
import com.sharemeal.app.core.ui.labelRes
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.domain.model.DonorCategory
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ProfileEvent.LoggedOut) {
                onLoggedOut()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.profile_screen_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        val user = state.user
        when {
            state.isLoading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = stringResource(R.string.profile_loading)
                )
            }

            user == null -> {
                ErrorState(
                    title = stringResource(R.string.profile_screen_title),
                    message = state.errorMessageRes?.let { stringResource(it) }
                        ?: stringResource(R.string.error_generic),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onRetry = viewModel::loadProfile
                )
            }

            else -> {
                ProfileContent(
                    state = state,
                    user = user,
                    innerPadding = innerPadding,
                    onFieldChange = viewModel::onFieldChange,
                    onDonorCategoryChange = viewModel::onDonorCategoryChange,
                    onStartEditing = viewModel::startEditing,
                    onCancelEditing = viewModel::cancelEditing,
                    onSave = viewModel::saveProfile,
                    onLogout = viewModel::logout
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    user: User,
    innerPadding: PaddingValues,
    onFieldChange: (AuthField, String) -> Unit,
    onDonorCategoryChange: (DonorCategory) -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSave: () -> Unit,
    onLogout: () -> Unit
) {
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
        item { ProfileHeader(user = user) }

        state.errorMessageRes?.let { messageRes ->
            item {
                FeedbackBanner(
                    message = stringResource(messageRes),
                    type = FeedbackType.ERROR
                )
            }
        }
        state.successMessageRes?.let { messageRes ->
            item {
                FeedbackBanner(
                    message = stringResource(messageRes),
                    type = FeedbackType.SUCCESS
                )
            }
        }

        if (state.isEditing) {
            item { SectionHeader(title = stringResource(R.string.profile_account_section)) }
            item {
                ShareMealTextField(
                    value = state.displayName,
                    onValueChange = { onFieldChange(AuthField.FULL_NAME, it) },
                    label = stringResource(
                        if (user.role == UserRole.NGO) {
                            R.string.field_ngo_name
                        } else {
                            R.string.field_full_name
                        }
                    ),
                    isError = state.fieldErrors.containsKey(AuthField.FULL_NAME),
                    errorMessage = state.fieldErrors[AuthField.FULL_NAME]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSaving
                )
            }
            item {
                ShareMealTextField(
                    value = user.email,
                    onValueChange = {},
                    label = stringResource(R.string.field_email),
                    enabled = false
                )
            }
            item {
                Text(
                    text = stringResource(R.string.profile_read_only_email),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ShareMealTextField(
                    value = state.mobile,
                    onValueChange = { onFieldChange(AuthField.MOBILE, it) },
                    label = stringResource(R.string.field_mobile),
                    isError = state.fieldErrors.containsKey(AuthField.MOBILE),
                    errorMessage = state.fieldErrors[AuthField.MOBILE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !state.isSaving
                )
            }

            item { SectionHeader(title = stringResource(R.string.profile_organization_section)) }
            if (user.role == UserRole.DONOR) {
                item {
                    ShareMealTextField(
                        value = state.organizationName,
                        onValueChange = { onFieldChange(AuthField.ORGANIZATION_NAME, it) },
                        label = stringResource(R.string.field_organization_name_optional),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !state.isSaving
                    )
                }
                item {
                    DonorCategorySelector(
                        selected = state.donorCategory,
                        onSelect = onDonorCategoryChange,
                        errorMessage = state.fieldErrors[AuthField.DONOR_CATEGORY]
                            ?.let { stringResource(it) },
                        enabled = !state.isSaving
                    )
                }
            } else {
                item {
                    ShareMealTextField(
                        value = state.contactPersonName,
                        onValueChange = { onFieldChange(AuthField.CONTACT_PERSON, it) },
                        label = stringResource(R.string.profile_contact_person),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !state.isSaving
                    )
                }
                item {
                    ShareMealTextField(
                        value = state.operatingArea,
                        onValueChange = { onFieldChange(AuthField.OPERATING_AREA, it) },
                        label = stringResource(R.string.profile_operating_area),
                        isError = state.fieldErrors.containsKey(AuthField.OPERATING_AREA),
                        errorMessage = state.fieldErrors[AuthField.OPERATING_AREA]
                            ?.let { stringResource(it) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !state.isSaving
                    )
                }
                item {
                    ShareMealTextField(
                        value = state.capacity,
                        onValueChange = { onFieldChange(AuthField.CAPACITY, it) },
                        label = stringResource(R.string.profile_capacity),
                        isError = state.fieldErrors.containsKey(AuthField.CAPACITY),
                        errorMessage = state.fieldErrors[AuthField.CAPACITY]
                            ?.let { stringResource(it) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !state.isSaving
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.profile_address_section)) }
            item {
                ShareMealTextField(
                    value = state.addressLine,
                    onValueChange = { onFieldChange(AuthField.ADDRESS_LINE, it) },
                    label = stringResource(R.string.field_address_line),
                    isError = state.fieldErrors.containsKey(AuthField.ADDRESS_LINE),
                    errorMessage = state.fieldErrors[AuthField.ADDRESS_LINE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSaving
                )
            }
            item {
                ShareMealTextField(
                    value = state.city,
                    onValueChange = { onFieldChange(AuthField.CITY, it) },
                    label = stringResource(R.string.field_city),
                    isError = state.fieldErrors.containsKey(AuthField.CITY),
                    errorMessage = state.fieldErrors[AuthField.CITY]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSaving
                )
            }
            item {
                ShareMealTextField(
                    value = state.state,
                    onValueChange = { onFieldChange(AuthField.STATE, it) },
                    label = stringResource(R.string.field_state),
                    isError = state.fieldErrors.containsKey(AuthField.STATE),
                    errorMessage = state.fieldErrors[AuthField.STATE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isSaving
                )
            }
            item {
                ShareMealTextField(
                    value = state.pinCode,
                    onValueChange = { onFieldChange(AuthField.PIN_CODE, it) },
                    label = stringResource(R.string.field_pin_code),
                    isError = state.fieldErrors.containsKey(AuthField.PIN_CODE),
                    errorMessage = state.fieldErrors[AuthField.PIN_CODE]
                        ?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !state.isSaving
                )
            }

            item {
                ShareMealButton(
                    text = stringResource(R.string.action_save),
                    onClick = onSave,
                    isLoading = state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                TextButton(
                    onClick = onCancelEditing,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        } else {
            item { SectionHeader(title = stringResource(R.string.profile_account_section)) }
            item {
                ProfileCard {
                    ProfileDetailRow(
                        label = stringResource(R.string.field_display_name),
                        value = user.displayName
                    )
                    ProfileDetailRow(
                        label = stringResource(R.string.field_email),
                        value = user.email
                    )
                    ProfileDetailRow(
                        label = stringResource(R.string.field_mobile),
                        value = user.mobile
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.profile_organization_section)) }
            item {
                ProfileCard {
                    if (user.role == UserRole.DONOR) {
                        val donorProfile = user.donorProfile
                        ProfileDetailRow(
                            label = stringResource(R.string.field_organization_name),
                            value = donorProfile?.organizationName.orEmpty()
                        )
                        donorProfile?.category?.let { category ->
                            ProfileDetailRow(
                                label = stringResource(R.string.profile_category),
                                value = stringResource(category.labelRes())
                            )
                        }
                    } else {
                        val ngoProfile = user.ngoProfile
                        ProfileDetailRow(
                            label = stringResource(R.string.profile_contact_person),
                            value = ngoProfile?.contactPersonName.orEmpty()
                        )
                        ProfileDetailRow(
                            label = stringResource(R.string.profile_operating_area),
                            value = ngoProfile?.operatingArea.orEmpty()
                        )
                        ProfileDetailRow(
                            label = stringResource(R.string.profile_capacity),
                            value = ngoProfile?.estimatedDistributionCapacity.orEmpty()
                        )
                    }
                }
            }

            item { SectionHeader(title = stringResource(R.string.profile_address_section)) }
            item {
                ProfileCard {
                    ProfileDetailRow(
                        label = stringResource(R.string.field_address_line),
                        value = user.address?.line1.orEmpty()
                    )
                    ProfileDetailRow(
                        label = stringResource(R.string.field_city),
                        value = user.address?.city.orEmpty()
                    )
                    ProfileDetailRow(
                        label = stringResource(R.string.field_state),
                        value = user.address?.state.orEmpty()
                    )
                    ProfileDetailRow(
                        label = stringResource(R.string.field_pin_code),
                        value = user.address?.pinCode.orEmpty()
                    )
                }
            }

            user.ngoProfile?.let { ngoProfile ->
                item { SectionHeader(title = stringResource(R.string.profile_verification_section)) }
                item {
                    ProfileCard {
                        VerificationRow(status = ngoProfile.verificationStatus)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        ProfileDetailRow(
                            label = stringResource(R.string.profile_verification_document),
                            value = ngoProfile.verificationDocumentInfo
                        )
                        if (ngoProfile.verificationStatus == NgoVerificationStatus.PENDING) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            InfoBanner(
                                message = stringResource(R.string.profile_verification_pending_note)
                            )
                        }
                    }
                }
            }

            item {
                ShareMealButton(
                    text = stringResource(R.string.action_edit),
                    onClick = onStartEditing,
                    icon = Icons.Rounded.Edit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ShareMealOutlinedButton(
                    text = stringResource(R.string.action_logout),
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val initial = user.displayName.firstOrNull()?.uppercaseChar()?.toString()
                    if (initial.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                StatusChip(
                    label = stringResource(
                        if (user.role == UserRole.NGO) {
                            R.string.role_ngo_title
                        } else {
                            R.string.role_donor_title
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            content()
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { stringResource(R.string.value_not_provided) },
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
    }
}

@Composable
private fun VerificationRow(status: NgoVerificationStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.profile_verification_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        val containerColor = when (status) {
            NgoVerificationStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
            NgoVerificationStatus.VERIFIED -> MaterialTheme.colorScheme.secondaryContainer
            NgoVerificationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
        }
        val contentColor = when (status) {
            NgoVerificationStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
            NgoVerificationStatus.VERIFIED -> MaterialTheme.colorScheme.onSecondaryContainer
            NgoVerificationStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
        }
        StatusChip(
            label = stringResource(status.labelRes()),
            containerColor = containerColor,
            contentColor = contentColor
        )
    }
}

@StringRes
private fun NgoVerificationStatus.labelRes(): Int = when (this) {
    NgoVerificationStatus.PENDING -> R.string.verification_pending
    NgoVerificationStatus.VERIFIED -> R.string.verification_verified
    NgoVerificationStatus.REJECTED -> R.string.verification_rejected
}
