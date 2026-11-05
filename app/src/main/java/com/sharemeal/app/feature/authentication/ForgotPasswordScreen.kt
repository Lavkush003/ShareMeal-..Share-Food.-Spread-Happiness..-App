package com.sharemeal.app.feature.authentication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.InfoBanner
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealLogo
import com.sharemeal.app.core.ui.components.ShareMealTextField
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthenticationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.forgot_password_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShareMealLogo(size = 72.dp)
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.forgot_password_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
            InfoBanner(message = stringResource(R.string.forgot_password_note))
            Spacer(modifier = Modifier.height(Spacing.lg))

            state.resetMessageRes?.let { messageRes ->
                FeedbackBanner(
                    message = stringResource(messageRes),
                    type = FeedbackType.INFO
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            ShareMealTextField(
                value = state.resetEmail,
                onValueChange = viewModel::onResetEmailChange,
                label = stringResource(R.string.field_email),
                leadingIcon = Icons.Rounded.Email,
                isError = state.resetFieldErrorRes != null,
                errorMessage = state.resetFieldErrorRes?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                enabled = !state.isRequestingReset
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            ShareMealButton(
                text = stringResource(R.string.action_send_reset_link),
                onClick = viewModel::requestPasswordReset,
                isLoading = state.isRequestingReset,
                icon = Icons.Rounded.LockReset,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            TextButton(
                onClick = onBack,
                enabled = !state.isRequestingReset
            ) {
                Text(text = stringResource(R.string.action_back_to_login))
            }
        }
    }
}
