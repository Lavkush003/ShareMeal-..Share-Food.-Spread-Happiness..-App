package com.sharemeal.app.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.ShareMealLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onDestination: (SplashDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.destination) {
        val destination = state.destination ?: return@LaunchedEffect
        delay(700)
        onDestination(destination)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ShareMealLogo(
                size = 112.dp,
                containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xxl))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.splash_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
            )
        }
    }
}
