package com.example.threatlens.ui.subscriptions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threatlens.R
import com.example.threatlens.data.fcm.NotificationHelper
import com.example.threatlens.data.model.Subscription
import com.example.threatlens.ui.components.EmptyScreen
import com.example.threatlens.ui.components.ErrorScreen
import com.example.threatlens.ui.components.LoadingScreen
import com.example.threatlens.ui.theme.*
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Assign appropriate icons to subscriptions based on title or index.
 */
fun getSubscriptionIcon(subscription: Subscription, index: Int): ImageVector {
    val name = subscription.name.lowercase()
    return when {
        name.contains("health") || name.contains("hospital") -> Icons.Default.LocalHospital
        name.contains("bank") || name.contains("finance") -> Icons.Default.Laptop
        name.contains("global") || name.contains("world") -> Icons.Default.Public
        name.contains("attack") || name.contains("high") -> Icons.Default.Security
        index % 4 == 0 -> Icons.Default.LocalHospital
        index % 4 == 1 -> Icons.Default.Laptop
        index % 4 == 2 -> Icons.Default.Public
        else -> Icons.Default.Shield
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsListScreen(
    onBack: (() -> Unit)? = null,
    onSubscriptionClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: SubscriptionsListViewModel = hiltViewModel()
) {
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED &&
                        NotificationManagerCompat.from(context).areNotificationsEnabled()
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }
    var isBannerDismissed by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        FirebaseCrashlytics.getInstance().setCustomKey("notification_permission_list_granted", isGranted)
    }

    val undoLabel = stringResource(R.string.action_undo)

    // Undo delete snackbar
    LaunchedEffect(uiState.deletedSubscription) {
        uiState.deletedSubscription?.let { deleted ->
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.subscription_deleted_snackbar, deleted.name),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.clearDeletedSubscription()
            }
        }
    }

    Scaffold(
        containerColor = ThreatLensBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_my_subscriptions),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
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
                actions = {
                    // Top Right '+' icon matching Screen 3
                    IconButton(
                        onClick = onCreateClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = ThreatLensSurfaceElevated,
                            contentColor = ThreatLensCyan
                        ),
                        modifier = Modifier
                            .padding(end = dimensionResource(R.dimen.dimen_8dp))
                            .size(dimensionResource(R.dimen.dimen_38dp))
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_create_subscription),
                            modifier = Modifier.size(dimensionResource(R.dimen.dimen_22dp))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThreatLensBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingScreen(message = stringResource(R.string.loading_subscriptions))

                uiState.error != null && uiState.subscriptions.isEmpty() -> ErrorScreen(
                    message = stringResource(R.string.error_generic_title),
                    subMessage = uiState.error ?: stringResource(R.string.error_failed_to_load_subscriptions),
                    onRetry = viewModel::loadSubscriptions
                )

                uiState.subscriptions.isEmpty() -> EmptyScreen(
                    message = stringResource(R.string.empty_subscriptions_title),
                    subMessage = stringResource(R.string.empty_subscriptions_subtitle),
                    onReset = onCreateClick
                )

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = dimensionResource(R.dimen.dimen_16dp))
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                top = dimensionResource(R.dimen.dimen_8dp),
                                bottom = dimensionResource(R.dimen.dimen_16dp)
                            ),
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_12dp))
                        ) {
                            if (!hasNotificationPermission && !isBannerDismissed) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                                        colors = CardDefaults.cardColors(containerColor = ThreatLensMediumSeverity.copy(alpha = 0.12f)),
                                        border = BorderStroke(
                                            dimensionResource(R.dimen.border_stroke_normal),
                                            ThreatLensMediumSeverity.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(dimensionResource(R.dimen.dimen_12dp)),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp)),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsOff,
                                                    contentDescription = null,
                                                    tint = ThreatLensMediumSeverity,
                                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_20dp))
                                                )
                                                Text(
                                                    text = stringResource(R.string.push_alerts_disabled_device),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ThreatLensTextPrimary
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_4dp)),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        } else {
                                                            NotificationHelper(context).openNotificationSettings(context)
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(
                                                        horizontal = dimensionResource(R.dimen.dimen_8dp),
                                                        vertical = dimensionResource(R.dimen.dimen_4dp)
                                                    )
                                                ) {
                                                    Text(
                                                        stringResource(R.string.action_enable),
                                                        color = ThreatLensCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = textDimensionResource(R.dimen.font_size_12sp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { isBannerDismissed = true },
                                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_28dp))
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.action_dismiss),
                                                        tint = ThreatLensTextTertiary,
                                                        modifier = Modifier.size(dimensionResource(R.dimen.dimen_16dp))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            items(
                                items = uiState.subscriptions,
                                key = { it.id }
                            ) { subscription ->
                                val index = uiState.subscriptions.indexOf(subscription)
                                val icon = getSubscriptionIcon(subscription, index)

                                SubscriptionRowItem(
                                    subscription = subscription,
                                    icon = icon,
                                    onClick = { onSubscriptionClick(subscription.id) },
                                    onToggle = { viewModel.togglePause(subscription) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        // Bottom button: "+ Create Subscription" matching Screen 3
                        Button(
                            onClick = onCreateClick,
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_full)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThreatLensSurfaceElevated,
                                contentColor = ThreatLensCyan
                            ),
                            border = BorderStroke(
                                dimensionResource(R.dimen.border_stroke_normal),
                                ThreatLensCyan.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensionResource(R.dimen.dimen_52dp))
                                .padding(bottom = dimensionResource(R.dimen.dimen_8dp))
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_20dp))
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_8dp)))
                            Text(
                                stringResource(R.string.action_create_subscription),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = textDimensionResource(R.dimen.font_size_15sp)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subscription Card Row matching Screen 3 in reference image.
 */
@Composable
private fun SubscriptionRowItem(
    subscription: Subscription,
    icon: ImageVector,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = !subscription.isPaused
    val cardCornerRadius = dimensionResource(R.dimen.corner_radius_card_large)
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cardCornerRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = ThreatLensSurfaceElevated
        ),
        border = BorderStroke(
            dimensionResource(R.dimen.border_stroke_normal),
            if (isActive) ThreatLensCardBorder else ThreatLensCardBorder.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.dimen_2dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.dimen_16dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in colored container
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.dimen_46dp))
                    .clip(CircleShape)
                    .background(
                        if (isActive) ThreatLensCyan.copy(alpha = 0.15f)
                        else ThreatLensSurfaceHighlight
                    )
                    .border(
                        dimensionResource(R.dimen.border_stroke_normal),
                        if (isActive) ThreatLensCyan.copy(alpha = 0.35f)
                        else ThreatLensCardBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) ThreatLensCyan else ThreatLensTextSecondary,
                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_22dp))
                )
            }

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_14dp)))

            // Title and Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textDimensionResource(R.dimen.font_size_15sp)
                    ),
                    color = ThreatLensTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_3dp)))
                Text(
                    text = subscription.cadenceSummary(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = textDimensionResource(R.dimen.font_size_12sp)
                    ),
                    color = ThreatLensTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_2dp)))
                Text(
                    text = subscription.nextFireFormatted(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = textDimensionResource(R.dimen.font_size_11sp)
                    ),
                    color = if (isActive) ThreatLensCyan else ThreatLensTextTertiary
                )
            }

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_10dp)))

            // Switch toggle matching Screen 3
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = darkTealButtonText,
                    checkedTrackColor = ThreatLensCyan,
                    uncheckedThumbColor = ThreatLensTextTertiary,
                    uncheckedTrackColor = ThreatLensSurfaceHighlight,
                    uncheckedBorderColor = ThreatLensCardBorder
                )
            )
        }
    }
}
