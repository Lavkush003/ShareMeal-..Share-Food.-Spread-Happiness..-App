package com.sharemeal.app.feature.discovery.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.labelRes
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.model.FoodSortOrder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodFilterSheet(
    query: FoodDiscoveryQuery,
    sheetState: SheetState,
    onQueryChange: (FoodDiscoveryQuery) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.discovery_filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Text(text = stringResource(R.string.discovery_clear_filters))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Sort Order Section
            Text(
                text = stringResource(R.string.discovery_sort_by),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FoodSortOrder.entries.forEach { sort ->
                    val label = when (sort) {
                        FoodSortOrder.NEAREST -> stringResource(R.string.discovery_sort_nearest)
                        FoodSortOrder.EARLIEST_DEADLINE -> stringResource(R.string.discovery_sort_earliest_deadline)
                        FoodSortOrder.NEWEST -> stringResource(R.string.discovery_sort_newest)
                    }
                    FilterChip(
                        selected = query.sortBy == sort,
                        onClick = { onQueryChange(query.copy(sortBy = sort)) },
                        label = { Text(text = label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Veg / Non-Veg Classification Section
            Text(
                text = "Classification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = query.classification == null,
                    onClick = { onQueryChange(query.copy(classification = null)) },
                    label = { Text(text = stringResource(R.string.discovery_filter_all_classifications)) }
                )
                FoodClassification.entries.forEach { classification ->
                    val label = when (classification) {
                        FoodClassification.VEGETARIAN -> stringResource(R.string.discovery_filter_veg_only)
                        FoodClassification.NON_VEGETARIAN -> stringResource(R.string.discovery_filter_non_veg_only)
                        FoodClassification.VEGAN -> "Vegan Only"
                        FoodClassification.OTHER -> "Other"
                    }
                    FilterChip(
                        selected = query.classification == classification,
                        onClick = { onQueryChange(query.copy(classification = classification)) },
                        label = { Text(text = label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Food Category Section
            Text(
                text = "Food Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = query.category == null,
                    onClick = { onQueryChange(query.copy(category = null)) },
                    label = { Text(text = stringResource(R.string.discovery_filter_all_categories)) }
                )
                FoodCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = query.category == cat,
                        onClick = { onQueryChange(query.copy(category = cat)) },
                        label = { Text(text = stringResource(cat.labelRes())) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Distance Filter Section
            Text(
                text = "Max Distance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                listOf(null, 5.0, 10.0, 20.0, 50.0).forEach { dist ->
                    val label = if (dist == null) "Any Distance" else "${dist.toInt()} km"
                    FilterChip(
                        selected = query.maxDistanceKm == dist,
                        onClick = { onQueryChange(query.copy(maxDistanceKm = dist)) },
                        label = { Text(text = label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            ShareMealButton(
                text = stringResource(R.string.discovery_apply_filters),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
