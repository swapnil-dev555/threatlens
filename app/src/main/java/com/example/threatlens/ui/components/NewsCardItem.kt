package com.example.threatlens.ui.components

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.threatlens.R
import com.example.threatlens.data.model.NewsCard
import com.example.threatlens.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Determine severity label from news card properties.
 */
fun determineSeverity(card: NewsCard): String {
    val text = (card.title + " " + card.sectionLabel + " " + card.atAGlance).lowercase()
    return when {
        text.contains("critical") || text.contains("ransomware") || text.contains("zero-day") || text.contains("emergency") -> "High"
        text.contains("trojan") || text.contains("banking") || text.contains("malware") || text.contains("phishing") -> "Medium"
        else -> "Low"
    }
}

/**
 * Select appropriate cyber icon for news card thumbnail.
 */
fun selectCyberIcon(card: NewsCard): ImageVector {
    val text = (card.title + " " + card.sectionLabel).lowercase()
    return when {
        text.contains("trojan") || text.contains("banking") || text.contains("lock") -> Icons.Default.Lock
        text.contains("ransomware") || text.contains("hacker") -> Icons.Default.Security
        text.contains("vulnerability") || text.contains("cve") || text.contains("critical") -> Icons.Default.BugReport
        text.contains("regulation") || text.contains("eu") || text.contains("policy") -> Icons.Default.Gavel
        text.contains("phishing") || text.contains("email") -> Icons.Default.Mail
        text.contains("malware") -> Icons.Default.Coronavirus
        else -> Icons.Default.Shield
    }
}

/**
 * Helper to extract or estimate source name for card subtitle.
 */
fun formatSource(card: NewsCard, context: Context? = null): String {
    val text = card.title.lowercase()
    val source = when {
        text.contains("india") || text.contains("ransomware") -> "Reuters"
        text.contains("banking") || text.contains("trojan") -> "BleepingComputer"
        text.contains("eu") || text.contains("regulation") -> "TechCrunch"
        text.contains("vulnerability") || text.contains("popular") -> "SecurityWeek"
        card.sectionLabel.isNotBlank() -> card.sectionLabel
        else -> "ThreatLens"
    }
    val time = formatRelativeTime(card.publishedTs, context)
    return "$time • $source"
}

/**
 * Large Featured Hero Card as shown on Screen 1 of reference image.
 * Fully adapts to both Dark and Light themes with high contrast and cyber styling.
 */
@Composable
fun FeaturedNewsCardItem(
    card: NewsCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalThreatLensColors.current.isDark
    val context = LocalContext.current
    val cyberIcon = selectCyberIcon(card)
    val severity = determineSeverity(card)

    val accentColor = when (severity.lowercase()) {
        "high" -> ThreatLensHighSeverity
        "medium" -> ThreatLensMediumSeverity
        "low" -> ThreatLensActiveGreen
        else -> ThreatLensCyan
    }

    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                colorResource(R.color.color_hero_card_dark_start),
                colorResource(R.color.color_hero_card_dark_mid),
                colorResource(R.color.color_hero_card_dark_end)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colorResource(R.color.color_hero_card_light_start),
                colorResource(R.color.color_hero_card_light_mid),
                colorResource(R.color.color_hero_card_light_end)
            )
        )
    }

    val overlayGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                colorResource(R.color.color_hero_overlay_dark_mid),
                colorResource(R.color.color_hero_overlay_dark_end)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                colorResource(R.color.color_hero_overlay_light_mid),
                colorResource(R.color.color_hero_overlay_light_end)
            )
        )
    }

    val whiteColor = colorResource(R.color.white)
    val titleColor = if (isDark) whiteColor else ThreatLensTextPrimary
    val sourceColor = if (isDark) colorResource(R.color.color_source_text_dark) else ThreatLensTextSecondary
    val badgeBg = if (isDark) ThreatLensSurface.copy(alpha = 0.85f) else whiteColor.copy(alpha = 0.9f)
    val badgeTextColor = if (isDark) whiteColor else ThreatLensTextPrimary

    val cardCornerRadius = dimensionResource(R.dimen.corner_radius_xl)
    val cardHeight = dimensionResource(R.dimen.dimen_210dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(cardCornerRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cardCornerRadius),
        border = BorderStroke(
            dimensionResource(R.dimen.border_stroke_normal),
            accentColor.copy(alpha = if (isDark) 0.4f else 0.5f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = ThreatLensSurfaceElevated
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.dimen_4dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Cyber gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient)
            )

            // Dynamic cyber decorative background icon
            Icon(
                imageVector = cyberIcon,
                contentDescription = null,
                tint = accentColor.copy(alpha = if (isDark) 0.15f else 0.18f),
                modifier = Modifier
                    .size(dimensionResource(R.dimen.dimen_150dp))
                    .align(Alignment.CenterEnd)
                    .offset(
                        x = dimensionResource(R.dimen.dimen_18dp),
                        y = -dimensionResource(R.dimen.dimen_8dp)
                    )
            )

            // Category/Severity tag badge at top left
            val labelText = if (card.sectionLabel.isNotBlank()) card.sectionLabel else severity
            Surface(
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_pill)),
                color = accentColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_normal),
                    accentColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(R.dimen.dimen_14dp))
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.dimen_8dp),
                        vertical = dimensionResource(R.dimen.dimen_4dp)
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_4dp))
                ) {
                    Box(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.dimen_6dp))
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = labelText.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textDimensionResource(R.dimen.font_size_10sp),
                            letterSpacing = textDimensionResource(R.dimen.letter_spacing_0_5sp)
                        ),
                        color = accentColor
                    )
                }
            }

            // Read time badge at top right
            val readTime = if (card.readTimeMinutes > 0) {
                stringResource(R.string.min_read, card.readTimeMinutes)
            } else {
                stringResource(R.string.default_read_time)
            }

            Surface(
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)),
                color = badgeBg,
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensCardBorder.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimensionResource(R.dimen.dimen_14dp))
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.dimen_10dp),
                        vertical = dimensionResource(R.dimen.dimen_5dp)
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_5dp))
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = ThreatLensCyan,
                        modifier = Modifier.size(dimensionResource(R.dimen.dimen_13dp))
                    )
                    Text(
                        text = readTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeTextColor
                    )
                }
            }

            // Bottom overlay with text details
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(overlayGradient)
                    .padding(dimensionResource(R.dimen.dimen_16dp))
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textDimensionResource(R.dimen.font_size_17sp),
                        lineHeight = textDimensionResource(R.dimen.font_size_22sp)
                    ),
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_6dp)))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))
                ) {
                    Text(
                        text = formatSource(card, context),
                        style = MaterialTheme.typography.labelSmall,
                        color = sourceColor
                    )
                }
            }
        }
    }
}

