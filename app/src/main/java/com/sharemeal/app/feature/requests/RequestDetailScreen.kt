package com.sharemeal.app.feature.requests

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.feature.requests.components.ApprovalDialog
import com.sharemeal.app.feature.requests.components.RejectionDialog
import com.sharemeal.app.feature.requests.components.RequestStatusChip
import com.sharemeal.app.feature.requests.components.SchedulePickupDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RequestDetailScreen(
    onBack: () -> Unit,
    onOpenPickup: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RequestDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showApproval by remember { mutableStateOf(false) }
    var showRejection by remember { mutableStateOf(false) }
    var showScheduling by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.request_detail_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                message = stringResource(R.string.request_detail_loading)
            )

            state.request == null -> ErrorState(
                title = stringResource(R.string.request_detail_title),
                message = state.errorText ?: stringResource(R.string.error_generic),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onRetry = viewModel::load
            )

            else -> RequestDetailContent(
                state = state,
                innerPadding = innerPadding,
                onApproveClick = { showApproval = true },
                onRejectClick = { showRejection = true },
                onScheduleClick = { showScheduling = true },
                onCancel = viewModel::cancel,
                onOpenPickup = { onOpenPickup(state.request!!.id) }
            )
        }
    }

    state.request?.let { request ->
        if (showApproval) {
            ApprovalDialog(
                request = request,
                onConfirm = { quantity, time ->
                    viewModel.approve(quantity, time)
                    showApproval = false
                },
                onDismiss = { showApproval = false }
            )
        }
        if (showRejection) {
            RejectionDialog(
                request = request,
                onConfirm = { reason ->
                    viewModel.reject(reason)
                    showRejection = false
                },
                onDismiss = { showRejection = false }
            )
        }
        if (showScheduling) {
            SchedulePickupDialog(
                onConfirm = { time ->
                    viewModel.schedulePickup(time)
                    showScheduling = false
                },
                onDismiss = { showScheduling = false }
            )
        }
    }
}

