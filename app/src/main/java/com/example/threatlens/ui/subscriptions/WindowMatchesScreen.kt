package com.example.threatlens.ui.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threatlens.R
import com.example.threatlens.ui.components.EmptyScreen
import com.example.threatlens.ui.components.ErrorScreen
import com.example.threatlens.ui.components.LoadingScreen
import com.example.threatlens.ui.components.NewsCardItem
import com.example.threatlens.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowMatchesScreen(
    onBack: (() -> Unit)? = null,
    onStoryClick: (subscriptionId: String, activityKey: String) -> Unit,
    onViewSubscription: ((String) -> Unit)? = null,
    viewModel: WindowMatchesViewModel = hiltViewModel()
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)

    Scaffold(
        containerColor = ThreatLensBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_push_notifications),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = ThreatLensTextPrimary
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = ThreatLensSurfaceElevated,
                                contentColor = ThreatLensTextPrimary
                            ),
                            modifier = Modifier
                                .padding(start = dimensionResource(R.dimen.dimen_8dp))
                                .size(dimensionResource(R.dimen.dimen_38dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_18dp))
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThreatLensBackground)
            )
        },
        bottomBar = {
            if (onViewSubscription != null) {
                Surface(
                    color = ThreatLensBackground,
                    border = BorderStroke(
                        dimensionResource(R.dimen.border_stroke_normal),
                        ThreatLensCardBorder.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = dimensionResource(R.dimen.dimen_20dp),
                                vertical = dimensionResource(R.dimen.dimen_12dp)
                            )
                    ) {
                        Button(
                            onClick = { onViewSubscription(viewModel.subscriptionId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThreatLensCyan,
                                contentColor = darkTealButtonText
                            ),
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_full)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensionResource(R.dimen.dimen_50dp))
                        ) {
                            Text(
                                stringResource(R.string.action_view_subscription),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingScreen(message = stringResource(R.string.loading_push_matches))

                uiState.error != null -> ErrorScreen(
                    message = stringResource(R.string.error_generic_title),
                    subMessage = uiState.error ?: stringResource(R.string.error_failed_to_load_push),
                    onRetry = viewModel::retry
                )

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = dimensionResource(R.dimen.dimen_16dp),
                            vertical = dimensionResource(R.dimen.dimen_10dp)
                        ),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_14dp))
                    ) {
                        // ── Simulated Notification Banner Card matching Screen 6 ──
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card_large)),
                                colors = CardDefaults.cardColors(containerColor = ThreatLensSurfaceElevated),
                                border = BorderStroke(
                                    dimensionResource(R.dimen.border_stroke_normal),
                                    ThreatLensCyan.copy(alpha = 0.4f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.dimen_3dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(dimensionResource(R.dimen.dimen_16dp)),
                                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_10dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(dimensionResource(R.dimen.dimen_32dp))
                                                    .clip(CircleShape)
                                                    .background(ThreatLensCyan.copy(alpha = 0.18f))
                                                    .border(
                                                        dimensionResource(R.dimen.border_stroke_normal),
                                                        ThreatLensCyan,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = ThreatLensCyan,
                                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_16dp))
                                                )
                                            }
                                            Text(
                                                text = stringResource(R.string.app_name),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ThreatLensTextPrimary
                                            )
                                        }

                                        Text(
                                            text = stringResource(R.string.push_simulated_time),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ThreatLensTextSecondary
                                        )
                                    }

                                    Text(
                                        text = stringResource(
                                            R.string.push_match_summary,
                                            uiState.matches.size.coerceAtLeast(3)
                                        ),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = ThreatLensTextPrimary
                                    )
                                }
                            }
                        }

                        // ── Matched Stories Header ──
                        item {
                            Text(
                                text = stringResource(R.string.title_matched_stories_count, uiState.matches.size),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = textDimensionResource(R.dimen.font_size_16sp)
                                ),
                                color = ThreatLensTextPrimary,
                                modifier = Modifier.padding(
                                    start = dimensionResource(R.dimen.dimen_4dp),
                                    top = dimensionResource(R.dimen.dimen_4dp)
                                )
                            )
                        }

                        if (uiState.matches.isEmpty()) {
                            item {
                                EmptyScreen(
                                    message = stringResource(R.string.no_stories_in_window),
                                    subMessage = stringResource(R.string.no_stories_in_window_sub),
                                    onReset = viewModel::retry
                                )
                            }
                        } else {
                            items(uiState.matches, key = { it.activityKey }) { card ->
                                NewsCardItem(
                                    card = card,
                                    onClick = { onStoryClick(viewModel.subscriptionId, card.activityKey) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_16dp))) }
                    }
                }
            }
        }
    }
}
