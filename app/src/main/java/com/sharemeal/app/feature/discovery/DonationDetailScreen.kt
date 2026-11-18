package com.sharemeal.app.feature.discovery

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FmdGood
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.ErrorState
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.InfoBanner
import com.sharemeal.app.core.ui.components.LoadingState
import com.sharemeal.app.core.ui.components.SectionHeader
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.ui.components.StatusChip
import com.sharemeal.app.core.ui.labelRes
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.NgoVerificationStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun DonationDetailScreen(
    onBack: () -> Unit,
    onViewRequests: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DonationDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRequestDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.donation_detail_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        val item = state.item

        when {
            state.isLoading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = "Loading donation details…"
                )
            }

            item == null -> {
                ErrorState(
                    title = stringResource(R.string.donation_detail_title),
                    message = state.errorText
                        ?: state.errorMessageRes?.let { stringResource(it) }
                        ?: stringResource(R.string.error_generic),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onRetry = viewModel::loadDonationDetails
                )
            }

            else -> {
                DonationDetailContent(
                    item = item,
                    state = state,
                    innerPadding = innerPadding,
                    onRequestClick = { showRequestDialog = true },
                    onViewRequests = onViewRequests
                )
            }
        }
    }

    if (showRequestDialog && state.item != null) {
        RequestDonationDialog(
            maxQuantity = state.item!!.donation.remainingQuantity,
            quantityUnit = state.item!!.donation.quantityUnit.name.lowercase(),
            pickupDeadline = state.item!!.donation.pickupDeadline,
            isSubmitting = state.isSubmittingRequest,
            onConfirm = { qty, people, pickupAt, notes ->
                viewModel.submitRequest(qty, people, pickupAt, notes)
                showRequestDialog = false
            },
            onDismiss = { showRequestDialog = false }
        )
    }
}

