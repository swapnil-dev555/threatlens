package com.example.threatlens.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.example.threatlens.R
import com.example.threatlens.ui.theme.*

/**
 * ThreatLens Cyan Shield Logo with central cyber iris/eye.
 */
@Composable
fun ThreatLensLogo(
    modifier: Modifier = Modifier,
    size: Dp = dimensionResource(R.dimen.dimen_32dp),
    tint: Color = ThreatLensCyan
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Shield outline path
            val shieldPath = Path().apply {
                moveTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.88f, h * 0.24f)
                lineTo(w * 0.88f, h * 0.60f)
                cubicTo(w * 0.88f, h * 0.82f, w * 0.5f, h * 0.95f, w * 0.5f, h * 0.95f)
                cubicTo(w * 0.5f, h * 0.95f, w * 0.12f, h * 0.82f, w * 0.12f, h * 0.60f)
                lineTo(w * 0.12f, h * 0.24f)
                close()
            }

            // Fill with dark glowing gradient
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(tint.copy(alpha = 0.35f), tint.copy(alpha = 0.10f))
                )
            )

            // Outer Shield stroke
            drawPath(
                path = shieldPath,
                color = tint,
                style = Stroke(width = w * 0.07f)
            )

            // Inner Eye / Iris
            val eyeCenter = Offset(w * 0.5f, h * 0.52f)
            val eyeRadius = w * 0.22f
            drawCircle(
                color = tint,
                radius = eyeRadius,
                center = eyeCenter,
                style = Stroke(width = w * 0.06f)
            )
            drawCircle(
                color = tint,
                radius = w * 0.10f,
                center = eyeCenter
            )
        }
    }
}

/**
 * Severity Chip: High (Red), Medium (Blue/Cyan), Low (Green).
 */
@Composable
fun SeverityBadge(
    severity: String,
    modifier: Modifier = Modifier
) {
    val norm = severity.trim().lowercase()
    val whiteColor = colorResource(R.color.white)
    val lowDarkTextColor = colorResource(R.color.color_low_severity_dark_text)

    val (bgColor, textColor) = when {
        norm == "high" || norm.contains("critical") -> ThreatLensHighSeverity to whiteColor
        norm == "medium" || norm.contains("mod") -> ThreatLensMediumSeverity to whiteColor
        norm == "low" -> ThreatLensLowSeverity to lowDarkTextColor
        else -> ThreatLensMediumSeverity to whiteColor
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)),
        color = bgColor
    ) {
        Text(
            text = severity.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = textDimensionResource(R.dimen.font_size_11sp)
            ),
            color = textColor,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.dimen_8dp),
                vertical = dimensionResource(R.dimen.dimen_2dp)
            )
        )
    }
}

/**
 * Cyber Thumbnail box for news cards with subtle grid/cyber glow.
 */
@Composable
fun CyberThumbnail(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = dimensionResource(R.dimen.dimen_68dp),
    tint: Color = ThreatLensCyan
) {
    val cornerRadius = dimensionResource(R.dimen.corner_radius_button)
    val borderStroke = dimensionResource(R.dimen.border_stroke_normal)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ThreatLensSurfaceHighlight,
                        ThreatLensSurface
                    )
                )
            )
            .border(borderStroke, ThreatLensCardBorder, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

/**
 * Loading state matching the reference image.
 */
@Composable
fun LoadingScreen(
    message: String = stringResource(R.string.loading_latest_news),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThreatLensBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = ThreatLensCyan,
            strokeWidth = dimensionResource(R.dimen.dimen_3dp),
            modifier = Modifier.size(dimensionResource(R.dimen.dimen_48dp))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_20dp)))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = ThreatLensTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Error state matching the reference image with Retry button.
 */
@Composable
fun ErrorScreen(
    message: String = stringResource(R.string.error_generic_title),
    subMessage: String = stringResource(R.string.error_generic_subtitle),
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThreatLensBackground)
            .padding(dimensionResource(R.dimen.dimen_32dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.dimen_72dp))
                .clip(CircleShape)
                .background(ThreatLensHighSeverity.copy(alpha = 0.15f))
                .border(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensHighSeverity.copy(alpha = 0.4f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.dimen_36dp)),
                tint = ThreatLensHighSeverity
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_20dp)))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ThreatLensTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_8dp)))
        Text(
            text = subMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = ThreatLensTextSecondary,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_24dp)))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThreatLensCyan,
                    contentColor = darkTealButtonText
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_full)),
                modifier = Modifier
                    .width(dimensionResource(R.dimen.dimen_140dp))
                    .height(dimensionResource(R.dimen.dimen_44dp))
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_16dp))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_8dp)))
                Text(
                    stringResource(R.string.action_retry),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Empty state matching the reference image with Reset Filters button.
 */
@Composable
fun EmptyScreen(
    message: String = stringResource(R.string.empty_news_title),
    subMessage: String = stringResource(R.string.empty_news_subtitle),
    onReset: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThreatLensBackground)
            .padding(dimensionResource(R.dimen.dimen_32dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.dimen_72dp))
                .clip(CircleShape)
                .background(ThreatLensCyan.copy(alpha = 0.12f))
                .border(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensCyan.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentPasteSearch,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.dimen_36dp)),
                tint = ThreatLensCyan
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_20dp)))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ThreatLensTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_8dp)))
        Text(
            text = subMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = ThreatLensTextSecondary,
            textAlign = TextAlign.Center
        )
        if (onReset != null) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_24dp)))
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThreatLensCyan,
                    contentColor = darkTealButtonText
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_full)),
                modifier = Modifier
                    .width(dimensionResource(R.dimen.dimen_160dp))
                    .height(dimensionResource(R.dimen.dimen_44dp))
            ) {
                Text(
                    stringResource(R.string.action_reset_filters),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Notification Permission Dialog matching the reference image.
 */
@Composable
fun NotificationPermissionDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThreatLensSurfaceElevated,
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_full)),
        icon = {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.dimen_56dp))
                    .clip(CircleShape)
                    .background(ThreatLensCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = ThreatLensCyan,
                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_30dp))
                )
            }
        },
        title = {
            Text(
                stringResource(R.string.permission_dialog_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = ThreatLensTextPrimary
            )
        },
        text = {
            Text(
                stringResource(R.string.permission_dialog_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = ThreatLensTextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onAllow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThreatLensCyan,
                    contentColor = darkTealButtonText
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                modifier = Modifier.height(dimensionResource(R.dimen.dimen_40dp))
            ) {
                Text(stringResource(R.string.action_allow), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensCardBorder
                ),
                modifier = Modifier.height(dimensionResource(R.dimen.dimen_40dp))
            ) {
                Text(stringResource(R.string.action_dont_allow), color = ThreatLensTextSecondary)
            }
        }
    )
}

/**
 * Offline indicator strip.
 */
@Composable
fun OfflineIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(300)) + fadeIn(),
        exit = fadeOut(tween(300)),
        modifier = modifier
    ) {
        Surface(
            color = ThreatLensErrorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.dimen_16dp),
                    vertical = dimensionResource(R.dimen.dimen_8dp)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = ThreatLensOnErrorContainer,
                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_16dp))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_8dp)))
                Text(
                    stringResource(R.string.offline_indicator_text),
                    style = MaterialTheme.typography.labelMedium,
                    color = ThreatLensOnErrorContainer
                )
            }
        }
    }
}
