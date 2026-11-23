package com.sharemeal.app.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.EmptyState
import com.sharemeal.app.core.ui.components.LoadingState
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.domain.model.AppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = rememberFormatter()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.notifications_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::markAllRead) {
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = stringResource(R.string.notifications_mark_all)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())

                state.notifications.isEmpty() -> EmptyState(
                    title = stringResource(R.string.notifications_empty_title),
                    message = stringResource(R.string.notifications_empty_body),
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(items = state.notifications, key = { it.id }) { notification ->
                        NotificationItem(
                            notification = notification,
                            formatter = formatter,
                            onClick = {
                                viewModel.markRead(notification.id)
                                notification.requestId?.let(onNotificationSelected)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberFormatter(): SimpleDateFormat =
    androidx.compose.runtime.remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }

@Composable
private fun NotificationItem(
    notification: AppNotification,
    formatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatter.format(Date(notification.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
