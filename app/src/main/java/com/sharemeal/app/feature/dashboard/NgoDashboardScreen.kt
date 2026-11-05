package com.sharemeal.app.feature.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.DashboardActionCard
import com.sharemeal.app.core.ui.components.InfoBanner
import com.sharemeal.app.core.ui.components.SectionHeader
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.ui.components.WelcomeBanner

@Composable
fun NgoDashboardScreen(
    onDiscovery: () -> Unit,
    onRequests: () -> Unit,
    onPickups: () -> Unit,
    onHistory: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                WelcomeBanner(
                    title = stringResource(R.string.ngo_welcome_title),
                    message = stringResource(R.string.ngo_welcome_body),
                    icon = Icons.Rounded.VolunteerActivism
                )
            }
            item {
                InfoBanner(message = stringResource(R.string.ngo_dashboard_preview_note))
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

private data class NgoDashboardAction(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit
)
