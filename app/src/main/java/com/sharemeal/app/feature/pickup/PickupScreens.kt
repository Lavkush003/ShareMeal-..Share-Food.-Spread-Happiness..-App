package com.sharemeal.app.feature.pickup

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.sharemeal.app.core.ui.components.EmptyState
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.InfoBanner
import com.sharemeal.app.core.ui.components.LoadingState
import com.sharemeal.app.core.ui.components.SectionHeader
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealTextField
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.ui.components.StatusChip
import com.sharemeal.app.domain.model.DistributionInput
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupTimelineEntry
import com.sharemeal.app.domain.model.PickupWorkflowStatus
import com.sharemeal.app.domain.model.WorkflowStage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun PickupListScreen(
    mode: PickupListMode,
    onBack: () -> Unit,
    onPickupSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PickupListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val titleRes = when (mode) {
        PickupListMode.UPCOMING -> R.string.pickup_upcoming_title
        PickupListMode.HISTORY -> R.string.pickup_history_title
        PickupListMode.COMPLETED -> R.string.pickup_completed_title
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(title = stringResource(titleRes), onBack = onBack)
        }
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

            state.items.isEmpty() -> EmptyState(
                title = stringResource(R.string.pickup_empty_title),
                message = stringResource(R.string.pickup_empty_body),
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(items = state.items, key = { it.request.id }) { item ->
                    PickupListCard(
                        item = item,
                        onClick = { onPickupSelected(item.request.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickupListCard(
    item: com.sharemeal.app.feature.pickup.PickupListItem,
    onClick: () -> Unit
) {
    val formatter = rememberPickupFormatter()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.request.donationTitle.orEmpty()
                        .ifBlank { stringResource(R.string.request_untitled_donation) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(label = pickupStatusLabel(item.pickup.status))
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(
                    R.string.pickup_card_subtitle,
                    item.request.ngoName,
                    item.request.id
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.pickup_card_time,
                    formatter.format(Date(item.pickup.schedule.scheduledAt))
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PickupDetailScreen(
    onBack: () -> Unit,
    onAction: (PickupActionType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PickupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.pickup_detail_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

            state.request == null || state.pickup == null -> EmptyState(
                title = stringResource(R.string.pickup_empty_title),
                message = state.errorText ?: stringResource(R.string.pickup_empty_body),
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

            else -> PickupDetailContent(
                state = state,
                innerPadding = innerPadding,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun PickupDetailContent(
    state: PickupDetailUiState,
    innerPadding: PaddingValues,
    onAction: (PickupActionType) -> Unit
) {
    val request = state.request ?: return
    val pickup = state.pickup ?: return
    val formatter = rememberPickupFormatter()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.md,
            bottom = Spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = request.donationTitle.orEmpty()
                                .ifBlank { stringResource(R.string.request_untitled_donation) },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusChip(label = pickupStatusLabel(pickup.status))
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = stringResource(R.string.request_detail_reference, request.id),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            InfoBanner(message = stringResource(R.string.pickup_collection_notice))
        }

        item { SectionHeader(title = stringResource(R.string.pickup_section_schedule)) }
        item {
            DetailCard {
                PickupRow(
                    stringResource(R.string.pickup_field_scheduled_at),
                    formatter.format(Date(pickup.schedule.scheduledAt))
                )
                PickupRow(
                    stringResource(R.string.pickup_field_contact),
                    pickup.schedule.contactPerson
                )
                PickupRow(
                    stringResource(R.string.pickup_field_instructions),
                    pickup.schedule.collectionInstructions
                )
                PickupRow(
                    stringResource(R.string.pickup_field_volunteer),
                    pickup.schedule.volunteerName
                )
                if (pickup.schedule.volunteerPhone.isNotBlank()) {
                    PickupRow(
                        stringResource(R.string.pickup_field_volunteer_phone),
                        pickup.schedule.volunteerPhone
                    )
                }
                PickupRow(
                    stringResource(R.string.pickup_field_expected_quantity),
                    pickup.schedule.expectedQuantity.toString()
                )
                state.donation?.let { donation ->
                    PickupRow(
                        stringResource(R.string.request_detail_address),
                        donation.pickupAddress
                    )
                    PickupRow(
                        stringResource(R.string.request_detail_phone),
                        donation.contactPhone
                    )
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.pickup_section_timeline)) }
        item {
            DetailCard {
                state.timeline.forEach { entry ->
                    PickupTimelineRow(entry = entry, formatter = formatter)
                }
            }
        }

        if (pickup.actualCollectedQuantity != null || state.distribution != null) {
            item { SectionHeader(title = stringResource(R.string.pickup_section_evidence)) }
            item {
                DetailCard {
                    pickup.actualCollectedQuantity?.let {
                        PickupRow(stringResource(R.string.pickup_field_actual_quantity), it.toString())
                    }
                    pickup.quantityDiscrepancy?.let {
                        PickupRow(stringResource(R.string.pickup_field_discrepancy), it.toString())
                    }
                    pickup.discrepancyReason?.takeIf { it.isNotBlank() }?.let {
                        PickupRow(stringResource(R.string.pickup_field_discrepancy_reason), it)
                    }
                    state.distribution?.let { distribution ->
                        PickupRow(
                            stringResource(R.string.pickup_field_received_quantity),
                            distribution.actualReceivedQuantity.toString()
                        )
                        PickupRow(
                            stringResource(R.string.pickup_field_distributed_quantity),
                            distribution.actualDistributedQuantity.toString()
                        )
                        PickupRow(
                            stringResource(R.string.pickup_field_beneficiaries),
                            distribution.beneficiariesServed.toString()
                        )
                        PickupRow(
                            stringResource(R.string.pickup_field_location),
                            distribution.location
                        )
                        PickupRow(
                            stringResource(R.string.pickup_field_remaining_quantity),
                            distribution.remainingQuantity.toString()
                        )
                    }
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.pickup_section_actions)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                when (pickup.status) {
                    PickupWorkflowStatus.SCHEDULED -> {
                        ShareMealButton(
                            text = stringResource(R.string.pickup_action_schedule),
                            icon = Icons.Rounded.LocalShipping,
                            onClick = { onAction(PickupActionType.SCHEDULE) }
                        )
                        if (!state.isDonor) {
                            ShareMealButton(
                                text = stringResource(R.string.pickup_action_ngo_ready),
                                onClick = { onAction(PickupActionType.NGO_READY) }
                            )
                        }
                    }
                    PickupWorkflowStatus.NGO_READY -> {
                        if (state.isDonor) {
                            ShareMealButton(
                                text = stringResource(R.string.pickup_action_donor_ready),
                                onClick = { onAction(PickupActionType.DONOR_READY) }
                            )
                        }
                    }
                    PickupWorkflowStatus.DONOR_READY -> {
                        if (!state.isDonor) {
                            ShareMealButton(
                                text = stringResource(R.string.pickup_action_collect),
                                icon = Icons.Rounded.LocalShipping,
                                onClick = { onAction(PickupActionType.COLLECTION) }
                            )
                        }
                    }
                    PickupWorkflowStatus.COLLECTED -> {
                        if (!state.isDonor) {
                            ShareMealButton(
                                text = stringResource(R.string.pickup_action_distribute),
                                onClick = { onAction(PickupActionType.DISTRIBUTION) }
                            )
                        }
                    }
                    PickupWorkflowStatus.DISTRIBUTED -> {
                        if (state.isDonor) {
                            ShareMealButton(
                                text = stringResource(R.string.pickup_action_complete),
                                icon = Icons.Rounded.CheckCircle,
                                onClick = { onAction(PickupActionType.COMPLETE) }
                            )
                        }
                    }
                    else -> Unit
                }
                if (pickup.status.isActive) {
                    ShareMealButton(
                        text = stringResource(R.string.pickup_action_exception),
                        onClick = { onAction(PickupActionType.EXCEPTION) }
                    )
                }
            }
        }

        if (state.events.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.pickup_section_timeline)) }
            items(items = state.events, key = { it.id }) { event ->
                DetailCard {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    event.note?.takeIf { it.isNotBlank() }?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = formatter.format(Date(event.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PickupActionScreen(
    action: PickupActionType,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PickupActionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val titleRes = when (action) {
        PickupActionType.SCHEDULE -> R.string.pickup_action_schedule
        PickupActionType.NGO_READY -> R.string.pickup_action_ngo_ready
        PickupActionType.DONOR_READY -> R.string.pickup_action_donor_ready
        PickupActionType.COLLECTION -> R.string.pickup_action_collect
        PickupActionType.DISTRIBUTION -> R.string.pickup_action_distribute
        PickupActionType.COMPLETE -> R.string.pickup_action_complete
        PickupActionType.EXCEPTION -> R.string.pickup_action_exception
    }

    LaunchedEffect(state.isDone) {
        if (state.isDone) onDone()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(title = stringResource(titleRes), onBack = onBack)
        }
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                state.errorText?.let {
                    item { FeedbackBanner(message = it, type = FeedbackType.ERROR) }
                }
                state.successText?.let {
                    item { FeedbackBanner(message = it, type = FeedbackType.SUCCESS) }
                }

                when (action) {
                    PickupActionType.SCHEDULE -> item {
                        SchedulePickupForm(state = state, viewModel = viewModel)
                    }
                    PickupActionType.NGO_READY -> item {
                        NgoReadyForm(state = state, viewModel = viewModel)
                    }
                    PickupActionType.DONOR_READY -> item {
                        DonorReadyForm(state = state, viewModel = viewModel)
                    }
                    PickupActionType.COLLECTION -> item {
                        CollectionForm(state = state, viewModel = viewModel)
                    }
                    PickupActionType.DISTRIBUTION -> item {
                        DistributionForm(state = state, viewModel = viewModel)
                    }
                    PickupActionType.COMPLETE -> item {
                        CompleteForm(state = state, viewModel = viewModel)
                    }
                    PickupActionType.EXCEPTION -> item {
                        ExceptionForm(state = state, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulePickupForm(
    state: PickupActionUiState,
    viewModel: PickupActionViewModel
) {
    var scheduledAt by remember(state.pickup) {
        mutableLongStateOf(state.pickup?.schedule?.scheduledAt ?: System.currentTimeMillis() + 3_600_000L)
    }
    var contact by remember(state.pickup) {
        mutableStateOf(state.pickup?.schedule?.contactPerson.orEmpty())
    }
    var instructions by remember(state.pickup) {
        mutableStateOf(state.pickup?.schedule?.collectionInstructions.orEmpty())
    }
    var volunteer by remember(state.pickup) {
        mutableStateOf(state.pickup?.schedule?.volunteerName.orEmpty())
    }
    var volunteerPhone by remember(state.pickup) {
        mutableStateOf(state.pickup?.schedule?.volunteerPhone.orEmpty())
    }
    var expected by remember(state.pickup, state.request) {
        mutableIntStateOf(
            state.pickup?.schedule?.expectedQuantity
                ?: state.request?.approvedQuantity
                ?: state.request?.requestedQuantity
                ?: 0
        )
    }
    var expectedText by remember(expected) { mutableStateOf(expected.toString()) }

    FormCard {
        Text(
            text = stringResource(R.string.pickup_schedule_dialog_body),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        DateTimePickerButton(
            label = stringResource(R.string.pickup_field_scheduled_at),
            timestamp = scheduledAt,
            onTimestampChange = { scheduledAt = it }
        )
        ShareMealTextField(
            value = contact,
            onValueChange = { contact = it },
            label = stringResource(R.string.pickup_field_contact)
        )
        ShareMealTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = stringResource(R.string.pickup_field_instructions),
            singleLine = false,
            minLines = 2
        )
        ShareMealTextField(
            value = volunteer,
            onValueChange = { volunteer = it },
            label = stringResource(R.string.pickup_field_volunteer)
        )
        ShareMealTextField(
            value = volunteerPhone,
            onValueChange = { volunteerPhone = it },
            label = stringResource(R.string.pickup_field_volunteer_phone),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Phone
            )
        )
        ShareMealTextField(
            value = expectedText,
            onValueChange = { value ->
                expectedText = value.filter(Char::isDigit)
                expected = expectedText.toIntOrNull() ?: 0
            },
            label = stringResource(R.string.pickup_field_expected_quantity),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealButton(
            text = stringResource(R.string.pickup_action_submit),
            isLoading = state.isLoading,
            onClick = {
                viewModel.schedule(
                    scheduledAt = scheduledAt,
                    contactPerson = contact,
                    collectionInstructions = instructions,
                    volunteerName = volunteer,
                    volunteerPhone = volunteerPhone,
                    expectedQuantity = expected
                )
            }
        )
    }
}

@Composable
private fun NgoReadyForm(
    state: PickupActionUiState,
    viewModel: PickupActionViewModel
) {
    var volunteer by remember(state.pickup) {
        mutableStateOf(state.pickup?.schedule?.volunteerName.orEmpty())
    }
    var phone by remember(state.pickup) {
        mutableStateOf(state.pickup?.schedule?.volunteerPhone.orEmpty())
    }
    var note by remember { mutableStateOf("") }

    FormCard {
        ShareMealTextField(
            value = volunteer,
            onValueChange = { volunteer = it },
            label = stringResource(R.string.pickup_field_volunteer)
        )
        ShareMealTextField(
            value = phone,
            onValueChange = { phone = it },
            label = stringResource(R.string.pickup_field_volunteer_phone),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Phone
            )
        )
        ShareMealTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(R.string.pickup_field_note),
            singleLine = false,
            minLines = 2
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealButton(
            text = stringResource(R.string.pickup_action_submit),
            isLoading = state.isLoading,
            onClick = { viewModel.confirmNgoReady(volunteer, phone, note.ifBlank { null }) }
        )
    }
}

@Composable
private fun DonorReadyForm(
    state: PickupActionUiState,
    viewModel: PickupActionViewModel
) {
    var contact by remember(state.pickup) {
        mutableStateOf(state.pickup?.schedule?.contactPerson.orEmpty())
    }
    var instructions by remember(state.pickup) {
        mutableStateOf(state.pickup?.schedule?.collectionInstructions.orEmpty())
    }
    var note by remember { mutableStateOf("") }

    FormCard {
        ShareMealTextField(
            value = contact,
            onValueChange = { contact = it },
            label = stringResource(R.string.pickup_field_contact)
        )
        ShareMealTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = stringResource(R.string.pickup_field_instructions),
            singleLine = false,
            minLines = 2
        )
        ShareMealTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(R.string.pickup_field_note),
            singleLine = false,
            minLines = 2
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealButton(
            text = stringResource(R.string.pickup_action_submit),
            isLoading = state.isLoading,
            onClick = { viewModel.confirmDonorReady(contact, instructions, note.ifBlank { null }) }
        )
    }
}

@Composable
private fun CollectionForm(
    state: PickupActionUiState,
    viewModel: PickupActionViewModel
) {
    var actualText by remember(state.pickup, state.request) {
        mutableStateOf(
            (state.pickup?.schedule?.expectedQuantity
                ?: state.request?.approvedQuantity
                ?: state.request?.requestedQuantity
                ?: 0).toString()
        )
    }
    var discrepancyReason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val actual = actualText.toIntOrNull() ?: 0

    FormCard {
        InfoBanner(message = stringResource(R.string.pickup_collection_notice))
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealTextField(
            value = actualText,
            onValueChange = { actualText = it.filter(Char::isDigit) },
            label = stringResource(R.string.pickup_field_actual_quantity),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        ShareMealTextField(
            value = discrepancyReason,
            onValueChange = { discrepancyReason = it },
            label = stringResource(R.string.pickup_field_discrepancy_reason),
            singleLine = false,
            minLines = 2
        )
        ShareMealTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(R.string.pickup_field_note),
            singleLine = false,
            minLines = 2
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealButton(
            text = stringResource(R.string.pickup_action_submit),
            isLoading = state.isLoading,
            onClick = {
                viewModel.confirmCollection(
                    actualCollectedQuantity = actual,
                    discrepancyReason = discrepancyReason.ifBlank { null },
                    note = note.ifBlank { null }
                )
            }
        )
    }
}

@Composable
private fun DistributionForm(
    state: PickupActionUiState,
    viewModel: PickupActionViewModel
) {
    var receivedText by remember(state.pickup) {
        mutableStateOf((state.pickup?.actualCollectedQuantity ?: 0).toString())
    }
    var distributedText by remember(receivedText) { mutableStateOf(receivedText) }
    var beneficiariesText by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var distributedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val received = receivedText.toIntOrNull() ?: 0
    val distributed = distributedText.toIntOrNull() ?: 0
    val beneficiaries = beneficiariesText.toIntOrNull() ?: 0
    val remaining = (received - distributed).coerceAtLeast(0)

    FormCard {
        InfoBanner(message = stringResource(R.string.pickup_distribution_notice))
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealTextField(
            value = receivedText,
            onValueChange = { receivedText = it.filter(Char::isDigit) },
            label = stringResource(R.string.pickup_field_received_quantity),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        ShareMealTextField(
            value = distributedText,
            onValueChange = { distributedText = it.filter(Char::isDigit) },
            label = stringResource(R.string.pickup_field_distributed_quantity),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        ShareMealTextField(
            value = beneficiariesText,
            onValueChange = { beneficiariesText = it.filter(Char::isDigit) },
            label = stringResource(R.string.pickup_field_beneficiaries),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        ShareMealTextField(
            value = location,
            onValueChange = { location = it },
            label = stringResource(R.string.pickup_field_location)
        )
        DateTimePickerButton(
            label = stringResource(R.string.request_detail_completed),
            timestamp = distributedAt,
            onTimestampChange = { distributedAt = it }
        )
        PickupRow(
            label = stringResource(R.string.pickup_field_remaining_quantity),
            value = remaining.toString()
        )
        ShareMealTextField(
            value = notes,
            onValueChange = { notes = it },
            label = stringResource(R.string.pickup_field_notes),
            singleLine = false,
            minLines = 2
        )
        ShareMealTextField(
            value = evidence,
            onValueChange = { evidence = it },
            label = stringResource(R.string.pickup_field_evidence_refs),
            singleLine = false,
            minLines = 2
        )
        InfoBanner(message = stringResource(R.string.pickup_evidence_notice))
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealButton(
            text = stringResource(R.string.pickup_action_submit),
            isLoading = state.isLoading,
            onClick = {
                viewModel.confirmDistribution(
                    DistributionInput(
                        actualReceivedQuantity = received,
                        actualDistributedQuantity = distributed,
                        beneficiariesServed = beneficiaries,
                        distributedAt = distributedAt,
                        location = location,
                        remainingQuantity = remaining,
                        notes = notes.ifBlank { null },
                        evidenceRefs = evidence.split(',')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    )
                )
            }
        )
    }
}

@Composable
private fun CompleteForm(
    state: PickupActionUiState,
    viewModel: PickupActionViewModel
) {
    FormCard {
        Text(
            text = stringResource(R.string.pickup_distribution_notice),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealButton(
            text = stringResource(R.string.pickup_action_submit),
            isLoading = state.isLoading,
            onClick = viewModel::complete
        )
    }
}

@Composable
private fun ExceptionForm(
    state: PickupActionUiState,
    viewModel: PickupActionViewModel
) {
    var reason by remember { mutableStateOf(PickupExceptionReason.MISSED_PICKUP) }
    var note by remember { mutableStateOf("") }

    FormCard {
        Text(
            text = stringResource(R.string.pickup_field_exception_reason),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        PickupExceptionReason.entries.forEach { option ->
            FilterChip(
                selected = reason == option,
                onClick = { reason = option },
                label = { Text(exceptionReasonLabel(option)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(R.string.pickup_field_exception_note),
            singleLine = false,
            minLines = 2
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealButton(
            text = stringResource(R.string.pickup_action_submit),
            isLoading = state.isLoading,
            onClick = { viewModel.recordException(reason, note.ifBlank { null }) }
        )
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md), content = content)
    }
}

@Composable
private fun DateTimePickerButton(
    label: String,
    timestamp: Long,
    onTimestampChange: (Long) -> Unit
) {
    val context = LocalContext.current
    val formatter = rememberPickupFormatter()
    ShareMealButton(
        text = "$label: ${formatter.format(Date(timestamp))}",
        onClick = {
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onTimestampChange(
                                Calendar.getInstance().apply {
                                    set(year, month, day, hour, minute)
                                }.timeInMillis
                            )
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
}

@Composable
private fun PickupTimelineRow(
    entry: PickupTimelineEntry,
    formatter: SimpleDateFormat
) {
    val isDone = entry.completedAt != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isDone) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workflowStageLabel(entry.stage),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (entry.isCurrent) FontWeight.Bold else FontWeight.Normal
            )
            entry.completedAt?.let {
                Text(
                    text = formatter.format(Date(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.isCurrent) {
                Text(
                    text = stringResource(R.string.pickup_timeline_current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md), content = content)
    }
}

@Composable
private fun PickupRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.ifBlank { stringResource(R.string.value_not_provided) },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun rememberPickupFormatter(): SimpleDateFormat {
    val locale = java.util.Locale.Builder()
        .setLanguage(ComposeLocale.current.language)
        .setRegion(ComposeLocale.current.region)
        .build()
    return remember(locale) { SimpleDateFormat("d MMM yyyy, h:mm a", locale) }
}

@Composable
private fun pickupStatusLabel(status: PickupWorkflowStatus): String = when (status) {
    PickupWorkflowStatus.SCHEDULED -> stringResource(R.string.pickup_status_scheduled)
    PickupWorkflowStatus.NGO_READY -> stringResource(R.string.pickup_status_ngo_ready)
    PickupWorkflowStatus.DONOR_READY -> stringResource(R.string.pickup_status_donor_ready)
    PickupWorkflowStatus.COLLECTED -> stringResource(R.string.pickup_status_collected)
    PickupWorkflowStatus.DISTRIBUTED -> stringResource(R.string.pickup_status_distributed)
    PickupWorkflowStatus.COMPLETED -> stringResource(R.string.pickup_status_completed)
    PickupWorkflowStatus.MISSED -> stringResource(R.string.pickup_status_missed)
    PickupWorkflowStatus.CANCELLED -> stringResource(R.string.pickup_status_cancelled)
    PickupWorkflowStatus.DONOR_UNAVAILABLE -> stringResource(R.string.pickup_status_donor_unavailable)
    PickupWorkflowStatus.NGO_UNABLE_TO_COLLECT -> stringResource(R.string.pickup_status_ngo_unable_to_collect)
    PickupWorkflowStatus.SAFETY_REJECTED -> stringResource(R.string.pickup_status_safety_rejected)
}

@Composable
private fun workflowStageLabel(stage: WorkflowStage): String = when (stage) {
    WorkflowStage.DONATION_PUBLISHED -> stringResource(R.string.pickup_stage_donation_published)
    WorkflowStage.NGO_REQUESTED -> stringResource(R.string.pickup_stage_ngo_requested)
    WorkflowStage.DONOR_APPROVED -> stringResource(R.string.pickup_stage_donor_approved)
    WorkflowStage.PICKUP_SCHEDULED -> stringResource(R.string.pickup_stage_pickup_scheduled)
    WorkflowStage.NGO_READY -> stringResource(R.string.pickup_status_ngo_ready)
    WorkflowStage.DONOR_READY -> stringResource(R.string.pickup_status_donor_ready)
    WorkflowStage.FOOD_COLLECTED -> stringResource(R.string.pickup_stage_food_collected)
    WorkflowStage.DISTRIBUTION_CONFIRMED -> stringResource(R.string.pickup_stage_distribution_confirmed)
    WorkflowStage.COMPLETED -> stringResource(R.string.pickup_stage_completed)
    WorkflowStage.MISSED -> stringResource(R.string.pickup_status_missed)
    WorkflowStage.CANCELLED -> stringResource(R.string.pickup_status_cancelled)
    WorkflowStage.DONOR_UNAVAILABLE -> stringResource(R.string.pickup_status_donor_unavailable)
    WorkflowStage.NGO_UNABLE_TO_COLLECT -> stringResource(R.string.pickup_status_ngo_unable_to_collect)
    WorkflowStage.SAFETY_REJECTED -> stringResource(R.string.pickup_status_safety_rejected)
}

@Composable
private fun exceptionReasonLabel(reason: PickupExceptionReason): String = when (reason) {
    PickupExceptionReason.MISSED_PICKUP -> stringResource(R.string.pickup_exception_missed_pickup)
    PickupExceptionReason.CANCELLED_PICKUP -> stringResource(R.string.pickup_exception_cancelled_pickup)
    PickupExceptionReason.DONOR_UNAVAILABLE -> stringResource(R.string.pickup_exception_donor_unavailable)
    PickupExceptionReason.NGO_UNABLE_TO_COLLECT -> stringResource(R.string.pickup_exception_ngo_unable_to_collect)
    PickupExceptionReason.SAFETY_REJECTED -> stringResource(R.string.pickup_exception_safety_rejected)
}