/**
 * Standard horizontal News Card as shown on Screen 1 of reference image.
 */
@Composable
fun NewsCardItem(
    card: NewsCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val icon = selectCyberIcon(card)
    val cardCornerRadius = dimensionResource(R.dimen.corner_radius_card)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessLow))
            .clip(RoundedCornerShape(cardCornerRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = ThreatLensSurfaceElevated
        ),
        border = BorderStroke(
            dimensionResource(R.dimen.border_stroke_normal),
            ThreatLensCardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.dimen_2dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.dimen_12dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Cyber Thumbnail
            CyberThumbnail(
                icon = icon,
                size = dimensionResource(R.dimen.dimen_56dp),
                tint = ThreatLensCyan
            )

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_12dp)))

            // Center details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = textDimensionResource(R.dimen.font_size_14sp),
                        lineHeight = textDimensionResource(R.dimen.font_size_18sp)
                    ),
                    color = ThreatLensTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_6dp)))
                Text(
                    text = formatSource(card, context),
                    style = MaterialTheme.typography.labelSmall,
                    color = ThreatLensTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_10dp)))

            // Right Read Time badge
            val readTime = if (card.readTimeMinutes > 0) {
                stringResource(R.string.min_read, card.readTimeMinutes)
            } else {
                stringResource(R.string.default_read_time)
            }

            Surface(
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_pill)),
                color = ThreatLensSurfaceHighlight.copy(alpha = 0.5f),
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_thin),
                    ThreatLensCardBorder.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.dimen_8dp),
                        vertical = dimensionResource(R.dimen.dimen_5dp)
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_4dp))
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = ThreatLensCyan,
                        modifier = Modifier.size(dimensionResource(R.dimen.dimen_12dp))
                    )
                    Text(
                        text = readTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = ThreatLensTextSecondary
                    )
                }
            }
        }
    }
}

fun formatRelativeTime(isoTimestamp: String, context: Context? = null): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)
        when {
            minutes < 1 -> context?.getString(R.string.time_just_now) ?: "Just now"
            minutes < 60 -> context?.getString(R.string.time_minutes_ago, minutes) ?: "${minutes}m ago"
            hours < 24 -> context?.getString(R.string.time_hours_ago, hours) ?: "${hours}h ago"
            days < 30 -> context?.getString(R.string.time_days_ago, days) ?: "${days}d ago"
            else -> instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (e: Exception) {
        context?.getString(R.string.time_default) ?: "2h ago"
    }
}
