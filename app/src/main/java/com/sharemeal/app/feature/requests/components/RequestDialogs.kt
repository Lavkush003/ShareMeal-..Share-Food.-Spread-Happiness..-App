package com.sharemeal.app.feature.requests.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.domain.model.DonationRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ApprovalDialog(
    request: DonationRequest,
    onConfirm: (Int, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf(request.approvedQuantity?.toString() ?: request.requestedQuantity.toString()) }
    var pickupTime by remember { mutableLongStateOf(request.expectedPickupAt ?: System.currentTimeMillis()) }
    val formatter = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }
    val context = LocalContext.current
    val quantity = quantityText.toIntOrNull()
    val isValid = quantity != null && quantity > 0 && quantity <= request.requestedQuantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.request_approve_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.request_approve_dialog_body,
                        request.requestedQuantity,
                        request.ngoName
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.request_approve_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantityText.isNotEmpty() && !isValid,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isValid) {
                    Text(
                        text = stringResource(R.string.request_approve_quantity_error, request.requestedQuantity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                ShareMealButton(
                    text = stringResource(R.string.request_pickup_time_label, formatter.format(Date(pickupTime))),
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = pickupTime }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        pickupTime = Calendar.getInstance().apply {
                                            set(year, month, day, hour, minute)
                                        }.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(quantity, pickupTime) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.request_approve_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun SchedulePickupDialog(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var pickupTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val formatter = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.request_schedule_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.request_schedule_dialog_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                ShareMealButton(
                    text = stringResource(R.string.request_pickup_time_label, formatter.format(Date(pickupTime))),
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = pickupTime }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        pickupTime = Calendar.getInstance().apply {
                                            set(year, month, day, hour, minute)
                                        }.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickupTime) }) {
                Text(stringResource(R.string.request_schedule_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun RejectionDialog(
    request: DonationRequest,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.request_reject_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.request_reject_dialog_body, request.ngoName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.request_reject_reason)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.takeIf { it.isNotBlank() }) }) {
                Text(stringResource(R.string.request_reject_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
