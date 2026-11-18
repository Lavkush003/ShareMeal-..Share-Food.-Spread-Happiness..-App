package com.sharemeal.app.feature.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.DashboardActionCard
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.LoadingState
import com.sharemeal.app.core.ui.components.SectionHeader
import com.sharemeal.app.core.ui.components.ShareMealOutlinedButton
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.ui.components.StatusChip
import com.sharemeal.app.core.ui.components.WelcomeBanner
import com.sharemeal.app.domain.model.NgoActivity
import com.sharemeal.app.domain.model.NgoActivityType
import com.sharemeal.app.domain.model.NgoDashboardStats
import com.sharemeal.app.domain.model.NgoVerificationStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NgoDashboardScreen(
    onDiscovery: () -> Unit,
    onRequests: () -> Unit,
    onPickups: () -> Unit,
    onCompleted: () -> Unit,
    onHistory: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NgoDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val actions = listOf(
        NgoDashboardAction(
            titleRes = R.string.ngo_action_discover_title,
            descriptionRes = R.string.ngo_action_discover_body,
            icon = Icons.Rounded.Search,
            onClick = onDiscovery
        ),
        NgoDashboardAction(
            titleRes = R.string.ngo_action_requests_title,
            descriptionRes = R.string.ngo_action_requests_body,
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            onClick = onRequests
        ),
        NgoDashboardAction(
            titleRes = R.string.ngo_action_pickups_title,
            descriptionRes = R.string.ngo_action_pickups_body,
            icon = Icons.Rounded.LocalShipping,
            onClick = onPickups
        ),
        NgoDashboardAction(
            titleRes = R.string.ngo_action_completed_title,
            descriptionRes = R.string.ngo_action_completed_body,
            icon = Icons.Rounded.CheckCircle,
            onClick = onCompleted
        ),
        NgoDashboardAction(
            titleRes = R.string.donor_action_history_title,
            descriptionRes = R.string.donor_action_history_body,
            icon = Icons.Rounded.History,
            onClick = onHistory
        ),
        NgoDashboardAction(
            titleRes = R.string.donor_action_notifications_title,
            descriptionRes = R.string.donor_action_notifications_body,
            icon = Icons.Rounded.Notifications,
            onClick = onNotifications
        ),
        NgoDashboardAction(
            titleRes = R.string.donor_action_profile_title,
            descriptionRes = R.string.donor_action_profile_body,
            icon = Icons.Rounded.Person,
            onClick = onProfile
        ),
        NgoDashboardAction(
            titleRes = R.string.donor_action_settings_title,
            descriptionRes = R.string.donor_action_settings_body,
            icon = Icons.Rounded.Settings,
            onClick = onSettings
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(title = stringResource(R.string.ngo_dashboard_title))
        }
    ) { innerPadding ->
        if (state.isLoading && state.stats == null) {
            LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
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
                state.errorMessageRes?.let { errRes ->
                    item {
                        FeedbackBanner(
                            message = stringResource(errRes),
                            type = FeedbackType.ERROR
                        )
                    }
                }
                state.infoMessageRes?.let { infoRes ->
                    item {
                        FeedbackBanner(
                            message = stringResource(infoRes),
                            type = FeedbackType.INFO
                        )
                    }
                }

                item {
                    val ngoName = state.stats?.ngoName ?: state.user?.displayName ?: "NGO Partner"
                    WelcomeBanner(
                        title = stringResource(R.string.ngo_welcome_title),
                        message = "Welcome back, $ngoName. Discover surplus food and coordinate collections.",
                        icon = Icons.Rounded.VolunteerActivism
                    )
                }

                state.stats?.let { stats ->
                    item {
                        VerificationCard(
                            status = stats.verificationStatus,
                            onToggleVerification = viewModel::toggleVerificationStatus,
                            onViewProfile = onProfile
                        )
                    }

                    item {
                        SectionHeader(title = stringResource(R.string.ngo_section_overview))
                    }

                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            maxItemsInEachRow = 2
                        ) {
                            StatCard(
                                title = stringResource(R.string.ngo_stats_available),
                                value = stats.availableNearbyDonationsCount.toString(),
                                icon = Icons.Rounded.Search,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDiscovery() }
                            )
                            StatCard(
                                title = stringResource(R.string.ngo_stats_pending),
                                value = stats.pendingRequestsCount.toString(),
                                icon = Icons.Rounded.HourglassEmpty,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onRequests() }
                            )
                        }
                    }

                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            maxItemsInEachRow = 2
                        ) {
                            StatCard(
                                title = stringResource(R.string.ngo_stats_approved),
                                value = stats.approvedCollectionsCount.toString(),
                                icon = Icons.Rounded.CheckCircle,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPickups() }
                            )
                            StatCard(
                                title = stringResource(R.string.ngo_stats_pickups),
                                value = stats.upcomingPickupsCount.toString(),
                                icon = Icons.Rounded.LocalShipping,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            maxItemsInEachRow = 2
                        ) {
                            StatCard(
                                title = stringResource(R.string.ngo_stats_completed),
                                value = stats.completedDistributionsCount.toString(),
                                icon = Icons.Rounded.History,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = stringResource(R.string.ngo_stats_total_meals),
                                value = stats.totalMealsCollected.toString(),
                                icon = Icons.Rounded.Restaurant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        SectionHeader(title = stringResource(R.string.ngo_section_recent_activities))
                    }

                    if (stats.recentActivities.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.ngo_no_activities),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(
                            items = stats.recentActivities,
                            key = { it.id }
                        ) { activity ->
                            ActivityCard(activity = activity)
                        }
                    }
                }

                item {
                    SectionHeader(title = stringResource(R.string.section_collection_workflow))
                }

                items(
                    items = actions,
                    key = { it.titleRes }
                ) { action ->
                    DashboardActionCard(
                        title = stringResource(action.titleRes),
                        description = stringResource(action.descriptionRes),
                        icon = action.icon,
                        onClick = action.onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun VerificationCard(
    status: NgoVerificationStatus,
    onToggleVerification: () -> Unit,
    onViewProfile: () -> Unit
) {
    val containerColor = when (status) {
        NgoVerificationStatus.VERIFIED -> MaterialTheme.colorScheme.secondaryContainer
        NgoVerificationStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
        NgoVerificationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (status) {
        NgoVerificationStatus.VERIFIED -> MaterialTheme.colorScheme.onSecondaryContainer
        NgoVerificationStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
        NgoVerificationStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (status) {
                        NgoVerificationStatus.VERIFIED -> Icons.Rounded.Verified
                        NgoVerificationStatus.PENDING -> Icons.Rounded.Info
                        NgoVerificationStatus.REJECTED -> Icons.Rounded.Info
                    },
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.size(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Verification Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = when (status) {
                            NgoVerificationStatus.VERIFIED -> "Verified NGO"
                            NgoVerificationStatus.PENDING -> "Verification Pending"
                            NgoVerificationStatus.REJECTED -> "Verification Rejected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusChip(
                    label = status.name,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = when (status) {
                    NgoVerificationStatus.VERIFIED -> "Your NGO is verified and authorized to submit donation collection requests."
                    NgoVerificationStatus.PENDING -> "Your NGO profile verification is currently under review by an administrator. Request actions are restricted until verified."
                    NgoVerificationStatus.REJECTED -> "Verification was not approved. Please update your profile and registration details."
                },
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewProfile) {
                    Text(text = "View / Edit Profile")
                }
                ShareMealOutlinedButton(
                    text = stringResource(R.string.ngo_test_mode_toggle),
                    onClick = onToggleVerification
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.size(Spacing.sm))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: NgoActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (activity.type) {
                    NgoActivityType.REQUEST_SUBMITTED -> Icons.AutoMirrored.Rounded.ReceiptLong
                    NgoActivityType.REQUEST_APPROVED -> Icons.Rounded.CheckCircle
                    NgoActivityType.PICKUP_SCHEDULED -> Icons.Rounded.LocalShipping
                    NgoActivityType.DONATION_COLLECTED -> Icons.Rounded.VolunteerActivism
                    NgoActivityType.DISTRIBUTION_COMPLETED -> Icons.Rounded.Restaurant
                    NgoActivityType.VERIFICATION_UPDATE -> Icons.Rounded.Verified
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class NgoDashboardAction(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit
)
