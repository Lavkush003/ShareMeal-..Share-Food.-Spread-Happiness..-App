package com.sharemeal.app.feature.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sharemeal.app.core.ui.components.PasswordTextField
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealLogo
import com.sharemeal.app.core.ui.components.ShareMealTextField
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.domain.model.UserRole

@Composable
fun LoginScreen(
    onLoggedIn: (UserRole) -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthenticationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthenticationEvent.Authenticated) {
                onLoggedIn(event.role)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.action_login),
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
            ShareMealLogo(size = 80.dp)
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.login_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xl))

            if (viewModel.isMockMode) {
                InfoBanner(message = stringResource(R.string.login_mock_note))
                Spacer(modifier = Modifier.height(Spacing.md))
                InfoBanner(message = stringResource(R.string.login_demo_credentials))
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            state.errorMessageRes?.let { messageRes ->
                FeedbackBanner(
                    message = stringResource(messageRes),
                    type = FeedbackType.ERROR
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            ShareMealTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.field_email),
                leadingIcon = Icons.Rounded.Email,
                isError = state.fieldErrors.containsKey(AuthField.EMAIL),
                errorMessage = state.fieldErrors[AuthField.EMAIL]?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                enabled = !state.isSubmitting
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            PasswordTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(R.string.field_password),
                isVisible = state.isPasswordVisible,
                onToggleVisibility = viewModel::togglePasswordVisibility,
                errorMessage = state.fieldErrors[AuthField.PASSWORD]?.let { stringResource(it) },
                enabled = !state.isSubmitting,
                imeAction = ImeAction.Done
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = state.rememberSession,
                        onCheckedChange = viewModel::onRememberSessionChange,
                        enabled = !state.isSubmitting
                    )
                    Text(
                        text = stringResource(R.string.login_remember),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(
                    onClick = onForgotPassword,
                    enabled = !state.isSubmitting
                ) {
                    Text(text = stringResource(R.string.login_forgot_password))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            ShareMealButton(
                text = stringResource(R.string.action_login),
                onClick = viewModel::login,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.login_no_account),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = onRegister,
                    enabled = !state.isSubmitting
                ) {
                    Text(text = stringResource(R.string.login_create_account))
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
