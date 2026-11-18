package com.sharemeal.app.feature.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.feature.requests.components.RequestCard

@Composable
fun NgoRequestsScreen(
    onBack: () -> Unit,
    onDiscovery: () -> Unit,
    onRequestSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NgoRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    val active = state.requests.filter {
        it.status == DonationRequestStatus.PENDING ||
            it.status == DonationRequestStatus.APPROVED ||
            it.status == DonationRequestStatus.PICKUP_SCHEDULED ||
            it.status == DonationRequestStatus.COLLECTED
    }
    val history = state.requests.filterNot { it in active }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.ngo_requests_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.request_tab_active, active.size)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.request_tab_history, history.size)) }
                )
            }

            state.errorText?.let {
                FeedbackBanner(message = it, type = FeedbackType.ERROR, modifier = Modifier.padding(Spacing.md))
            }
            state.infoText?.let {
                FeedbackBanner(message = it, type = FeedbackType.SUCCESS, modifier = Modifier.padding(Spacing.md))
            }

            val visible = if (selectedTab == 0) active else history
            when {
                state.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())

                visible.isEmpty() -> EmptyState(
                    title = stringResource(R.string.request_empty_title),
                    message = stringResource(R.string.request_empty_body),
                    actionLabel = stringResource(R.string.request_empty_action),
                    onAction = onDiscovery,
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(items = visible, key = { it.id }) { request ->
                        RequestCard(request = request, onClick = { onRequestSelected(request.id) })
                        if (selectedTab == 0) {
                            when (request.status) {
                                DonationRequestStatus.PENDING -> ShareMealButton(
                                    text = stringResource(R.string.request_action_cancel),
                                    onClick = { viewModel.cancelRequest(request.id) }
                                )
                                DonationRequestStatus.APPROVED,
                                DonationRequestStatus.PICKUP_SCHEDULED -> ShareMealButton(
                                    text = stringResource(R.string.request_action_confirm_collection),
                                    icon = Icons.Rounded.LocalShipping,
                                    onClick = { viewModel.confirmCollected(request.id) }
                                )
                                else -> Unit
                            }
                        }
                    }
                    item {
                        ShareMealButton(
                            text = stringResource(R.string.request_action_find_more),
                            icon = Icons.Rounded.Add,
                            onClick = onDiscovery
                        )
                    }
                }
            }
        }
    }
}
