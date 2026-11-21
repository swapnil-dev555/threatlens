package com.example.threatlens.ui.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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
import com.example.threatlens.common.AppConstants
import com.example.threatlens.data.model.NewsCard
import com.example.threatlens.ui.components.*
import com.example.threatlens.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    onStoryClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: NewsFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val darkTealButtonText = colorResource(R.color.color_dark_teal_button_text)

    Scaffold(
        containerColor = ThreatLensBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThreatLensBackground)
                    .statusBarsPadding()
            ) {
                // Top header: ThreatLens Logo + Name + Notification Bell
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensionResource(R.dimen.dimen_16dp),
                            vertical = dimensionResource(R.dimen.dimen_10dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_10dp))
                    ) {
                        ThreatLensLogo(size = dimensionResource(R.dimen.dimen_34dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = textDimensionResource(R.dimen.letter_spacing_0_5sp)
                            ),
                            color = ThreatLensTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onNotificationsClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = ThreatLensSurfaceElevated,
                            contentColor = ThreatLensTextPrimary
                        ),
                        modifier = Modifier.size(dimensionResource(R.dimen.dimen_40dp))
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = ThreatLensCyan,
                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_8dp))
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.nav_notifications),
                                modifier = Modifier.size(dimensionResource(R.dimen.dimen_26dp))
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensionResource(R.dimen.dimen_16dp),
                            vertical = dimensionResource(R.dimen.dimen_2dp)
                        ),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThreatLensTextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = ThreatLensTextSecondary,
                            modifier = Modifier.size(dimensionResource(R.dimen.dimen_20dp))
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = uiState.searchQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.action_clear),
                                    tint = ThreatLensTextSecondary,
                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_18dp))
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_full)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThreatLensSurfaceElevated,
                        unfocusedContainerColor = ThreatLensSurface,
                        focusedBorderColor = ThreatLensCyan,
                        unfocusedBorderColor = ThreatLensCardBorder,
                        focusedTextColor = ThreatLensTextPrimary,
                        unfocusedTextColor = ThreatLensTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_8dp)))

                // Section Filter Pills (All, Ransomware, Phishing, Malware, etc.)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.dimen_16dp)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_8dp)),
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.dimen_6dp))
                ) {
                    item {
                        val isSelected = uiState.selectedSection == null
                        Surface(
                            onClick = { viewModel.onSectionSelected(null) },
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                            color = if (isSelected) ThreatLensCyan else ThreatLensSurfaceElevated,
                            border = BorderStroke(
                                dimensionResource(R.dimen.border_stroke_normal),
                                if (isSelected) ThreatLensCyan else ThreatLensCardBorder
                            ),
                            modifier = Modifier.height(dimensionResource(R.dimen.dimen_34dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_4dp)),
                                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.dimen_12dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = null,
                                    tint = if (isSelected) darkTealButtonText else ThreatLensTextSecondary,
                                    modifier = Modifier.size(dimensionResource(R.dimen.dimen_14dp))
                                )
                                Text(
                                    text = stringResource(R.string.category_all),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) darkTealButtonText else ThreatLensTextSecondary
                                )
                            }
                        }
                    }

                    items(uiState.sections) { section ->
                        val isSelected = uiState.selectedSection?.key == section.key
                        Surface(
                            onClick = { viewModel.onSectionSelected(section) },
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xl)),
                            color = if (isSelected) ThreatLensCyan else ThreatLensSurfaceElevated,
                            border = BorderStroke(
                                dimensionResource(R.dimen.border_stroke_normal),
                                if (isSelected) ThreatLensCyan else ThreatLensCardBorder
                            ),
                            modifier = Modifier.height(dimensionResource(R.dimen.dimen_34dp))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.dimen_14dp))
                            ) {
                                Text(
                                    text = section.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) darkTealButtonText else ThreatLensTextSecondary
                                )
                            }
                        }
                    }
                }

                // ── Auto-swiping Carousel of top 5 news pinned at the top after categories ──
                val carouselStories = remember(uiState.stories) { uiState.stories.take(AppConstants.CAROUSEL_PAGE_LIMIT) }
                if (carouselStories.isNotEmpty()) {
                    NewsCarouselView(
                        stories = carouselStories,
                        onStoryClick = onStoryClick
                    )
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingScreen(message = stringResource(R.string.loading_latest_news))

                uiState.error != null && uiState.stories.isEmpty() -> ErrorScreen(
                    message = stringResource(R.string.error_generic_title),
                    subMessage = uiState.error ?: stringResource(R.string.error_generic_subtitle),
                    onRetry = viewModel::refresh
                )

                uiState.stories.isEmpty() -> EmptyScreen(
                    message = stringResource(R.string.empty_news_title),
                    subMessage = stringResource(R.string.empty_news_subtitle),
                    onReset = {
                        viewModel.onSearchQueryChanged("")
                        viewModel.onSectionSelected(null)
                    }
                )

                else -> {
                    val listState = rememberLazyListState()

                    // Infinite pagination
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val total = listState.layoutInfo.totalItemsCount
                            lastVisible >= total - 3 && uiState.hasMore && !uiState.isLoadingMore
                        }
                    }
                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) viewModel.loadMore()
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = dimensionResource(R.dimen.dimen_16dp),
                            end = dimensionResource(R.dimen.dimen_16dp),
                            top = dimensionResource(R.dimen.dimen_8dp),
                            bottom = dimensionResource(R.dimen.dimen_24dp)
                        ),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_10dp))
                    ) {
                        // Header label above the full news list
                        item(key = "feed_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = dimensionResource(R.dimen.dimen_4dp)),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.selectedSection != null) {
                                        stringResource(R.string.section_feed_title, uiState.selectedSection?.label ?: "").uppercase()
                                    } else {
                                        stringResource(R.string.latest_threat_feed)
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = textDimensionResource(R.dimen.letter_spacing_1sp)
                                    ),
                                    color = ThreatLensTextSecondary
                                )

                                Surface(
                                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_pill)),
                                    color = ThreatLensSurfaceHighlight.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        dimensionResource(R.dimen.border_stroke_thin),
                                        ThreatLensCardBorder.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.reports_count, uiState.stories.size),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = ThreatLensTextSecondary,
                                        modifier = Modifier.padding(
                                            horizontal = dimensionResource(R.dimen.dimen_8dp),
                                            vertical = dimensionResource(R.dimen.dimen_3dp)
                                        )
                                    )
                                }
                            }
                        }

                        // Feed items: Existing news list starting from 1st item as requested
                        itemsIndexed(
                            items = uiState.stories,
                            key = { _, card -> card.activityKey }
                        ) { _, card ->
                            NewsCardItem(
                                card = card,
                                onClick = { onStoryClick(card.activityKey) },
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = tween(300),
                                        fadeOutSpec = tween(300),
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(dimensionResource(R.dimen.dimen_16dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = ThreatLensCyan,
                                        modifier = Modifier.size(dimensionResource(R.dimen.dimen_24dp)),
                                        strokeWidth = dimensionResource(R.dimen.dimen_2dp)
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

/**
 * Pinned top Carousel view displaying top 5 news with 5s auto-swipe animation.
 */
@Composable
private fun NewsCarouselView(
    stories: List<NewsCard>,
    onStoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (stories.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { stories.size })

    // Reset carousel to first slide whenever category / story list changes
    LaunchedEffect(stories) {
        if (stories.isNotEmpty() && pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    // Auto swipe every 5 seconds
    LaunchedEffect(pagerState, stories.size) {
        if (stories.size > 1) {
            while (true) {
                delay(AppConstants.CAROUSEL_SWIPE_DELAY_MS)
                val targetPage = (pagerState.currentPage + 1) % stories.size
                pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = tween(
                        durationMillis = AppConstants.CAROUSEL_SCROLL_ANIM_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(R.dimen.dimen_4dp),
                bottom = dimensionResource(R.dimen.dimen_4dp)
            )
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.dimen_16dp)),
            pageSpacing = dimensionResource(R.dimen.dimen_12dp),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val story = stories[page]
            FeaturedNewsCardItem(
                card = story,
                onClick = { onStoryClick(story.activityKey) }
            )
        }

        // Animated carousel page indicator dots
        if (stories.size > 1) {
            val dotUnselected = dimensionResource(R.dimen.carousel_indicator_dot_unselected)
            val dotSelected = dimensionResource(R.dimen.carousel_indicator_dot_selected)

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.dimen_8dp)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(stories.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (isSelected) dotSelected else dotUnselected,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
                        label = "carouselDotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(R.dimen.dimen_3dp))
                            .height(dimensionResource(R.dimen.carousel_indicator_height))
                            .width(indicatorWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) ThreatLensCyan else ThreatLensCardBorder.copy(alpha = 0.6f)
                            )
                    )
                }
            }
        }
    }
}
