package com.sharemeal.app.feature.discovery

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.EmptyState
import com.sharemeal.app.core.ui.components.ErrorState
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.LoadingState
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.domain.model.FoodSortOrder
import com.sharemeal.app.feature.discovery.components.FoodCard
import com.sharemeal.app.feature.discovery.components.FoodFilterSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDiscoveryScreen(
    onBack: () -> Unit,
    onDonationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodDiscoveryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.discovery_screen_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.discovery_refresh)
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
            // Search input field
            OutlinedTextField(
                value = state.query.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                placeholder = { Text(text = stringResource(R.string.discovery_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (state.query.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Quick Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                // Filter Button with Badge
                FilterChip(
                    selected = state.activeFilterCount > 0,
                    onClick = { showFilterSheet = true },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.discovery_filter_title))
                            if (state.activeFilterCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                BadgedBox(badge = { Badge { Text(state.activeFilterCount.toString()) } }) {
                                    Spacer(modifier = Modifier.size(8.dp))
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                // Quick Veg Only chip
                FilterChip(
                    selected = state.query.classification == FoodClassification.VEGETARIAN,
                    onClick = {
                        val newClass = if (state.query.classification == FoodClassification.VEGETARIAN) null else FoodClassification.VEGETARIAN
                        viewModel.updateQuery(state.query.copy(classification = newClass))
                    },
                    label = { Text(text = stringResource(R.string.discovery_filter_veg_only)) }
                )

                // Quick Non-Veg Only chip
                FilterChip(
                    selected = state.query.classification == FoodClassification.NON_VEGETARIAN,
                    onClick = {
                        val newClass = if (state.query.classification == FoodClassification.NON_VEGETARIAN) null else FoodClassification.NON_VEGETARIAN
                        viewModel.updateQuery(state.query.copy(classification = newClass))
                    },
                    label = { Text(text = stringResource(R.string.discovery_filter_non_veg_only)) }
                )

                // Sort By Nearest quick chip
                FilterChip(
                    selected = state.query.sortBy == FoodSortOrder.NEAREST,
                    onClick = {
                        val newSort = if (state.query.sortBy == FoodSortOrder.NEAREST) FoodSortOrder.NEWEST else FoodSortOrder.NEAREST
                        viewModel.updateQuery(state.query.copy(sortBy = newSort))
                    },
                    label = { Text(text = stringResource(R.string.discovery_sort_nearest)) }
                )
            }

            state.errorMessageRes?.let { errRes ->
                FeedbackBanner(
                    message = stringResource(errRes),
                    type = FeedbackType.ERROR,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                )
            }

            // Discovery List View
            when {
                state.isLoading && state.items.isEmpty() -> {
                    LoadingState(
                        modifier = Modifier.fillMaxSize(),
                        message = "Searching surplus food listings…"
                    )
                }

                state.errorMessageRes != null && state.items.isEmpty() -> {
                    ErrorState(
                        title = stringResource(R.string.discovery_screen_title),
                        message = stringResource(state.errorMessageRes!!),
                        modifier = Modifier.fillMaxSize(),
                        onRetry = viewModel::refresh
                    )
                }

                state.items.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.discovery_empty_title),
                        message = stringResource(R.string.discovery_empty_body),
                        actionLabel = stringResource(R.string.discovery_clear_filters),
                        onAction = viewModel::clearFilters,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.lg,
                            end = Spacing.lg,
                            top = Spacing.xs,
                            bottom = Spacing.xl
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        item {
                            Text(
                                text = "${state.items.size} donations available nearby",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        items(
                            items = state.items,
                            key = { it.donation.id }
                        ) { item ->
                            FoodCard(
                                item = item,
                                onClick = { onDonationSelected(item.donation.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FoodFilterSheet(
            query = state.query,
            sheetState = sheetState,
            onQueryChange = viewModel::updateQuery,
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}
