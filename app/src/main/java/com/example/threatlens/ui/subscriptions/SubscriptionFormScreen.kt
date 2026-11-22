package com.example.threatlens.ui.subscriptions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threatlens.R
import com.example.threatlens.data.fcm.NotificationHelper
import com.example.threatlens.data.model.FilterOption
import com.example.threatlens.ui.components.LoadingScreen
import com.example.threatlens.ui.theme.*
import com.google.firebase.crashlytics.FirebaseCrashlytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFormScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)
    val blackColor = colorResource(R.color.black)

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
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        FirebaseCrashlytics.getInstance().setCustomKey("notification_permission_form_granted", isGranted)
        if (!isGranted) {
            showPermissionDeniedDialog = true
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onBack()
    }

    val isNameEmpty = uiState.name.isBlank()
    val isCategoriesEmpty = uiState.categories.isEmpty()

    Scaffold(
        containerColor = ThreatLensBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) stringResource(R.string.title_edit_subscription) else stringResource(R.string.title_create_subscription),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = ThreatLensTextPrimary
                    )
                },
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
                    IconButton(
                        onClick = {
                            hasAttemptedSubmit = true
                            if (viewModel.isFormValid()) {
                                viewModel.save()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = ThreatLensSurfaceElevated,
                            contentColor = ThreatLensCyan
                        ),
                        modifier = Modifier
                            .padding(end = dimensionResource(R.dimen.dimen_8dp))
                            .size(dimensionResource(R.dimen.dimen_38dp))
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = ThreatLensCyan,
                                strokeWidth = dimensionResource(R.dimen.dimen_2dp),
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_18dp))
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.action_save),
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_22dp))
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThreatLensBackground)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingScreen(message = stringResource(R.string.loading_subscription))
            return@Scaffold
        }

        if (showPermissionDeniedDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDeniedDialog = false },
                containerColor = ThreatLensSurfaceElevated,
                icon = {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = ThreatLensMediumSeverity,
                        modifier = Modifier.size(dimensionResource(R.dimen.dimen_28dp))
                    )
                },
                title = {
                    Text(
                        stringResource(R.string.permission_denied_dialog_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThreatLensTextPrimary
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.permission_denied_dialog_message),
                        color = ThreatLensTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDeniedDialog = false
                            NotificationHelper(context).openNotificationSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThreatLensCyan,
                            contentColor = darkTealButtonText
                        ),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_button))
                    ) {
                        Text(stringResource(R.string.action_open_settings), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showPermissionDeniedDialog = false },
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_button)),
                        border = BorderStroke(
                            dimensionResource(R.dimen.border_stroke_normal),
                            ThreatLensCardBorder
                        )
                    ) {
                        Text(stringResource(R.string.action_continue_without_alerts), color = ThreatLensTextSecondary)
                    }
                }
            )
        }

        if (showTimePicker) {
            val parts = uiState.cadenceTime.split(":")
            val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val timePickerState = rememberTimePickerState(
                initialHour = initHour,
                initialMinute = initMinute,
                is24Hour = false
            )

            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val formatted = String.format(
                                java.util.Locale.US,
                                "%02d:%02d:00",
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            viewModel.updateCadenceTime(formatted)
                            showTimePicker = false
                        }
                    ) {
                        Text(
                            stringResource(R.string.action_ok),
                            color = ThreatLensCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(
                            stringResource(R.string.action_cancel),
                            color = ThreatLensTextSecondary
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.form_time_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThreatLensTextPrimary
                    )
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = ThreatLensSurface,
                                clockDialSelectedContentColor = ThreatLensBackground,
                                clockDialUnselectedContentColor = ThreatLensTextPrimary,
                                selectorColor = ThreatLensCyan,
                                containerColor = ThreatLensSurfaceElevated,
                                periodSelectorBorderColor = ThreatLensCardBorder,
                                periodSelectorSelectedContainerColor = ThreatLensCyan.copy(alpha = 0.25f),
                                periodSelectorUnselectedContainerColor = ThreatLensSurface,
                                periodSelectorSelectedContentColor = ThreatLensCyan,
                                periodSelectorUnselectedContentColor = ThreatLensTextSecondary,
                                timeSelectorSelectedContainerColor = ThreatLensCyan.copy(alpha = 0.25f),
                                timeSelectorUnselectedContainerColor = ThreatLensSurface,
                                timeSelectorSelectedContentColor = ThreatLensCyan,
                                timeSelectorUnselectedContentColor = ThreatLensTextPrimary
                            )
                        )
                    }
                },
                containerColor = ThreatLensSurfaceElevated,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                tonalElevation = dimensionResource(R.dimen.dimen_8dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = dimensionResource(R.dimen.dimen_20dp),
                    vertical = dimensionResource(R.dimen.dimen_12dp)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_18dp))
        ) {
            // Notification Permission Warning Banner if disabled
            if (!hasNotificationPermission) {
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
                        modifier = Modifier.padding(dimensionResource(R.dimen.dimen_14dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_10dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = ThreatLensMediumSeverity,
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_22dp))
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.push_alerts_disabled_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThreatLensTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.push_alerts_disabled_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThreatLensTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_8dp)))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    NotificationHelper(context).openNotificationSettings(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThreatLensMediumSeverity,
                                contentColor = blackColor
                            ),
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)),
                            contentPadding = PaddingValues(
                                horizontal = dimensionResource(R.dimen.dimen_12dp),
                                vertical = dimensionResource(R.dimen.dimen_6dp)
                            )
                        ) {
                            Text(
                                stringResource(R.string.action_enable),
                                fontSize = textDimensionResource(R.dimen.font_size_12sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Error banner if any
            uiState.error?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThreatLensErrorContainer),
                    border = BorderStroke(
                        dimensionResource(R.dimen.border_stroke_normal),
                        ThreatLensError.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_button)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        modifier = Modifier.padding(dimensionResource(R.dimen.dimen_14dp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = ThreatLensOnErrorContainer
                    )
                }
            }

            // ── 1. Subscription Name * ──
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))) {
                Text(
                    text = stringResource(R.string.form_name_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ThreatLensTextPrimary
                )
                val showNameError = hasAttemptedSubmit && isNameEmpty

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    placeholder = {
                        Text(
                            stringResource(R.string.form_name_placeholder),
                            color = ThreatLensTextTertiary,
                            fontSize = textDimensionResource(R.dimen.font_size_14sp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                    isError = showNameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThreatLensSurfaceElevated,
                        unfocusedContainerColor = ThreatLensSurface,
                        focusedBorderColor = if (showNameError) ThreatLensHighSeverity else ThreatLensCyan,
                        unfocusedBorderColor = if (showNameError) ThreatLensHighSeverity else ThreatLensCardBorder,
                        errorBorderColor = ThreatLensHighSeverity,
                        focusedTextColor = ThreatLensTextPrimary,
                        unfocusedTextColor = ThreatLensTextPrimary
                    )
                )

                if (showNameError) {
                    Text(
                        text = stringResource(R.string.form_name_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = ThreatLensHighSeverity,
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.dimen_6dp))
                    )
                }
            }

            // ── 2. Categories * ──
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp))) {
                Text(
                    text = stringResource(R.string.form_categories_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ThreatLensTextPrimary
                )

                val showCatError = hasAttemptedSubmit && isCategoriesEmpty

                // Box displaying selected chips and dropdown trigger
                var categoryPickerOpen by remember { mutableStateOf(false) }

                Surface(
                    onClick = { categoryPickerOpen = !categoryPickerOpen },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                    color = ThreatLensSurfaceElevated,
                    border = BorderStroke(
                        dimensionResource(R.dimen.border_stroke_normal),
                        if (showCatError) ThreatLensHighSeverity else ThreatLensCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(R.dimen.dimen_12dp),
                                vertical = dimensionResource(R.dimen.dimen_10dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.categories.isEmpty()) {
                            Text(
                                stringResource(R.string.form_categories_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThreatLensTextTertiary,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp)),
                                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))
                            ) {
                                uiState.categories.forEach { cat ->
                                    val label = cat.replace('_', ' ').replaceFirstChar { it.uppercase() }
                                    Surface(
                                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)),
                                        color = ThreatLensCyan.copy(alpha = 0.18f),
                                        border = BorderStroke(
                                            dimensionResource(R.dimen.border_stroke_normal),
                                            ThreatLensCyan.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = dimensionResource(R.dimen.dimen_8dp),
                                                vertical = dimensionResource(R.dimen.dimen_4dp)
                                            ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_4dp))
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = ThreatLensCyan
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.remove_content_desc),
                                                tint = ThreatLensCyan,
                                                modifier = Modifier
                                                    .size(dimensionResource(R.dimen.dimen_14dp))
                                                    .clickable { viewModel.toggleCategory(cat) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Icon(
                            imageVector = if (categoryPickerOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.toggle_options_content_desc),
                            tint = ThreatLensTextSecondary,
                            modifier = Modifier.size(dimensionResource(R.dimen.dimen_20dp))
                        )
                    }
                }

                if (showCatError) {
                    Text(
                        text = stringResource(R.string.form_categories_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = ThreatLensHighSeverity,
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.dimen_6dp))
                    )
                }

                // Category selector chips (when opened or available)
                val defaultCategories = listOf(
                    FilterOption("ransomware", "Ransomware"),
                    FilterOption("healthcare", "Healthcare"),
                    FilterOption("banking", "Banking"),
                    FilterOption("malware", "Malware"),
                    FilterOption("phishing", "Phishing"),
                    FilterOption("vulnerabilities", "Vulnerabilities"),
                    FilterOption("critical_infrastructure", "Critical Infrastructure")
                )
                val availableCats = if (uiState.categoryOptions.isNotEmpty()) uiState.categoryOptions else defaultCategories

                AnimatedVisibility(visible = categoryPickerOpen) {
                    Surface(
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                        color = ThreatLensSurface,
                        border = BorderStroke(
                            dimensionResource(R.dimen.border_stroke_normal),
                            ThreatLensCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(R.dimen.dimen_4dp))
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(dimensionResource(R.dimen.dimen_12dp)),
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp)),
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))
                        ) {
                            availableCats.forEach { option ->
                                val isSelected = option.value in uiState.categories
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleCategory(option.value) },
                                    label = {
                                        Text(
                                            option.label,
                                            fontSize = textDimensionResource(R.dimen.font_size_12sp)
                                        )
                                    },
                                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ThreatLensCyan,
                                        selectedLabelColor = darkTealButtonText,
                                        containerColor = ThreatLensSurfaceElevated,
                                        labelColor = ThreatLensTextSecondary
                                    ),
                                    border = BorderStroke(
                                        dimensionResource(R.dimen.border_stroke_normal),
                                        if (isSelected) ThreatLensCyan else ThreatLensCardBorder
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. Cadence * (Daily, Weekly, Monthly) ──
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp))) {
                Text(
                    text = stringResource(R.string.form_cadence_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ThreatLensTextPrimary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)))
                        .background(ThreatLensSurfaceElevated)
                        .border(
                            dimensionResource(R.dimen.border_stroke_normal),
                            ThreatLensCardBorder,
                            RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card))
                        )
                        .padding(dimensionResource(R.dimen.dimen_4dp)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_4dp))
                ) {
                    val cadences = listOf(
                        "daily" to stringResource(R.string.cadence_daily),
                        "weekly" to stringResource(R.string.cadence_weekly),
                        "monthly" to stringResource(R.string.cadence_monthly)
                    )
                    cadences.forEach { (key, label) ->
                        val isSelected = uiState.cadence == key
                        Surface(
                            onClick = { viewModel.updateCadence(key) },
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)),
                            color = if (isSelected) ThreatLensCyan else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(dimensionResource(R.dimen.dimen_40dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) darkTealButtonText else ThreatLensTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // ── 4. Days * (When Weekly) ──
            AnimatedVisibility(visible = uiState.cadence == "weekly") {
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp))) {
                    Text(
                        text = stringResource(R.string.form_days_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ThreatLensTextPrimary
                    )

                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEachIndexed { index, day ->
                            val isSelected = uiState.cadenceDayOfWeek == index
                            Surface(
                                onClick = { viewModel.updateCadenceDayOfWeek(index) },
                                shape = CircleShape,
                                color = if (isSelected) ThreatLensCyan else ThreatLensSurfaceElevated,
                                border = BorderStroke(
                                    dimensionResource(R.dimen.border_stroke_normal),
                                    if (isSelected) ThreatLensCyan else ThreatLensCardBorder
                                ),
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_42dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = textDimensionResource(R.dimen.font_size_11sp)
                                        ),
                                        color = if (isSelected) darkTealButtonText else ThreatLensTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 5. Time * (Android Clock with AM / PM) ──
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))) {
                Text(
                    text = stringResource(R.string.form_time_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ThreatLensTextPrimary
                )

                val displayTime = remember(uiState.cadenceTime) {
                    formatCadenceTimeDisplay(uiState.cadenceTime)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)))
                        .clickable { showTimePicker = true }
                ) {
                    OutlinedTextField(
                        value = displayTime,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(
                                    Icons.Outlined.Schedule,
                                    contentDescription = stringResource(R.string.time_picker_content_desc),
                                    tint = ThreatLensCyan,
                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_20dp))
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = ThreatLensSurface,
                            disabledBorderColor = ThreatLensCardBorder,
                            disabledTextColor = ThreatLensTextPrimary,
                            disabledTrailingIconColor = ThreatLensCyan
                        )
                    )
                }
            }

            // ── 6. Time Zone * ──
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))) {
                Text(
                    text = stringResource(R.string.form_timezone_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ThreatLensTextPrimary
                )

                val zones = listOf(
                    "Asia/Kolkata (IST)",
                    "UTC",
                    "America/New_York (EST)",
                    "Europe/London (GMT)",
                    "Asia/Singapore (SGT)",
                    "Asia/Tokyo (JST)"
                )
                var tzExpanded by remember { mutableStateOf(false) }
                val currentZoneLabel = if (uiState.timezone.contains("Kolkata") || uiState.timezone.contains("IST")) {
                    "Asia/Kolkata (IST)"
                } else {
                    uiState.timezone
                }

                ExposedDropdownMenuBox(
                    expanded = tzExpanded,
                    onExpandedChange = { tzExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentZoneLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                if (tzExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = ThreatLensTextSecondary
                            )
                        },
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThreatLensSurfaceElevated,
                            unfocusedContainerColor = ThreatLensSurface,
                            focusedBorderColor = ThreatLensCyan,
                            unfocusedBorderColor = ThreatLensCardBorder,
                            focusedTextColor = ThreatLensTextPrimary,
                            unfocusedTextColor = ThreatLensTextPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = tzExpanded,
                        onDismissRequest = { tzExpanded = false },
                        containerColor = ThreatLensSurfaceElevated,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card))
                    ) {
                        zones.forEach { zone ->
                            DropdownMenuItem(
                                text = { Text(zone, color = ThreatLensTextPrimary) },
                                onClick = {
                                    val cleanTz = zone.split(" ").first()
                                    viewModel.updateTimezone(cleanTz)
                                    tzExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_12dp)))

            // ── Save Button matching Screen 4 ──
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (viewModel.isFormValid()) {
                        viewModel.save()
                    }
                },
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_full)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThreatLensCyan,
                    contentColor = darkTealButtonText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.dimen_52dp))
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = darkTealButtonText,
                        modifier = Modifier.size(dimensionResource(R.dimen.dimen_20dp)),
                        strokeWidth = dimensionResource(R.dimen.dimen_2dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_24dp)))
        }
    }
}

private fun formatCadenceTimeDisplay(timeStr: String): String {
    return try {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        String.format(java.util.Locale.US, "%02d:%02d %s", hour12, minute, amPm)
    } catch (_: Exception) {
        timeStr.take(5)
    }
}
