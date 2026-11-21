package com.example.threatlens.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.threatlens.R
import com.example.threatlens.common.AppConstants
import com.example.threatlens.ui.components.ThreatLensLogo
import com.example.threatlens.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(AppConstants.SPLASH_DELAY_MS.milliseconds)
        onSplashComplete()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppConstants.SPLASH_CONTENT_ANIM_MS,
            easing = FastOutSlowInEasing
        ),
        label = "contentAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AppConstants.SPLASH_PULSE_ANIM_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(AppConstants.SPLASH_PULSE_ANIM_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AppConstants.SPLASH_SCAN_ANIM_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanOffset"
    )

    val cyanColor = ThreatLensCyan
    val pulseRingColor = cyanColor.copy(alpha = pulseAlpha * 0.35f)
    val secondRingAlpha = ((pulseRadius + 0.5f) % 1f).let { (1f - it) * 0.3f }
    val secondRingColor = cyanColor.copy(alpha = secondRingAlpha)

    val radarStroke1 = dimensionResource(R.dimen.dimen_2dp)
    val radarStroke2 = dimensionResource(R.dimen.border_stroke_medium)
    val radarCenterOffset = dimensionResource(R.dimen.dimen_40dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ThreatLensBackground),
        contentAlignment = Alignment.Center
    ) {
        // Subtle cyber grid/radar pulse background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f - radarCenterOffset.toPx())
            val maxRadius = size.width * 0.45f

            // Radar pulse ring 1
            drawCircle(
                color = pulseRingColor,
                radius = maxRadius * pulseRadius,
                center = center,
                style = Stroke(width = radarStroke1.toPx())
            )

            // Radar pulse ring 2 (offset)
            val secondPulse = (pulseRadius + 0.5f) % 1f
            drawCircle(
                color = secondRingColor,
                radius = maxRadius * secondPulse,
                center = center,
                style = Stroke(width = radarStroke2.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.dimen_32dp))
        ) {
            // Animated Logo
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(contentAlpha),
                contentAlignment = Alignment.Center
            ) {
                ThreatLensLogo(size = dimensionResource(R.dimen.dimen_96dp))
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_28dp)))

            // Brand Title with subtle glowing tracking
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = textDimensionResource(R.dimen.font_size_32sp),
                    letterSpacing = textDimensionResource(R.dimen.letter_spacing_1_5sp)
                ),
                color = ThreatLensTextPrimary,
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_8dp)))

            // Subtitle
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = textDimensionResource(R.dimen.letter_spacing_0_5sp)
                ),
                color = ThreatLensTextSecondary,
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_48dp)))

            // Futuristic glowing scanning bar
            val scanBarWidth = dimensionResource(R.dimen.dimen_180dp)
            val scanBarHeight = dimensionResource(R.dimen.dimen_3dp)
            val scanBarCorner = dimensionResource(R.dimen.corner_radius_badge)
            val scanPuckWidth = dimensionResource(R.dimen.dimen_60dp)
            val scanOffsetRange = dimensionResource(R.dimen.dimen_140dp) - dimensionResource(R.dimen.dimen_20dp) // ~120dp

            Box(
                modifier = Modifier
                    .width(scanBarWidth)
                    .height(scanBarHeight)
                    .alpha(contentAlpha)
                    .background(
                        ThreatLensSurfaceHighlight,
                        shape = RoundedCornerShape(scanBarCorner)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(scanPuckWidth)
                        .offset(x = scanOffsetRange * scanOffset)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    ThreatLensCyan,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}
