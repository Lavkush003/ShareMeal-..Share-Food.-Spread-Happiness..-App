package com.sharemeal.app.feature.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.EmptyState
import com.sharemeal.app.core.ui.components.LoadingState
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.feature.requests.components.RequestCard

@Composable
fun RequestHistoryScreen(
    onBack: () -> Unit,
    onRequestSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RequestHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.request_history_title),
                onBack = onBack
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

                state.requests.isEmpty() -> EmptyState(
                    title = stringResource(R.string.request_history_empty_title),
                    message = stringResource(R.string.request_history_empty_body),
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(items = state.requests, key = { it.id }) { request ->
                        RequestCard(request = request, onClick = { onRequestSelected(request.id) })
                    }
                }
            }
        }
    }
}
