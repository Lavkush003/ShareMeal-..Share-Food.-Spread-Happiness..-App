package com.sharemeal.app.core.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.sharemeal.app.R

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next
) {
    ShareMealTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Rounded.Lock,
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) {
                        Icons.Rounded.VisibilityOff
                    } else {
                        Icons.Rounded.Visibility
                    },
                    contentDescription = stringResource(
                        if (isVisible) R.string.cd_hide_password else R.string.cd_show_password
                    )
                )
            }
        },
        isError = errorMessage != null,
        errorMessage = errorMessage,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        visualTransformation = if (isVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        enabled = enabled
    )
}
