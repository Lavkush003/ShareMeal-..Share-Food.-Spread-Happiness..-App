package com.sharemeal.app.feature.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
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
fun DonorDashboardScreen(
    onAddDonation: () -> Unit,
    onMyDonations: () -> Unit,
    onHistory: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        DashboardAction(
            titleRes = R.string.donor_action_add_title,
            descriptionRes = R.string.donor_action_add_body,
            icon = Icons.Rounded.AddCircle,
            onClick = onAddDonation
        ),
        DashboardAction(
            titleRes = R.string.donor_action_donations_title,
            descriptionRes = R.string.donor_action_donations_body,
            icon = Icons.Rounded.Restaurant,
            onClick = onMyDonations
        ),
        DashboardAction(
            titleRes = R.string.donor_action_history_title,
            descriptionRes = R.string.donor_action_history_body,
            icon = Icons.Rounded.History,
            onClick = onHistory
        ),
        DashboardAction(
            titleRes = R.string.donor_action_notifications_title,
            descriptionRes = R.string.donor_action_notifications_body,
            icon = Icons.Rounded.Notifications,
            onClick = onNotifications
        ),
        DashboardAction(
            titleRes = R.string.donor_action_profile_title,
            descriptionRes = R.string.donor_action_profile_body,
            icon = Icons.Rounded.Person,
            onClick = onProfile
        ),
        DashboardAction(
            titleRes = R.string.donor_action_settings_title,
            descriptionRes = R.string.donor_action_settings_body,
            icon = Icons.Rounded.Settings,
            onClick = onSettings
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(title = stringResource(R.string.donor_dashboard_title))
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
                    title = stringResource(R.string.donor_welcome_title),
                    message = stringResource(R.string.donor_welcome_body),
                    icon = Icons.Rounded.Restaurant
                )
            }
            item {
                InfoBanner(message = stringResource(R.string.donor_dashboard_preview_note))
            }
            item {
                SectionHeader(title = stringResource(R.string.section_donation_workflow))
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

private data class DashboardAction(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit
)
