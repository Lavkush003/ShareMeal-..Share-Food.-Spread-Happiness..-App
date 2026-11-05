package com.sharemeal.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.labelRes
import com.sharemeal.app.domain.model.DonorCategory

@Composable
fun DonorCategorySelector(
    selected: DonorCategory?,
    onSelect: (DonorCategory) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected?.let { stringResource(it.labelRes()) }
        ?: stringResource(R.string.field_select_category)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.field_donor_category),
            style = MaterialTheme.typography.labelLarge,
            color = if (errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.padding(top = Spacing.xs))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = Icons.Rounded.Storefront, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DonorCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(category.labelRes())) },
                        onClick = {
                            onSelect(category)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs)
            )
        }
    }
}

@Composable
fun RoleOptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.width(44.dp)
            ) {
                Box(
                    modifier = Modifier.padding(Spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.padding(top = Spacing.xs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}