@Composable
private fun DonationDetailContent(
    item: FoodDiscoveryItem,
    state: DonationDetailUiState,
    innerPadding: PaddingValues,
    onRequestClick: () -> Unit,
    onViewRequests: () -> Unit
) {
    val donation = item.donation
    val locale = java.util.Locale.Builder()
            .setLanguage(ComposeLocale.current.language)
            .setRegion(ComposeLocale.current.region)
            .build()
    val timeFormat = remember(locale) { SimpleDateFormat("h:mm a, d MMM", locale) }
    val preparedAtStr = timeFormat.format(Date(donation.preparedAt))
    val pickupStartStr = timeFormat.format(Date(donation.pickupWindowStart))
    val deadlineStr = timeFormat.format(Date(donation.pickupDeadline))

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
        state.errorText?.let { errorMsg ->
            item {
                FeedbackBanner(
                    message = errorMsg,
                    type = FeedbackType.ERROR
                )
            }
        }

        if (state.requestSubmitted) {
            item {
                FeedbackBanner(
                    message = stringResource(R.string.donation_detail_request_success),
                    type = FeedbackType.SUCCESS
                )
            }
            item {
                ShareMealButton(
                    text = "View My Requests",
                    onClick = onViewRequests,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Header Hero Image Placeholder
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = donation.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Classification & Status Badges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(
                    label = when (donation.classification) {
                        FoodClassification.VEGETARIAN -> "Vegetarian"
                        FoodClassification.NON_VEGETARIAN -> "Non-Vegetarian"
                        FoodClassification.VEGAN -> "Vegan"
                        FoodClassification.OTHER -> "Other"
                    },
                    containerColor = when (donation.classification) {
                        FoodClassification.VEGETARIAN -> MaterialTheme.colorScheme.secondaryContainer
                        FoodClassification.NON_VEGETARIAN -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                StatusChip(
                    label = stringResource(donation.category.labelRes()),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                item.distanceKm?.let { dist ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FmdGood,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = stringResource(R.string.discovery_distance_format, dist),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Description
        item {
            Text(
                text = donation.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Available Quantity Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = stringResource(
                                R.string.donation_detail_available_quantity,
                                donation.remainingQuantity,
                                donation.quantityUnit.name.lowercase()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${donation.remainingQuantity} ${donation.quantityUnit.name.lowercase()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Preparation & Pickup Schedule
        item { SectionHeader(title = "Schedule & Timings") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    DetailRow(
                        icon = Icons.Rounded.AccessTime,
                        label = "Prepared At",
                        value = preparedAtStr
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    DetailRow(
                        icon = Icons.Rounded.Schedule,
                        label = "Pickup Window Start",
                        value = pickupStartStr
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    DetailRow(
                        icon = Icons.Rounded.Schedule,
                        label = "Pickup Deadline",
                        value = deadlineStr
                    )
                }
            }
        }

        // Storage & Packaging
        item { SectionHeader(title = "Storage & Packaging") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    DetailRow(
                        icon = Icons.Rounded.Inventory2,
                        label = "Storage Conditions",
                        value = donation.storageConditions
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    DetailRow(
                        icon = Icons.Rounded.Inventory2,
                        label = "Packaging Details",
                        value = donation.packaging
                    )
                }
            }
        }

        // Food Safety & Allergens
        item { SectionHeader(title = "Food Safety & Declarations") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Food Condition Declared Safe",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Donor has certified that this food was prepared under hygienic conditions and stored safely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    DetailRow(
                        icon = Icons.Rounded.Security,
                        label = "Allergen Information",
                        value = donation.allergenInformation.ifBlank { "None reported by donor." }
                    )
                }
            }
        }

        // Donor & Pickup Address (Privacy Restricted)
        item { SectionHeader(title = "Donor & Location") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    DetailRow(
                        icon = Icons.Rounded.Business,
                        label = "Donor",
                        value = item.donorName
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    DetailRow(
                        icon = Icons.Rounded.FmdGood,
                        label = "General Area",
                        value = "${donation.city}, ${donation.state}"
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    InfoBanner(
                        message = stringResource(R.string.donation_detail_privacy_note)
                    )
                }
            }
        }

        // NGO Eligibility & Request Action Button
        item {
            if (state.isVerified) {
                ShareMealButton(
                    text = stringResource(R.string.donation_detail_request_action),
                    onClick = onRequestClick,
                    enabled = !state.requestSubmitted,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "Verification Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Only verified NGOs can submit donation requests. Your NGO profile verification is currently pending review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RequestDonationDialog(
    maxQuantity: Int,
    quantityUnit: String,
    pickupDeadline: Long,
    isSubmitting: Boolean,
    onConfirm: (Int, Int, Long, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf(maxQuantity.toString()) }
    var peopleText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var peopleError by remember { mutableStateOf<String?>(null) }
    var expectedPickupAt by remember { mutableLongStateOf(pickupDeadline) }
    val context = LocalContext.current
    val formatter = remember {
        SimpleDateFormat(
            "d MMM yyyy, h:mm a",
            java.util.Locale.Builder()
                .setLanguage(ComposeLocale.current.language)
                .setRegion(ComposeLocale.current.region)
                .build()
        )
    }
    val quantityErrorMessage = stringResource(R.string.donation_detail_request_error_quantity, maxQuantity)
    val peopleErrorMessage = stringResource(R.string.donation_detail_request_error_people)
    val pickupErrorMessage = stringResource(R.string.donation_detail_request_error_pickup)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.donation_detail_request_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.donation_detail_request_dialog_body),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter(Char::isDigit)
                        quantityError = null
                    },
                    label = { Text(text = stringResource(R.string.donation_detail_request_quantity)) },
                    isError = quantityError != null,
                    supportingText = quantityError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = peopleText,
                    onValueChange = {
                        peopleText = it.filter(Char::isDigit)
                        peopleError = null
                    },
                    label = { Text(text = stringResource(R.string.donation_detail_request_people)) },
                    isError = peopleError != null,
                    supportingText = peopleError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ShareMealButton(
                    text = stringResource(R.string.donation_detail_request_pickup, formatter.format(Date(expectedPickupAt))),
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = expectedPickupAt }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        expectedPickupAt = Calendar.getInstance().apply {
                                            set(year, month, day, hour, minute)
                                        }.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text(text = stringResource(R.string.donation_detail_request_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            ShareMealButton(
                text = stringResource(R.string.donation_detail_request_submit),
                isLoading = isSubmitting,
                onClick = {
                    val qty = quantityText.toIntOrNull()
                    val people = peopleText.toIntOrNull()
                    when {
                        qty == null || qty <= 0 || qty > maxQuantity -> {
                            quantityError = quantityErrorMessage
                        }
                        people == null || people <= 0 -> {
                            peopleError = peopleErrorMessage
                        }
                        expectedPickupAt <= System.currentTimeMillis() -> {
                            peopleError = pickupErrorMessage
                        }
                        else -> onConfirm(qty, people, expectedPickupAt, notesText.trim().ifEmpty { null })
                    }
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}
