# CyberSentinel

A cyber-threat intelligence app for security analysts. Monitors a feed of cyber-news stories and
lets analysts create **news subscriptions** — saved filters that deliver push notifications on a
chosen schedule.

---

## Quick Start

```bash
./gradlew assembleDebug
```

The debug build uses the **fake backend** (no real API needed). All 65 seeded stories, sections,
filter options, and subscriptions are available immediately.

---

## Architecture

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| State | ViewModel + StateFlow (MVI-flavoured MVVM) |
| DI | Hilt |
| Networking | Retrofit 3 + OkHttp 5 + Kotlinx Serialization |
| Push | Firebase Cloud Messaging |
| Crash reporting | Firebase Crashlytics |

See [DECISIONS.md](DECISIONS.md) for detailed architectural reasoning.

---

## Switching to the Real Backend

In `app/build.gradle.kts`, change the `defaultConfig` or add a build variant:

```kotlin
buildConfigField("boolean", "USE_FAKE_BACKEND", "false")
buildConfigField("String", "BASE_URL", "\"https://your.real.api.host/api/v1/\"")
```

No code changes required. The `FakeBackendInterceptor` is only wired in when `USE_FAKE_BACKEND = true`.

---

## Firebase Setup

Replace `app/google-services.json` with your project's real file from the Firebase Console.
The placeholder file allows the project to compile but Crashlytics and FCM will not function.

---

## Seeded Push Notification Test Data

To test the "notification deep link" flow without a live FCM push:

1. Build and launch the app in debug mode.
2. Use the **Debug screen** (bug icon in the top-right of the feed) to view the seeded IDs.
3. Craft an `adb` push or use the Firebase Console test message:

```
Subscription ID : 8127922c-a1f5-47c1-ac3e-9886058a9dce
Window ID       : 0d6b5c0e-9d57-4c8a-bd5a-6f3a7a3f2b10
```

FCM data payload:
```json
{
  "title": "Incident alert",
  "body": "Ransomware group claims breach and 2 more",
  "data": {
    "subscription_id": "8127922c-a1f5-47c1-ac3e-9886058a9dce",
    "window_id": "0d6b5c0e-9d57-4c8a-bd5a-6f3a7a3f2b10",
    "matched_count": "3"
  }
}
```

Tapping the resulting notification (or launching the app with these extras) opens the
**Window Matches** screen showing the 3 stories behind that notification.

---

## Debug Screen Features

- **Fake backend latency** — drag slider (0 – 5000 ms)
- **Force 401** — toggle to test token refresh flow
- **Force 500** — toggle to test error states
- **Force timeout** — toggle to test timeout handling
- **FCM Token** — copyable, for use in Firebase Console test sends
- **Device ID** — copyable, the registered backend device ID
- **Force crash** — fires a `RuntimeException` for Crashlytics testing
- **Log non-fatal** — records a non-fatal exception to Crashlytics

---

## Screens

| # | Screen | Description |
|---|---|---|
| 1 | News Feed | Paged card feed with section filter chips and debounced text search |
| 2 | Story Detail | Full story with MITRE techniques, CVEs, actor/sector/country tags |
| 3 | Subscriptions List | Pause/resume toggle (optimistic), delete with undo snackbar |
| 4 | Create / Edit Subscription | Dynamic cadence form, category filter chips |
| 5 | Subscription Detail | Subscription metadata + live matching stories (loaded in parallel) |
| 6 | Window Matches | Stories behind a specific push notification |
| 7 | Debug | Fake backend controls, FCM token, Crashlytics testing |

---

## Running Unit Tests

```bash
./gradlew :app:testDebugUnitTest
```

Key tests:
- `NewsFeedViewModelTest` — stale search result prevention, error state, section filtering
- `SubscriptionsListViewModelTest` — optimistic pause/resume, revert on failure, rapid tap serialization
- `FakeTokenProviderTest` — concurrent token access, refresh produces new token

---

## Code Quality & Resource Architecture

- **Strings & Localization:** All UI texts, buttons, placeholders, dialog titles, and relative time formats are moved to `res/values/strings.xml`.
- **Dimensions & Typography:** All layout measurements (`.dp`) and font/text sizes (`.sp`) are consolidated in `res/values/dimens.xml`, accessed in Compose via `dimensionResource` and `textDimensionResource`.
- **Colors:** Palette colors and contextual accent colors are unified in `res/values/colors.xml`.
- **Global Constants:** Configurable parameters, timeouts, timers, preference keys, and seeded test IDs are centralized in `com.example.threatlens.common.AppConstants`.
