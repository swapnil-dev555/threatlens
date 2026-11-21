package com.example.threatlens.ui.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threatlens.R
import com.example.threatlens.ui.components.EmptyScreen
import com.example.threatlens.ui.components.ErrorScreen
import com.example.threatlens.ui.components.LoadingScreen
import com.example.threatlens.ui.components.NewsCardItem
import com.example.threatlens.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionDetailScreen(
    onBack: () -> Unit,
    onStoryClick: (subscriptionId: String, activityKey: String) -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: SubscriptionDetailViewModel = hiltViewModel()
) {
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isMatchesExpanded by remember { mutableStateOf(false) }
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)
    val whiteColor = colorResource(R.color.white)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = ThreatLensSurfaceElevated,
            title = {
                Text(
                    stringResource(R.string.delete_subscription_dialog_title),
                    color = ThreatLensTextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.delete_subscription_dialog_message,
                        uiState.subscription?.name ?: ""
                    ),
                    color = ThreatLensTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSubscription(onDeleted = onBack)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThreatLensHighSeverity,
                        contentColor = whiteColor
                    ),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card))
                ) {
                    Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                    border = BorderStroke(
                        dimensionResource(R.dimen.border_stroke_normal),
                        ThreatLensCardBorder
                    )
                ) {
                    Text(stringResource(R.string.action_cancel), color = ThreatLensTextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = ThreatLensBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
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
                },
                actions = {
                    // Replaced 3-dot menu with an explicit Edit button with text
                    FilledTonalButton(
                        onClick = {
                            val id = uiState.subscription?.id ?: viewModel.subscriptionId
                            if (id.isNotBlank()) onEditClick(id)
                        },
                        enabled = uiState.subscription != null,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_button)),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ThreatLensCyan.copy(alpha = 0.15f),
                            contentColor = ThreatLensCyan
                        ),
                        contentPadding = PaddingValues(
                            horizontal = dimensionResource(R.dimen.dimen_14dp),
                            vertical = dimensionResource(R.dimen.dimen_6dp)
                        ),
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.dimen_12dp))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.dimen_16dp))
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_6dp)))
                        Text(
                            text = stringResource(R.string.action_edit),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = textDimensionResource(R.dimen.font_size_13sp)
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThreatLensBackground)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Both are loading initially
                uiState.isLoadingSubscription && uiState.isLoadingMatches && uiState.subscription == null && uiState.matches.isEmpty() -> {
                    LoadingScreen(message = stringResource(R.string.loading_sub_and_matches))
                }

                // Both failed
                uiState.subscription == null && !uiState.isLoadingSubscription && uiState.subscriptionError != null &&
                        uiState.matches.isEmpty() && !uiState.isLoadingMatches && uiState.matchesError != null -> {
                    ErrorScreen(
                        message = stringResource(R.string.failed_load_details_and_matches),
                        subMessage = stringResource(
                            R.string.subscription_and_matches_error_sub,
                            uiState.subscriptionError ?: "",
                            uiState.matchesError ?: ""
                        ),
                        onRetry = viewModel::retry
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = dimensionResource(R.dimen.dimen_16dp),
                            vertical = dimensionResource(R.dimen.dimen_8dp)
                        ),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_16dp))
                    ) {
                        // ── Top Subscription Section (or error/loading banner) ──
                        when {
                            uiState.isLoadingSubscription && uiState.subscription == null -> {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_hero)),
                                        colors = CardDefaults.cardColors(containerColor = ThreatLensSurfaceElevated),
                                        border = BorderStroke(
                                            dimensionResource(R.dimen.border_stroke_normal),
                                            ThreatLensCardBorder
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(dimensionResource(R.dimen.dimen_140dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = ThreatLensCyan,
                                                strokeWidth = dimensionResource(R.dimen.dimen_2dp)
                                            )
                                        }
                                    }
                                }
                            }

                            uiState.subscriptionError != null && uiState.subscription == null -> {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card_large)),
                                        colors = CardDefaults.cardColors(containerColor = ThreatLensErrorContainer),
                                        border = BorderStroke(
                                            dimensionResource(R.dimen.border_stroke_normal),
                                            ThreatLensError.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(dimensionResource(R.dimen.dimen_16dp)),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.subscription_details_unavailable),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = ThreatLensOnErrorContainer
                                                )
                                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_4dp)))
                                                Text(
                                                    text = uiState.subscriptionError ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ThreatLensOnErrorContainer.copy(alpha = 0.85f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_12dp)))
                                            Button(
                                                onClick = viewModel::retrySubscription,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ThreatLensCyan,
                                                    contentColor = darkTealButtonText
                                                ),
                                                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)),
                                                modifier = Modifier.height(dimensionResource(R.dimen.dimen_36dp))
                                            ) {
                                                Text(
                                                    stringResource(R.string.action_retry),
                                                    fontSize = textDimensionResource(R.dimen.font_size_12sp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            uiState.subscription != null -> {
                                val sub = uiState.subscription!!
                                val isActive = !sub.isPaused

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_hero)),
                                        colors = CardDefaults.cardColors(containerColor = ThreatLensSurfaceElevated),
                                        border = BorderStroke(
                                            dimensionResource(R.dimen.border_stroke_normal),
                                            ThreatLensCardBorder
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(dimensionResource(R.dimen.dimen_20dp)),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_10dp))
                                        ) {
                                            // Shield Icon Glowing Badge
                                            Box(
                                                modifier = Modifier
                                                    .size(dimensionResource(R.dimen.dimen_64dp))
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.radialGradient(
                                                            colors = listOf(
                                                                ThreatLensCyan.copy(alpha = 0.35f),
                                                                ThreatLensCyan.copy(alpha = 0.05f)
                                                            )
                                                        )
                                                    )
                                                    .border(
                                                        dimensionResource(R.dimen.border_stroke_medium),
                                                        ThreatLensCyan,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Shield,
                                                    contentDescription = null,
                                                    tint = ThreatLensCyan,
                                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_32dp))
                                                )
                                            }

                                            // Subscription Name
                                            Text(
                                                text = sub.name,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = textDimensionResource(R.dimen.font_size_19sp)
                                                ),
                                                color = ThreatLensTextPrimary,
                                                textAlign = TextAlign.Center
                                            )

                                            // Active / Paused Pill Badge
                                            Surface(
                                                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)),
                                                color = if (isActive) ThreatLensActiveGreenBg else ThreatLensSurfaceHighlight,
                                                border = BorderStroke(
                                                    dimensionResource(R.dimen.border_stroke_normal),
                                                    if (isActive) ThreatLensActiveGreen.copy(alpha = 0.5f) else ThreatLensCardBorder
                                                )
                                            ) {
                                                Text(
                                                    text = if (isActive) stringResource(R.string.status_active) else stringResource(R.string.status_paused),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = textDimensionResource(R.dimen.font_size_11sp)
                                                    ),
                                                    color = if (isActive) ThreatLensActiveGreen else ThreatLensTextSecondary,
                                                    modifier = Modifier.padding(
                                                        horizontal = dimensionResource(R.dimen.dimen_12dp),
                                                        vertical = dimensionResource(R.dimen.dimen_4dp)
                                                    )
                                                )
                                            }

                                            // Schedule details
                                            Text(
                                                text = sub.cadenceSummary(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ThreatLensTextSecondary
                                            )
                                            Text(
                                                text = sub.nextFireFormatted(),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = ThreatLensCyan
                                            )
                                        }
                                    }
                                }

                                // ── Configured Filter Tags ──
                                val allCriteria = buildList {
                                    addAll(sub.categories.map { "Category: $it" })
                                    addAll(sub.ransomwareFamilies.map { "Ransomware: $it" })
                                    addAll(sub.actors.map { "Actor: $it" })
                                    addAll(sub.tools.map { "Tool: $it" })
                                    addAll(sub.techniques.map { "Technique: $it" })
                                    addAll(sub.vendors.map { "Vendor: $it" })
                                    addAll(sub.products.map { "Product: $it" })
                                    addAll(sub.targetSectors.map { "Sector: $it" })
                                    addAll(sub.targetCountries.map { "Country: $it" })
                                }

                                if (allCriteria.isNotEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                                            colors = CardDefaults.cardColors(containerColor = ThreatLensSurfaceElevated),
                                            border = BorderStroke(
                                                dimensionResource(R.dimen.border_stroke_normal),
                                                ThreatLensCardBorder
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(dimensionResource(R.dimen.dimen_16dp)),
                                                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_10dp))
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.monitored_intel_criteria),
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = ThreatLensTextPrimary
                                                )
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp)),
                                                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))
                                                ) {
                                                    allCriteria.forEach { crit ->
                                                        Surface(
                                                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small)),
                                                            color = ThreatLensSurfaceHighlight.copy(alpha = 0.5f),
                                                            border = BorderStroke(
                                                                dimensionResource(R.dimen.border_stroke_thin),
                                                                ThreatLensCardBorder
                                                            )
                                                        ) {
                                                            Text(
                                                                text = crit,
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = textDimensionResource(R.dimen.font_size_11sp)
                                                                ),
                                                                color = ThreatLensCyan,
                                                                modifier = Modifier.padding(
                                                                    horizontal = dimensionResource(R.dimen.dimen_8dp),
                                                                    vertical = dimensionResource(R.dimen.dimen_4dp)
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Current Matches Section Header ──
                        val totalMatches = uiState.matches.size
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = dimensionResource(R.dimen.dimen_4dp)),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.current_matches_count, totalMatches),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = textDimensionResource(R.dimen.font_size_16sp)
                                    ),
                                    color = ThreatLensTextPrimary
                                )

                                if (totalMatches > 5) {
                                    Text(
                                        text = if (isMatchesExpanded) {
                                            stringResource(R.string.action_show_less)
                                        } else {
                                            stringResource(R.string.action_view_all_arrow, totalMatches)
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ThreatLensCyan,
                                        modifier = Modifier.clickable {
                                            isMatchesExpanded = !isMatchesExpanded
                                        }
                                    )
                                }
                            }
                        }

                        // ── Matches List / Empty State / Retry ──
                        when {
                            uiState.isLoadingMatches -> item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(dimensionResource(R.dimen.dimen_100dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = ThreatLensCyan,
                                        strokeWidth = dimensionResource(R.dimen.dimen_2dp)
                                    )
                                }
                            }

                            uiState.matchesError != null -> item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ThreatLensErrorContainer),
                                    border = BorderStroke(
                                        dimensionResource(R.dimen.border_stroke_normal),
                                        ThreatLensError.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(dimensionResource(R.dimen.dimen_14dp)),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.could_not_load_matches,
                                                uiState.matchesError ?: ""
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThreatLensOnErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = viewModel::retryMatches,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ThreatLensCyan,
                                                contentColor = darkTealButtonText
                                            ),
                                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_button)),
                                            modifier = Modifier.height(dimensionResource(R.dimen.dimen_36dp))
                                        ) {
                                            Text(
                                                stringResource(R.string.action_retry),
                                                fontSize = textDimensionResource(R.dimen.font_size_12sp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            uiState.matches.isEmpty() -> item {
                                EmptyScreen(
                                    message = stringResource(R.string.empty_matches_title),
                                    subMessage = stringResource(R.string.empty_matches_subtitle),
                                    onReset = viewModel::retryMatches
                                )
                            }

                            else -> {
                                val displayedMatches = if (isMatchesExpanded || totalMatches <= 5) {
                                    uiState.matches
                                } else {
                                    uiState.matches.take(5)
                                }

                                items(displayedMatches, key = { it.activityKey }) { card ->
                                    NewsCardItem(
                                        card = card,
                                        onClick = { onStoryClick(viewModel.subscriptionId, card.activityKey) }
                                    )
                                }

                                if (totalMatches > 5 && !isMatchesExpanded) {
                                    item {
                                        OutlinedButton(
                                            onClick = { isMatchesExpanded = true },
                                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                                            border = BorderStroke(
                                                dimensionResource(R.dimen.border_stroke_normal),
                                                ThreatLensCyan.copy(alpha = 0.4f)
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = ThreatLensCyan.copy(alpha = 0.05f),
                                                contentColor = ThreatLensCyan
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(dimensionResource(R.dimen.dimen_46dp))
                                        ) {
                                            Text(
                                                stringResource(R.string.action_view_all_matches, totalMatches),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = textDimensionResource(R.dimen.font_size_13sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Last Updated Row ──
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = dimensionResource(R.dimen.dimen_4dp)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.last_updated_placeholder),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThreatLensTextTertiary
                                )
                                IconButton(
                                    onClick = viewModel::retry,
                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_28dp))
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.refresh_matches),
                                        tint = ThreatLensCyan,
                                        modifier = Modifier.size(dimensionResource(R.dimen.dimen_16dp))
                                    )
                                }
                            }
                        }

                        // ── Action Buttons Row (Pause / Resume and Delete) ──
                        if (uiState.subscription != null) {
                            val sub = uiState.subscription!!
                            val isActive = !sub.isPaused
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = dimensionResource(R.dimen.dimen_4dp),
                                            bottom = dimensionResource(R.dimen.dimen_24dp)
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_12dp))
                                ) {
                                    // Pause / Resume Button
                                    OutlinedButton(
                                        onClick = viewModel::togglePause,
                                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                                        border = BorderStroke(
                                            dimensionResource(R.dimen.border_stroke_normal),
                                            ThreatLensCyan.copy(alpha = 0.5f)
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = ThreatLensCyan.copy(alpha = 0.08f),
                                            contentColor = ThreatLensCyan
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(dimensionResource(R.dimen.dimen_48dp))
                                    ) {
                                        Icon(
                                            imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(dimensionResource(R.dimen.dimen_18dp))
                                        )
                                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_8dp)))
                                        Text(
                                            text = if (isActive) stringResource(R.string.action_pause) else stringResource(R.string.action_resume),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Delete Button
                                    OutlinedButton(
                                        onClick = { showDeleteDialog = true },
                                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                                        border = BorderStroke(
                                            dimensionResource(R.dimen.border_stroke_normal),
                                            ThreatLensHighSeverity.copy(alpha = 0.5f)
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = ThreatLensHighSeverity.copy(alpha = 0.08f),
                                            contentColor = ThreatLensHighSeverity
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(dimensionResource(R.dimen.dimen_48dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(dimensionResource(R.dimen.dimen_18dp))
                                        )
                                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_8dp)))
                                        Text(
                                            text = stringResource(R.string.action_delete),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