@Composable
private fun RequestDetailContent(
    state: RequestDetailUiState,
    innerPadding: PaddingValues,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onCancel: () -> Unit,
    onOpenPickup: () -> Unit
) {
    val request = state.request ?: return
    val donation = state.donation
    val formatter = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }
    val canRevealPickup = request.status.allocatesQuantity
    val ngo = state.ngo

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
        state.errorText?.let {
            item { FeedbackBanner(message = it, type = FeedbackType.ERROR) }
        }
        state.infoText?.let {
            item { FeedbackBanner(message = it, type = FeedbackType.SUCCESS) }
        }

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
                            text = request.donationTitle.orEmpty().ifBlank {
                                stringResource(R.string.request_untitled_donation)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        RequestStatusChip(
                            status = request.status,
                            syncPending = request.syncPending
                        )
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

        item { SectionHeader(title = stringResource(R.string.request_section_quantities)) }
        item {
            DetailCard {
                DetailRow(stringResource(R.string.request_detail_requested), request.requestedQuantity.toString())
                DetailRow(stringResource(R.string.request_detail_approved), (request.approvedQuantity ?: 0).toString())
                DetailRow(stringResource(R.string.request_detail_people), (request.peopleToServe ?: 0).toString())
                state.summary?.let { summary ->
                    DetailRow(stringResource(R.string.request_detail_total), summary.totalQuantity.toString())
                    DetailRow(stringResource(R.string.request_detail_allocated), summary.allocatedQuantity.toString())
                    DetailRow(stringResource(R.string.request_detail_remaining), summary.remainingQuantity.toString())
                    DetailRow(stringResource(R.string.request_detail_collected), summary.collectedQuantity.toString())
                    DetailRow(stringResource(R.string.request_detail_uncollected), summary.uncollectedQuantity.toString())
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.request_section_timing)) }
        item {
            DetailCard {
                DetailRow(
                    label = stringResource(R.string.request_detail_submitted),
                    value = formatter.format(Date(request.requestedAt))
                )
                request.expectedPickupAt?.let {
                    DetailRow(stringResource(R.string.request_detail_expected_pickup), formatter.format(Date(it)))
                }
                request.pickupScheduledTime?.let {
                    DetailRow(stringResource(R.string.request_detail_scheduled_pickup), formatter.format(Date(it)))
                }
                request.completedAt?.let {
                    DetailRow(stringResource(R.string.request_detail_completed), formatter.format(Date(it)))
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.request_section_ngo)) }
        item {
            DetailCard {
                DetailRow(stringResource(R.string.request_detail_ngo), request.ngoName)
                ngo?.ngoProfile?.let { profile ->
                    DetailRow(
                        stringResource(R.string.request_detail_registration),
                        profile.registrationNumber ?: stringResource(R.string.value_not_provided)
                    )
                    DetailRow(stringResource(R.string.request_detail_contact), profile.contactPersonName)
                    DetailRow(stringResource(R.string.request_detail_operating_area), profile.operatingArea)
                    DetailRow(stringResource(R.string.request_detail_capacity), profile.estimatedDistributionCapacity)
                }
                DetailRow(
                    stringResource(R.string.request_detail_verification),
                    verificationLabel(state.ngoVerification)
                )
            }
        }

        request.notes?.takeIf { it.isNotBlank() }?.let {
            item { SectionHeader(title = stringResource(R.string.request_section_message)) }
            item {
                DetailCard {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        request.rejectionReason?.takeIf { it.isNotBlank() }?.let {
            item {
                InfoBanner(message = stringResource(R.string.request_detail_rejection_reason, it))
            }
        }

        item { SectionHeader(title = stringResource(R.string.request_section_pickup)) }
        item {
            if (canRevealPickup && donation != null) {
                DetailCard {
                    DetailRow(stringResource(R.string.request_detail_address), donation.pickupAddress)
                    DetailRow(
                        stringResource(R.string.request_detail_city),
                        listOf(donation.city, donation.state, donation.pinCode)
                            .filter { it.isNotBlank() }.joinToString(", ")
                    )
                    DetailRow(stringResource(R.string.request_detail_contact), donation.contactPerson)
                    DetailRow(stringResource(R.string.request_detail_phone), donation.contactPhone)
                    donation.specialInstructions?.takeIf { it.isNotBlank() }?.let {
                        DetailRow(stringResource(R.string.request_detail_instructions), it)
                    }
                }
            } else {
                InfoBanner(message = stringResource(R.string.request_detail_privacy_notice))
            }
        }

        if (state.isDonor) {
            when (request.status) {
                DonationRequestStatus.PENDING -> {
                    item {
                        ShareMealButton(
                            text = stringResource(R.string.request_action_approve),
                            icon = Icons.Rounded.CheckCircle,
                            onClick = onApproveClick
                        )
                    }
                    item {
                        ShareMealButton(text = stringResource(R.string.request_action_reject), onClick = onRejectClick)
                    }
                }
                DonationRequestStatus.APPROVED -> {
                    item {
                        ShareMealButton(
                            text = stringResource(R.string.request_action_schedule_pickup),
                            icon = Icons.Rounded.CalendarMonth,
                            onClick = onScheduleClick
                        )
                    }
                }
                DonationRequestStatus.PICKUP_SCHEDULED,
                DonationRequestStatus.COLLECTED,
                DonationRequestStatus.DISTRIBUTED -> {
                    item {
                        ShareMealButton(
                            text = stringResource(R.string.pickup_action_open),
                            icon = Icons.Rounded.LocalShipping,
                            onClick = onOpenPickup
                        )
                    }
                }
                else -> Unit
            }
        } else {
            when (request.status) {
                DonationRequestStatus.PENDING,
                DonationRequestStatus.APPROVED,
                DonationRequestStatus.PICKUP_SCHEDULED -> {
                    item {
                        ShareMealButton(text = stringResource(R.string.request_action_cancel), onClick = onCancel)
                    }
                }
                else -> Unit
            }
            if (request.status != DonationRequestStatus.PENDING &&
                request.status != DonationRequestStatus.CANCELLED &&
                request.status != DonationRequestStatus.REJECTED &&
                request.status != DonationRequestStatus.COMPLETED
            ) {
                item {
                    ShareMealButton(
                        text = stringResource(R.string.pickup_action_open),
                        icon = Icons.Rounded.LocalShipping,
                        onClick = onOpenPickup
                    )
                }
            }
        }
    }
}

@Composable
private fun verificationLabel(status: NgoVerificationStatus): String = when (status) {
    NgoVerificationStatus.VERIFIED -> stringResource(R.string.verification_verified)
    NgoVerificationStatus.PENDING -> stringResource(R.string.verification_pending)
    NgoVerificationStatus.REJECTED -> stringResource(R.string.verification_rejected)
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
private fun DetailRow(label: String, value: String) {
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
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
