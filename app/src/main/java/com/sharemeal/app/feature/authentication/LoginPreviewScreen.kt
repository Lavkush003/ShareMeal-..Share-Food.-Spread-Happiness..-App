package com.sharemeal.app.feature.authentication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.InfoBanner
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealLogo
import com.sharemeal.app.core.ui.components.ShareMealOutlinedButton

@Composable
fun LoginPreviewScreen(
    onDonorPreview: () -> Unit,
    onNgoPreview: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.xl))
        ShareMealLogo(size = 88.dp)
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.login_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
        InfoBanner(message = stringResource(R.string.login_preview_note))
        Spacer(modifier = Modifier.height(Spacing.xxl))
        ShareMealButton(
            text = stringResource(R.string.login_continue_donor),
            onClick = onDonorPreview,
            icon = Icons.Rounded.Restaurant
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealOutlinedButton(
            text = stringResource(R.string.login_continue_ngo),
            onClick = onNgoPreview,
            icon = Icons.Rounded.VolunteerActivism
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        TextButton(onClick = onRegister) {
            Text(text = stringResource(R.string.login_create_account))
        }
    }
}
