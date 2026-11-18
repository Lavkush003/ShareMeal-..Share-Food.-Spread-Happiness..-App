package com.sharemeal.app.feature.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.EmptyState
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.LoadingState
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.feature.requests.components.ApprovalDialog
import com.sharemeal.app.feature.requests.components.RejectionDialog
import com.sharemeal.app.feature.requests.components.RequestCard

@Composable
fun DonorRequestsScreen(
    onBack: () -> Unit,
    onRequestSelected: (String) -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DonorRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var approvalTarget by remember { mutableStateOf<DonationRequest?>(null) }
    var rejectionTarget by remember { mutableStateOf<DonationRequest?>(null) }

    val pending = state.requests.filter { it.status == DonationRequestStatus.PENDING }
    val active = state.requests.filter {
        it.status == DonationRequestStatus.APPROVED ||
            it.status == DonationRequestStatus.PICKUP_SCHEDULED ||
            it.status == DonationRequestStatus.COLLECTED
    }
    val visible = if (selectedTab == 0) pending else active

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.donor_requests_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            androidx.compose.material3.PrimaryTabRow(selectedTabIndex = selectedTab) {
                androidx.compose.material3.Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.donor_requests_pending, pending.size)) }
                )
                androidx.compose.material3.Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.donor_requests_active, active.size)) }
                )
            }

            state.errorText?.let {
                FeedbackBanner(message = it, type = FeedbackType.ERROR, modifier = Modifier.padding(Spacing.md))
            }
            state.infoText?.let {
                FeedbackBanner(message = it, type = FeedbackType.SUCCESS, modifier = Modifier.padding(Spacing.md))
            }

            when {
                state.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())

                visible.isEmpty() -> EmptyState(
                    title = stringResource(R.string.donor_requests_empty_title),
                    message = stringResource(R.string.donor_requests_empty_body),
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(items = visible, key = { it.id }) { request ->
                        RequestCard(
                            request = request,
                            ngo = state.ngoProfiles[request.ngoId],
                            onClick = { onRequestSelected(request.id) }
                        )
                        if (request.status == DonationRequestStatus.PENDING) {
                            ShareMealButton(
                                text = stringResource(R.string.request_action_approve),
                                onClick = { approvalTarget = request }
                            )
                            ShareMealButton(
                                text = stringResource(R.string.request_action_reject),
                                onClick = { rejectionTarget = request }
                            )
                        } else if (request.status == DonationRequestStatus.COLLECTED) {
                            ShareMealButton(
                                text = stringResource(R.string.request_action_complete),
                                icon = Icons.Rounded.CheckCircle,
                                onClick = { viewModel.complete(request.id) }
                            )
                        }
                    }
                    item {
                        ShareMealButton(
                            text = stringResource(R.string.request_action_view_history),
                            onClick = onHistory
                        )
                    }
                }
            }
        }
    }

    approvalTarget?.let { request ->
        ApprovalDialog(
            request = request,
            onConfirm = { quantity, pickupTime ->
                viewModel.approve(request.id, quantity, pickupTime)
                approvalTarget = null
            },
            onDismiss = { approvalTarget = null }
        )
    }
    rejectionTarget?.let { request ->
        RejectionDialog(
            request = request,
            onConfirm = { reason ->
                viewModel.reject(request.id, reason)
                rejectionTarget = null
            },
            onDismiss = { rejectionTarget = null }
        )
    }
}

@Composable
private fun RequestCard(
    request: DonationRequest,
    ngo: User?,
    onClick: () -> Unit
) {
    com.sharemeal.app.feature.requests.components.RequestCard(
        request = request.copy(ngoName = ngo?.ngoProfile?.ngoName ?: request.ngoName),
        onClick = onClick
    )
}
