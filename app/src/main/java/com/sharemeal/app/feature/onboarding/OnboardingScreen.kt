package com.sharemeal.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealLogo
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val benefitTitleRes: Int,
    val benefitBodyRes: Int,
    val icon: ImageVector
)

private val pages = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_page1_title,
        bodyRes = R.string.onboarding_page1_body,
        benefitTitleRes = R.string.onboarding_benefit_waste_title,
        benefitBodyRes = R.string.onboarding_benefit_waste_body,
        icon = Icons.Rounded.Eco
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_page2_title,
        bodyRes = R.string.onboarding_page2_body,
        benefitTitleRes = R.string.onboarding_benefit_ngo_title,
        benefitBodyRes = R.string.onboarding_benefit_ngo_body,
        icon = Icons.Rounded.Place
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_page3_title,
        bodyRes = R.string.onboarding_page3_body,
        benefitTitleRes = R.string.onboarding_benefit_community_title,
        benefitBodyRes = R.string.onboarding_benefit_community_body,
        icon = Icons.Rounded.Favorite
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onFinished()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShareMealLogo(size = 48.dp)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = viewModel::complete,
                enabled = !state.isCompleting
            ) {
                Text(text = stringResource(R.string.action_skip))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        PageIndicator(
            pageCount = pages.size,
            selectedPage = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg)
        )

        ShareMealButton(
            text = stringResource(
                if (isLastPage) R.string.action_get_started else R.string.action_next
            ),
            onClick = {
                if (isLastPage) {
                    viewModel.complete()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            isLoading = state.isCompleting
        )
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(136.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = stringResource(page.benefitTitleRes),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(page.benefitBodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    selectedPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.xs)
                    .size(if (index == selectedPage) 10.dp else 8.dp)
                    .background(
                        color = if (index == selectedPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}
