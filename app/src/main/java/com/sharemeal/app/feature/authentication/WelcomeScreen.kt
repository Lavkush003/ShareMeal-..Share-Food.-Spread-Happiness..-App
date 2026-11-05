package com.sharemeal.app.feature.authentication

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealLogo
import com.sharemeal.app.core.ui.components.ShareMealOutlinedButton

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
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
        ShareMealLogo(size = 104.dp)
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
        WelcomeFeature(
            icon = Icons.Rounded.Restaurant,
            title = stringResource(R.string.welcome_feature_donor_title),
            body = stringResource(R.string.welcome_feature_donor_body)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        WelcomeFeature(
            icon = Icons.Rounded.VolunteerActivism,
            title = stringResource(R.string.welcome_feature_ngo_title),
            body = stringResource(R.string.welcome_feature_ngo_body)
        )
        Spacer(modifier = Modifier.height(Spacing.xxl))
        ShareMealButton(
            text = stringResource(R.string.welcome_login),
            onClick = onLogin,
            icon = Icons.Rounded.Restaurant
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        ShareMealOutlinedButton(
            text = stringResource(R.string.welcome_register),
            onClick = onRegister
        )
    }
}

@Composable
private fun WelcomeFeature(
    icon: ImageVector,
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
