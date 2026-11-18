package com.sharemeal.app.feature.requests.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.StatusChip
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun RequestStatusChip(status: DonationRequestStatus, syncPending: Boolean = false) {
    val colors = when {
        syncPending -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
        status == DonationRequestStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
        status == DonationRequestStatus.APPROVED ||
            status == DonationRequestStatus.PICKUP_SCHEDULED -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
        status == DonationRequestStatus.COLLECTED ||
            status == DonationRequestStatus.COMPLETED ||
            status == DonationRequestStatus.DISTRIBUTED -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    }
    StatusChip(
        label = if (syncPending) {
            stringResource(R.string.request_status_pending_sync)
        } else {
            requestStatusLabel(status)
        },
        containerColor = colors.first,
        contentColor = colors.second
    )
}

@Composable
fun requestStatusLabel(status: DonationRequestStatus): String = when (status) {
    DonationRequestStatus.PENDING -> stringResource(R.string.request_status_pending)
    DonationRequestStatus.APPROVED -> stringResource(R.string.request_status_approved)
    DonationRequestStatus.REJECTED -> stringResource(R.string.request_status_rejected)
    DonationRequestStatus.CANCELLED -> stringResource(R.string.request_status_cancelled)
    DonationRequestStatus.PICKUP_SCHEDULED -> stringResource(R.string.request_status_pickup_scheduled)
    DonationRequestStatus.COLLECTED -> stringResource(R.string.request_status_collected)
    DonationRequestStatus.DISTRIBUTED,
    DonationRequestStatus.COMPLETED -> stringResource(R.string.request_status_completed)
    DonationRequestStatus.EXPIRED -> stringResource(R.string.request_status_expired)
}

@Composable
fun RequestCard(
    request: DonationRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = SimpleDateFormat(
        "d MMM, h:mm a",
        java.util.Locale.Builder()
            .setLanguage(ComposeLocale.current.language)
            .setRegion(ComposeLocale.current.region)
            .build()
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.donationTitle.orEmpty().ifBlank {
                            stringResource(R.string.request_untitled_donation)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.request_card_subtitle,
                            request.ngoName,
                            formatter.format(Date(request.requestedAt))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RequestStatusChip(status = request.status, syncPending = request.syncPending)
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = stringResource(
                        R.string.request_card_requested,
                        request.requestedQuantity
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                request.approvedQuantity?.let {
                    Text(
                        text = stringResource(R.string.request_card_approved, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            request.expectedPickupAt?.let {
                Text(
                    text = stringResource(
                        R.string.request_card_pickup,
                        formatter.format(Date(it))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
