package com.example.threatlens.common

/**
 * Global application constants for ThreatLens.
 * Consolidates networking, timeouts, auth, notifications, animations, and debug simulation values.
 */
object AppConstants {

    // ── Networking & API ──
    const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 30L
    const val DEFAULT_READ_TIMEOUT_SECONDS = 30L
    const val DEFAULT_WRITE_TIMEOUT_SECONDS = 30L
    const val DEFAULT_PAGE_SIZE = 20

    // ── Search & Carousel ──
    const val SEARCH_DEBOUNCE_MS = 400L
    const val CAROUSEL_SWIPE_DELAY_MS = 5000L
    const val CAROUSEL_PAGE_LIMIT = 5
    const val CAROUSEL_SCROLL_ANIM_DURATION_MS = 650
    const val MAX_PREVIEW_MATCHES = 5

    // ── Authentication & Token ──
    const val TOKEN_LIFETIME_MS = 3 * 60 * 1000L // 3 minutes
    const val REFRESH_DELAY_MS = 1_200L // 1.2 seconds simulation
    const val HEADER_AUTHORIZATION = "Authorization"
    const val TOKEN_PREFIX_BEARER = "Bearer "

    // ── Notifications & Deep Links ──
    const val CHANNEL_ID_ALERTS = "cyber_alerts"
    const val CHANNEL_NAME_ALERTS = "Threat Alerts"
    const val NOTIFICATION_ID_BASE = 1001
    const val EXTRA_SUBSCRIPTION_ID = "extra_subscription_id"
    const val EXTRA_WINDOW_ID = "extra_window_id"
    const val EXTRA_STORY_ACTIVITY_KEY = "extra_story_activity_key"
    const val NOTIFICATION_PERMISSION_KEY = "notification_permission_granted"

    // ── DataStore / Preferences Keys ──
    const val PREFS_DEVICE_NAME = "device_prefs"
    const val PREF_KEY_DEVICE_ID = "device_id"
    const val PREF_KEY_FCM_TOKEN = "fcm_token"
    const val PREF_KEY_PENDING_FCM_TOKEN = "pending_fcm_token"

    // ── Seeded IDs for Debugging & Demonstrations ──
    const val SEEDED_SUB_ID = "8127922c-a1f5-47c1-ac3e-9886058a9dce"
    const val SEEDED_SUB_ID_2 = "13ece4e4-3007-48df-95e2-face400a0ae1"
    const val SEEDED_WINDOW_ID = "0d6b5c0e-9d57-4c8a-bd5a-6f3a7a3f2b10"
    const val DEFAULT_DEBUG_LATENCY_MS = 300L
    const val MAX_DEBUG_LATENCY_MS = 5000L

    // ── Splash Screen Animation Timers ──
    const val SPLASH_DELAY_MS = 1800L
    const val SPLASH_CONTENT_ANIM_MS = 800
    const val SPLASH_PULSE_ANIM_MS = 1500
    const val SPLASH_SCAN_ANIM_MS = 2000
    const val SPLASH_CROSSFADE_MS = 500

    // ── Default Mock / Fallback Values ──
    const val DEFAULT_READ_TIME_MINUTES = 3
    const val DEFAULT_TIMEZONE = "Asia/Kolkata (IST)"
    const val DEFAULT_TIMEZONE_CLEAN = "Asia/Kolkata"
    const val DEFAULT_TIME_VALUE = "09:00:00"
}
