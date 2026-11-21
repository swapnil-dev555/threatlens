package com.example.threatlens.ui.story

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threatlens.R
import com.example.threatlens.data.model.NewsStory
import com.example.threatlens.ui.components.ErrorScreen
import com.example.threatlens.ui.components.LoadingScreen
import com.example.threatlens.ui.components.formatRelativeTime
import com.example.threatlens.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(
    onBack: () -> Unit,
    viewModel: StoryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val story = uiState.story

    val defaultSectionLabel = stringResource(R.string.general_threat_activity)
    val shareChooserTitle = stringResource(R.string.share_story_chooser_title)

    Scaffold(
        containerColor = ThreatLensBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = story?.sectionLabel?.ifBlank { defaultSectionLabel } ?: defaultSectionLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textDimensionResource(R.dimen.font_size_18sp)
                        ),
                        color = ThreatLensTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = ThreatLensTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val currentStory = uiState.story ?: return@IconButton
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "${currentStory.title}\n\n${currentStory.sourceUrl}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, shareChooserTitle))
                        }
                    ) {
                        Icon(
                            Icons.Outlined.IosShare,
                            contentDescription = stringResource(R.string.action_share),
                            tint = ThreatLensTextPrimary,
                            modifier = Modifier.size(dimensionResource(R.dimen.dimen_22dp))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThreatLensBackground
                )
            )
        },
        bottomBar = {
            story?.sourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Surface(
                    color = ThreatLensBackground,
                    border = BorderStroke(
                        dimensionResource(R.dimen.border_stroke_thin),
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
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThreatLensCyan,
                                contentColor = colorResource(R.color.white)
                            ),
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensionResource(R.dimen.dimen_48dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_18dp))
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_8dp)))
                            Text(
                                stringResource(R.string.action_open_source),
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
                uiState.isLoading -> LoadingScreen(message = stringResource(R.string.loading_threat_details))

                uiState.error != null -> ErrorScreen(
                    message = stringResource(R.string.error_generic_title),
                    subMessage = uiState.error ?: stringResource(R.string.error_failed_to_load_story),
                    onRetry = viewModel::retry
                )

                story != null -> StoryDetailsContent(story = story, isSubscriptionMatch = uiState.isSubscriptionMatch)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryDetailsContent(
    story: NewsStory,
    isSubscriptionMatch: Boolean = false
) {
    val scrollState = rememberScrollState()
    val isDark = LocalThreatLensColors.current.isDark
    val context = LocalContext.current

    val glanceBgColor = if (isDark) colorResource(R.color.color_glance_bg_dark) else colorResource(R.color.color_glance_bg_light)
    val glanceTextColor = if (isDark) colorResource(R.color.color_glance_text_dark) else colorResource(R.color.color_glance_text_light)
    val glanceBodyColor = if (isDark) colorResource(R.color.color_glance_body_dark) else colorResource(R.color.color_dark_teal_button_text)

    val icounterBgColor = if (isDark) colorResource(R.color.color_icounter_bg_dark) else colorResource(R.color.color_icounter_bg_light)
    val icounterHeaderColor = if (isDark) colorResource(R.color.color_icounter_header_dark) else colorResource(R.color.color_icounter_header_light)
    val icounterBodyColor = if (isDark) colorResource(R.color.color_icounter_body_dark) else colorResource(R.color.color_icounter_body_light)

    val timeFormatted = formatRelativeTime(story.publishedTs, context)
    val readTime = if (story.readTimeMinutes > 0) {
        stringResource(R.string.min_read, story.readTimeMinutes)
    } else {
        stringResource(R.string.default_read_time)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                horizontal = dimensionResource(R.dimen.dimen_16dp),
                vertical = dimensionResource(R.dimen.dimen_12dp)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_16dp))
    ) {
        // ── 1. Top Badges Row: [ General Threat Activity ] [ TLP: Clear ] ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sectionLabel = story.sectionLabel.ifBlank { stringResource(R.string.general_threat_activity) }
            OutlinedPillBadge(text = sectionLabel)

            val tlpText = stringResource(
                R.string.tlp_format,
                story.tlp.ifBlank { stringResource(R.string.tlp_default) }
            )
            OutlinedPillBadge(text = tlpText)

            if (story.isRansomware) {
                OutlinedPillBadge(
                    text = stringResource(R.string.tag_ransomware),
                    borderColor = ThreatLensHighSeverity.copy(alpha = 0.5f),
                    textColor = ThreatLensHighSeverity
                )
            }

            if (isSubscriptionMatch) {
                OutlinedPillBadge(
                    text = stringResource(R.string.tag_subscription_match),
                    borderColor = ThreatLensCyan.copy(alpha = 0.6f),
                    textColor = ThreatLensCyan
                )
            }
        }

        // ── 2. Headline Title ──
        Text(
            text = story.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = textDimensionResource(R.dimen.font_size_22sp),
                lineHeight = textDimensionResource(R.dimen.font_size_28sp)
            ),
            color = ThreatLensTextPrimary
        )

        // ── 3. Metadata Row: 🕒 27d ago · 3 min read ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = ThreatLensTextSecondary,
                modifier = Modifier.size(dimensionResource(R.dimen.dimen_16dp))
            )
            Text(
                text = stringResource(R.string.metadata_time_read_separator, timeFormatted, readTime),
                style = MaterialTheme.typography.bodyMedium,
                color = ThreatLensTextSecondary
            )
        }

        // ── 4. At a Glance Card (Cyan Box) ──
        val atAGlanceText = story.atAGlance.ifBlank { story.summary }
        if (atAGlanceText.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
                colors = CardDefaults.cardColors(containerColor = glanceBgColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.dimen_16dp)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))
                ) {
                    Text(
                        text = stringResource(R.string.header_at_a_glance),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textDimensionResource(R.dimen.font_size_15sp)
                        ),
                        color = glanceTextColor
                    )
                    Text(
                        text = atAGlanceText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = textDimensionResource(R.dimen.font_size_14sp),
                            lineHeight = textDimensionResource(R.dimen.font_size_20sp)
                        ),
                        color = glanceBodyColor
                    )
                }
            }
        }

        // ── 5. Summary Section ──
        if (story.summary.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))) {
                Text(
                    text = stringResource(R.string.header_summary),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textDimensionResource(R.dimen.font_size_17sp)
                    ),
                    color = ThreatLensTextPrimary
                )
                Text(
                    text = story.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = textDimensionResource(R.dimen.font_size_14sp),
                        lineHeight = textDimensionResource(R.dimen.font_size_21sp)
                    ),
                    color = ThreatLensTextPrimary
                )
            }
        }

        // ── 6. How the Attack Works Section ──
        val howWorksText = story.howTheAttackWorks.ifBlank {
            stringResource(R.string.default_attack_explanation, story.summary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))) {
            Text(
                text = stringResource(R.string.header_how_attack_works),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = textDimensionResource(R.dimen.font_size_17sp)
                ),
                color = ThreatLensTextPrimary
            )
            Text(
                text = howWorksText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = textDimensionResource(R.dimen.font_size_14sp),
                    lineHeight = textDimensionResource(R.dimen.font_size_21sp)
                ),
                color = ThreatLensTextPrimary
            )
        }

        // ── 7. iCounter Observation Card (Lavender Box) ──
        val icounterText = story.icounterObservation.ifBlank {
            stringResource(R.string.default_icounter_observation)
        }
        Card(
            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_card)),
            colors = CardDefaults.cardColors(containerColor = icounterBgColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.dimen_16dp)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))
            ) {
                Text(
                    text = stringResource(R.string.header_icounter_observation),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textDimensionResource(R.dimen.font_size_15sp)
                    ),
                    color = icounterHeaderColor
                )
                Text(
                    text = icounterText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = textDimensionResource(R.dimen.font_size_14sp),
                        lineHeight = textDimensionResource(R.dimen.font_size_20sp)
                    ),
                    color = icounterBodyColor
                )
            }
        }

        // ── 8. Horizontal Divider ──
        HorizontalDivider(
            color = ThreatLensCardBorder.copy(alpha = 0.6f),
            thickness = dimensionResource(R.dimen.border_stroke_thin),
            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.dimen_4dp))
        )

        // ── 9. Intelligence Section ──
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_12dp))) {
            Text(
                text = stringResource(R.string.header_intelligence),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = textDimensionResource(R.dimen.font_size_18sp)
                ),
                color = ThreatLensTextPrimary
            )

            // Techniques (MITRE)
            if (story.techniques.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_techniques),
                    items = story.techniques
                )
            }

            // Threat Actors
            if (story.actorNames.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_actors),
                    items = story.actorNames
                )
            }

            // Malware Families
            if (story.malware.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_malware),
                    items = story.malware
                )
            }

            // Tools
            if (story.tools.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_tools),
                    items = story.tools
                )
            }

            // CVEs
            if (story.cves.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_cves),
                    items = story.cves
                )
            }

            // Vendors
            if (story.vendors.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_vendors),
                    items = story.vendors
                )
            }

            // Products
            if (story.products.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_products),
                    items = story.products
                )
            }

            // Target Sectors
            if (story.targetSectors.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_sectors),
                    items = story.targetSectors
                )
            }

            // Target Countries
            if (story.targetCountries.isNotEmpty()) {
                IntelligenceGroup(
                    label = stringResource(R.string.intel_countries),
                    items = story.targetCountries
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_24dp)))
    }
}

@Composable
private fun OutlinedPillBadge(
    text: String,
    borderColor: Color = ThreatLensCardBorder,
    textColor: Color = ThreatLensTextPrimary
) {
    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_pill)),
        color = Color.Transparent,
        border = BorderStroke(dimensionResource(R.dimen.border_stroke_normal), borderColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = textDimensionResource(R.dimen.font_size_12sp)
            ),
            color = textColor,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.dimen_12dp),
                vertical = dimensionResource(R.dimen.dimen_6dp)
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntelligenceGroup(
    label: String,
    items: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_6dp))) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = textDimensionResource(R.dimen.font_size_13sp),
                fontWeight = FontWeight.SemiBold
            ),
            color = ThreatLensTextSecondary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp))
        ) {
            items.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_pill)),
                    color = ThreatLensSurfaceElevated,
                    border = BorderStroke(
                        dimensionResource(R.dimen.border_stroke_normal),
                        ThreatLensCardBorder
                    )
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = textDimensionResource(R.dimen.font_size_12sp)
                        ),
                        color = ThreatLensTextPrimary,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.dimen_12dp),
                            vertical = dimensionResource(R.dimen.dimen_6dp)
                        )
                    )
                }
            }
        }
    }
}
