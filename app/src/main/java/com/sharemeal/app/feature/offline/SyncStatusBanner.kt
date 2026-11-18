package com.sharemeal.app.feature.offline

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.domain.model.SyncState

@Composable
fun SyncStatusBanner(
    modifier: Modifier = Modifier,
    viewModel: SyncStatusViewModel = hiltViewModel()
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    if (status.state == SyncState.SYNCED) return

    val message = when (status.state) {
        SyncState.OFFLINE -> stringResource(R.string.sync_banner_offline)
        SyncState.CONFLICT -> stringResource(R.string.sync_banner_conflict)
        SyncState.FAILED -> stringResource(R.string.sync_banner_failed)
        SyncState.PENDING, SyncState.IN_PROGRESS ->
            stringResource(R.string.sync_banner_pending, status.pendingCount)

        SyncState.SYNCED -> ""
    }

    Surface(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.sm
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall
            )
            if (status.state != SyncState.OFFLINE) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                TextButton(onClick = viewModel::retry) {
                    Text(text = stringResource(R.string.sync_action_retry))
                }
            }
        }
    }
}
