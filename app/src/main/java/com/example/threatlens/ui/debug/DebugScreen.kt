package com.example.threatlens.ui.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threatlens.R
import com.example.threatlens.common.AppConstants
import com.example.threatlens.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onBack: (() -> Unit)? = null,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showCrashConfirm by remember { mutableStateOf(false) }

    val whiteColor = colorResource(R.color.white)

    if (showCrashConfirm) {
        AlertDialog(
            onDismissRequest = { showCrashConfirm = false },
            containerColor = ThreatLensSurfaceElevated,
            title = {
                Text(
                    stringResource(R.string.debug_force_crash_title),
                    color = ThreatLensTextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    stringResource(R.string.debug_force_crash_message),
                    color = ThreatLensTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::forceCrash,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThreatLensHighSeverity,
                        contentColor = whiteColor
                    ),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card))
                ) {
                    Text(stringResource(R.string.action_crash_now), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCrashConfirm = false },
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
                title = {
                    Text(
                        stringResource(R.string.debug_title),
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = dimensionResource(R.dimen.dimen_16dp),
                    vertical = dimensionResource(R.dimen.dimen_12dp)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_16dp))
        ) {
            // ── Section 0: Theme & Appearance ──
            Text(
                stringResource(R.string.debug_theme_section),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ThreatLensTextPrimary,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.dimen_4dp))
            )

            Card(
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                colors = CardDefaults.cardColors(containerColor = ThreatLensSurfaceElevated),
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.dimen_18dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_12dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.dimen_40dp))
                                .clip(CircleShape)
                                .background(ThreatLensCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = ThreatLensCyan,
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_22dp))
                            )
                        }
                        Column {
                            Text(
                                text = if (uiState.isDarkMode) stringResource(R.string.debug_dark_mode) else stringResource(R.string.debug_light_mode),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThreatLensTextPrimary
                            )
                            Text(
                                text = if (uiState.isDarkMode) stringResource(R.string.debug_cyber_dark_active) else stringResource(R.string.debug_clean_light_active),
                                style = MaterialTheme.typography.bodySmall,
                                color = ThreatLensTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = !uiState.isDarkMode,
                        onCheckedChange = { isLight -> viewModel.setDarkMode(!isLight) },
                        thumbContent = {
                            Icon(
                                imageVector = if (uiState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThreatLensCyan,
                            checkedTrackColor = ThreatLensCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = ThreatLensTextSecondary,
                            uncheckedTrackColor = ThreatLensSurfaceHighlight
                        )
                    )
                }
            }

            // ── Section 1: Fake Backend Controls ──
            Text(
                stringResource(R.string.debug_backend_section),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ThreatLensTextPrimary,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.dimen_4dp))
            )

            Card(
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                colors = CardDefaults.cardColors(containerColor = ThreatLensSurfaceElevated),
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.dimen_18dp)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_14dp))
                ) {
                    // Latency input & slider
                    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_4dp))) {
                        Text(
                            stringResource(R.string.debug_latency_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = ThreatLensTextSecondary
                        )
                        OutlinedTextField(
                            value = "${uiState.latencyMs}",
                            onValueChange = { newVal ->
                                newVal.toLongOrNull()?.let { viewModel.setLatency(it.coerceIn(0, AppConstants.MAX_DEBUG_LATENCY_MS)) }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_button)),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ThreatLensSurface,
                                unfocusedContainerColor = ThreatLensSurface,
                                focusedBorderColor = ThreatLensCyan,
                                unfocusedBorderColor = ThreatLensCardBorder,
                                focusedTextColor = ThreatLensTextPrimary,
                                unfocusedTextColor = ThreatLensTextPrimary
                            )
                        )
                    }

                    Slider(
                        value = uiState.latencyMs.toFloat(),
                        onValueChange = { viewModel.setLatency(it.toLong()) },
                        valueRange = 0f..AppConstants.MAX_DEBUG_LATENCY_MS.toFloat(),
                        steps = 49,
                        colors = SliderDefaults.colors(
                            thumbColor = ThreatLensCyan,
                            activeTrackColor = ThreatLensCyan,
                            inactiveTrackColor = ThreatLensSurfaceHighlight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        color = ThreatLensCardBorder,
                        thickness = dimensionResource(R.dimen.border_stroke_thin)
                    )

                    // Switches matching Screen 7
                    DebugSwitchRow(
                        label = stringResource(R.string.debug_force_401),
                        checked = uiState.force401,
                        onCheckedChange = viewModel::setForce401
                    )
                    DebugSwitchRow(
                        label = stringResource(R.string.debug_force_500),
                        checked = uiState.force500,
                        onCheckedChange = viewModel::setForce500
                    )
                    DebugSwitchRow(
                        label = stringResource(R.string.debug_force_timeout),
                        checked = uiState.forceTimeout,
                        onCheckedChange = viewModel::setForceTimeout
                    )
                }
            }

            // ── Section 2: Push, Subscriptions & Device Info ──
            Text(
                stringResource(R.string.debug_device_info_section),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ThreatLensTextPrimary,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.dimen_4dp),
                    top = dimensionResource(R.dimen.dimen_4dp)
                )
            )

            val subIdLabel = stringResource(R.string.debug_label_sub_id)
            val windowIdLabel = stringResource(R.string.debug_label_window_id)
            val fcmTokenLabel = stringResource(R.string.debug_label_fcm_token)
            val deviceIdLabel = stringResource(R.string.debug_label_device_id)

            Card(
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                colors = CardDefaults.cardColors(containerColor = ThreatLensSurfaceElevated),
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.dimen_18dp)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_14dp))
                ) {
                    CopyableInfoItem(
                        label = subIdLabel,
                        value = uiState.subscriptionId,
                        onCopy = {
                            copyToClipboard(context, subIdLabel, uiState.subscriptionId)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.debug_copied_to_clipboard, subIdLabel)
                                )
                            }
                        }
                    )

                    HorizontalDivider(
                        color = ThreatLensCardBorder,
                        thickness = dimensionResource(R.dimen.border_stroke_thin)
                    )

                    CopyableInfoItem(
                        label = windowIdLabel,
                        value = uiState.windowId,
                        onCopy = {
                            copyToClipboard(context, windowIdLabel, uiState.windowId)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.debug_copied_to_clipboard, windowIdLabel)
                                )
                            }
                        }
                    )

                    HorizontalDivider(
                        color = ThreatLensCardBorder,
                        thickness = dimensionResource(R.dimen.border_stroke_thin)
                    )

                    CopyableInfoItem(
                        label = fcmTokenLabel,
                        value = uiState.fcmToken ?: "fcmToken_abc123456789xyz",
                        onCopy = {
                            val tokenVal = uiState.fcmToken ?: "fcmToken_abc123456789xyz"
                            copyToClipboard(context, fcmTokenLabel, tokenVal)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.debug_copied_to_clipboard, fcmTokenLabel)
                                )
                            }
                        }
                    )

                    HorizontalDivider(
                        color = ThreatLensCardBorder,
                        thickness = dimensionResource(R.dimen.border_stroke_thin)
                    )

                    CopyableInfoItem(
                        label = deviceIdLabel,
                        value = uiState.deviceId ?: "device_456789",
                        onCopy = {
                            val deviceVal = uiState.deviceId ?: "device_456789"
                            copyToClipboard(context, deviceIdLabel, deviceVal)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.debug_copied_to_clipboard, deviceIdLabel)
                                )
                            }
                        }
                    )
                }
            }

            // ── Section 3: Testing & Simulations ──
            Text(
                stringResource(R.string.debug_simulations_section),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ThreatLensTextPrimary,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.dimen_4dp),
                    top = dimensionResource(R.dimen.dimen_4dp)
                )
            )

            val pushSuccessMsg = stringResource(R.string.debug_push_triggered_snackbar)

            // Trigger Push Notification with Fake Backend
            Button(
                onClick = {
                    viewModel.triggerPushNotification()
                    scope.launch {
                        snackbarHostState.showSnackbar(pushSuccessMsg)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThreatLensCyan.copy(alpha = 0.2f),
                    contentColor = ThreatLensCyan
                ),
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensCyan.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.dimen_52dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = ThreatLensCyan,
                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_20dp))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_10dp)))
                Text(
                    stringResource(R.string.action_trigger_push),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Button(
                onClick = { showCrashConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThreatLensHighSeverity.copy(alpha = 0.2f),
                    contentColor = ThreatLensHighSeverity
                ),
                border = BorderStroke(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensHighSeverity.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.dimen_52dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ThreatLensHighSeverity,
                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_20dp))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_10dp)))
                Text(
                    stringResource(R.string.action_trigger_test_crash),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_24dp)))
        }
    }
}

@Composable
private fun DebugSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ThreatLensTextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

@Composable
private fun CopyableInfoItem(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ThreatLensTextSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)))
                .background(ThreatLensSurface)
                .border(
                    dimensionResource(R.dimen.border_stroke_normal),
                    ThreatLensCardBorder,
                    RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium))
                )
                .padding(
                    horizontal = dimensionResource(R.dimen.dimen_12dp),
                    vertical = dimensionResource(R.dimen.dimen_10dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = textDimensionResource(R.dimen.font_size_13sp)
                ),
                color = ThreatLensTextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(dimensionResource(R.dimen.dimen_28dp))
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.debug_copy_content_desc, label),
                    tint = ThreatLensCyan,
                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_16dp))
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
