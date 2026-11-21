package com.example.threatlens.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.threatlens.R
import com.example.threatlens.ui.debug.DebugScreen
import com.example.threatlens.ui.feed.NewsFeedScreen
import com.example.threatlens.ui.story.StoryDetailScreen
import com.example.threatlens.ui.subscriptions.*
import com.example.threatlens.ui.theme.*

@Composable
fun AppNavGraph(
    deepLinkSubscriptionId: String? = null,
    deepLinkWindowId: String? = null
) {
    val navController = rememberNavController()

    // If launched from a push notification, go directly to window matches
    val effectiveStart: Any = if (deepLinkSubscriptionId != null && deepLinkWindowId != null) {
        Screen.WindowMatches(deepLinkSubscriptionId, deepLinkWindowId)
    } else {
        Screen.NewsFeed
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val isHome = currentRoute.contains("NewsFeed")
    val isSubscriptions = currentRoute.contains("SubscriptionsList")
    val isNotifications = currentRoute.contains("Notifications") || currentRoute.contains("WindowMatches")
    val isDebug = currentRoute.contains("Debug")

    val showBottomBar = isHome || isSubscriptions || (isNotifications && deepLinkSubscriptionId == null) || isDebug

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = ThreatLensBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = ThreatLensSurface,
                    tonalElevation = dimensionResource(R.dimen.dimen_8dp),
                    modifier = Modifier.border(
                        BorderStroke(
                            dimensionResource(R.dimen.border_stroke_thin),
                            ThreatLensCardBorder.copy(alpha = 0.6f)
                        )
                    )
                ) {
                    val labelFontSize = textDimensionResource(R.dimen.font_size_11sp)

                    // 1. Home (News Feed)
                    NavigationBarItem(
                        selected = isHome,
                        onClick = {
                            if (!isHome) {
                                val popped = navController.popBackStack(Screen.NewsFeed, inclusive = false)
                                if (!popped) {
                                    navController.navigate(Screen.NewsFeed) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.nav_home)
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.nav_home),
                                fontSize = labelFontSize,
                                fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ThreatLensCyan.copy(alpha = 0.2f),
                            selectedIconColor = ThreatLensCyan,
                            selectedTextColor = ThreatLensCyan,
                            unselectedIconColor = ThreatLensTextSecondary,
                            unselectedTextColor = ThreatLensTextSecondary
                        )
                    )

                    // 2. Subscriptions
                    NavigationBarItem(
                        selected = isSubscriptions,
                        onClick = {
                            if (!isSubscriptions) {
                                navController.navigate(Screen.SubscriptionsList) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (isSubscriptions) Icons.AutoMirrored.Filled.Feed else Icons.AutoMirrored.Outlined.Feed,
                                contentDescription = stringResource(R.string.nav_subscriptions)
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.nav_subscriptions),
                                fontSize = labelFontSize,
                                fontWeight = if (isSubscriptions) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ThreatLensCyan.copy(alpha = 0.2f),
                            selectedIconColor = ThreatLensCyan,
                            selectedTextColor = ThreatLensCyan,
                            unselectedIconColor = ThreatLensTextSecondary,
                            unselectedTextColor = ThreatLensTextSecondary
                        )
                    )

                    // 3. Notifications
                    NavigationBarItem(
                        selected = isNotifications,
                        onClick = {
                            if (!isNotifications) {
                                navController.navigate(Screen.Notifications) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (isNotifications) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.nav_notifications)
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.nav_notifications),
                                fontSize = labelFontSize,
                                fontWeight = if (isNotifications) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ThreatLensCyan.copy(alpha = 0.2f),
                            selectedIconColor = ThreatLensCyan,
                            selectedTextColor = ThreatLensCyan,
                            unselectedIconColor = ThreatLensTextSecondary,
                            unselectedTextColor = ThreatLensTextSecondary
                        )
                    )

                    // 4. Debug / More
                    NavigationBarItem(
                        selected = isDebug,
                        onClick = {
                            if (!isDebug) {
                                navController.navigate(Screen.Debug) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (isDebug) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                                contentDescription = stringResource(R.string.nav_more)
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.nav_more),
                                fontSize = labelFontSize,
                                fontWeight = if (isDebug) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ThreatLensCyan.copy(alpha = 0.2f),
                            selectedIconColor = ThreatLensCyan,
                            selectedTextColor = ThreatLensCyan,
                            unselectedIconColor = ThreatLensTextSecondary,
                            unselectedTextColor = ThreatLensTextSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = effectiveStart,
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            enterTransition = {
                val fromRoute = initialState.destination.route
                val toRoute = targetState.destination.route
                if (isTopLevelTab(fromRoute) && isTopLevelTab(toRoute)) {
                    fadeIn(animationSpec = tween(durationMillis = 200))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                }
            },
            exitTransition = {
                val fromRoute = initialState.destination.route
                val toRoute = targetState.destination.route
                if (isTopLevelTab(fromRoute) && isTopLevelTab(toRoute)) {
                    fadeOut(animationSpec = tween(durationMillis = 200))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                }
            },
            popEnterTransition = {
                val fromRoute = initialState.destination.route
                val toRoute = targetState.destination.route
                if (isTopLevelTab(fromRoute) && isTopLevelTab(toRoute)) {
                    fadeIn(animationSpec = tween(durationMillis = 200))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                }
            },
            popExitTransition = {
                val fromRoute = initialState.destination.route
                val toRoute = targetState.destination.route
                if (isTopLevelTab(fromRoute) && isTopLevelTab(toRoute)) {
                    fadeOut(animationSpec = tween(durationMillis = 200))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                }
            }
        ) {
            // ── Screen 1: News Feed ──
            composable<Screen.NewsFeed> {
                NewsFeedScreen(
                    onStoryClick = { key -> navController.navigate(Screen.StoryDetail(key)) },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // ── Screen 2: Story Detail ──
            composable<Screen.StoryDetail> {
                StoryDetailScreen(onBack = { navController.navigateUp() })
            }

            // ── Screen 3: Subscriptions List ──
            composable<Screen.SubscriptionsList> {
                SubscriptionsListScreen(
                    onBack = null, // Top-level tab
                    onSubscriptionClick = { id -> navController.navigate(Screen.SubscriptionDetail(id)) },
                    onCreateClick = { navController.navigate(Screen.CreateSubscription) },
                    onEditClick = { id -> navController.navigate(Screen.EditSubscription(id)) }
                )
            }

            // ── Screen 4: Create Subscription ──
            composable<Screen.CreateSubscription> {
                SubscriptionFormScreen(onBack = { navController.navigateUp() })
            }

            // ── Screen 4: Edit Subscription ──
            composable<Screen.EditSubscription> {
                SubscriptionFormScreen(onBack = { navController.navigateUp() })
            }

            // ── Screen 5: Subscription Detail ──
            composable<Screen.SubscriptionDetail> { backStackEntry ->
                val detailRoute = backStackEntry.toRoute<Screen.SubscriptionDetail>()
                SubscriptionDetailScreen(
                    onBack = { navController.navigateUp() },
                    onStoryClick = { subscriptionId, key ->
                        navController.navigate(Screen.StoryDetail(activityKey = key, subscriptionId = subscriptionId))
                    },
                    onEditClick = { subId ->
                        val targetId = subId.ifBlank { detailRoute.subscriptionId }
                        navController.navigate(Screen.EditSubscription(targetId))
                    }
                )
            }

            // ── Screen 6: Notifications tab / Push Results ──
            composable<Screen.Notifications> {
                WindowMatchesScreen(
                    onBack = null, // Top-level tab
                    onStoryClick = { subscriptionId, key ->
                        navController.navigate(Screen.StoryDetail(activityKey = key, subscriptionId = subscriptionId))
                    },
                    onViewSubscription = { subId -> navController.navigate(Screen.SubscriptionDetail(subId)) }
                )
            }

            // ── Screen 6: Deep Link Window Matches ──
            composable<Screen.WindowMatches> {
                WindowMatchesScreen(
                    onBack = { navController.navigateUp() },
                    onStoryClick = { subscriptionId, key ->
                        navController.navigate(Screen.StoryDetail(activityKey = key, subscriptionId = subscriptionId))
                    },
                    onViewSubscription = { subId -> navController.navigate(Screen.SubscriptionDetail(subId)) }
                )
            }

            // ── Screen 7: Debug Tools ──
            composable<Screen.Debug> {
                DebugScreen(onBack = null) // Top-level tab
            }
        }
    }
}

private fun isTopLevelTab(route: String?): Boolean {
    val r = route ?: return false
    return r.contains("NewsFeed") ||
            r.contains("SubscriptionsList") ||
            r.contains("Notifications") ||
            r.contains("Debug")
}
