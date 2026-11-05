package com.sharemeal.app.feature.authentication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.RoleOptionCard
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.domain.model.UserRole

@Composable
fun RoleSelectionScreen(
    onContinue: (UserRole) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.register_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.register_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
            RoleOptionCard(
                title = stringResource(R.string.role_donor_title),
                description = stringResource(R.string.role_donor_body),
                selected = selectedRole == UserRole.DONOR,
                onClick = { selectedRole = UserRole.DONOR },
                icon = Icons.Rounded.Restaurant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            RoleOptionCard(
                title = stringResource(R.string.role_ngo_title),
                description = stringResource(R.string.role_ngo_body),
                selected = selectedRole == UserRole.NGO,
                onClick = { selectedRole = UserRole.NGO },
                icon = Icons.Rounded.VolunteerActivism
            )
            Spacer(modifier = Modifier.height(Spacing.xxl))
            ShareMealButton(
                text = stringResource(R.string.role_selection_continue),
                onClick = { selectedRole?.let(onContinue) },
                enabled = selectedRole != null,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            TextButton(onClick = onLogin) {
                Text(text = stringResource(R.string.register_have_account))
            }
        }
    }
}
