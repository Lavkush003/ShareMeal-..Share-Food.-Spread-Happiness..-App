package com.sharemeal.app.feature.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.domain.model.SyncState
import java.text.DateFormat
import java.util.Date

@Composable
fun OfflineSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfflineSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.offline_settings_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.offline_settings_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(
                    R.string.offline_settings_status,
                    statusMessage(uiState.status.state)
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.offline_settings_cached_donations,
                    uiState.cachedDonationCount
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = run {
                    val lastSyncedAt = uiState.status.lastSyncedAt
                    if (lastSyncedAt == null) {
                        stringResource(R.string.offline_settings_never_synced)
                    } else {
                        stringResource(
                            R.string.offline_settings_last_synced,
                            formatTime(lastSyncedAt)
                        )
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (uiState.status.state == SyncState.OFFLINE) {
                    stringResource(R.string.offline_settings_network_offline)
                } else {
                    stringResource(R.string.offline_settings_network_online)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            uiState.status.lastError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = viewModel::syncNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.sync_action_retry))
            }
        }
    }
}

@Composable
private fun statusMessage(state: SyncState): String = when (state) {
    SyncState.SYNCED -> stringResource(R.string.sync_banner_synced)
    SyncState.OFFLINE -> stringResource(R.string.sync_banner_offline)
    SyncState.PENDING, SyncState.IN_PROGRESS -> stringResource(R.string.sync_banner_pending, 0)
    SyncState.FAILED -> stringResource(R.string.sync_banner_failed)
    SyncState.CONFLICT -> stringResource(R.string.sync_banner_conflict)
}

private fun formatTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
