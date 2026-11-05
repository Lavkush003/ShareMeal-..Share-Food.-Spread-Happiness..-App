package com.sharemeal.app.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sharemeal.app.core.ui.Spacing

enum class FeedbackType {
    INFO,
    SUCCESS,
    ERROR
}

@Composable
fun FeedbackBanner(
    message: String,
    type: FeedbackType,
    modifier: Modifier = Modifier
) {
    val colors = when (type) {
        FeedbackType.INFO -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
        FeedbackType.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
        FeedbackType.ERROR -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    }
    val icon: ImageVector = when (type) {
        FeedbackType.INFO -> Icons.Rounded.Info
        FeedbackType.SUCCESS -> Icons.Rounded.CheckCircle
        FeedbackType.ERROR -> Icons.Rounded.Error
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.first,
        contentColor = colors.second
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
