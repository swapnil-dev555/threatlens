# CyberSentinel — Architecture & Decisions

## Architecture: MVI-flavoured MVVM

**Decision:** Use `ViewModel` + `StateFlow` (single immutable `UiState` per screen) collected with
`collectAsStateWithLifecycle()`. This is lifecycle-aware, plays well with Compose recomposition, and
avoids the complexity of a full MVI framework while getting all the benefits.

| Layer | Contents |
|---|---|
| **UI** | Compose screens + `@HiltViewModel` ViewModels |
| **Domain** | Business logic lives in ViewModels (app is CRUD-heavy; a separate domain layer would add boilerplate with no gain at this scale) |
| **Data** | Repositories → Retrofit API → OkHttp interceptors |

---

## Dependency Injection: Hilt

**Reason:** Hilt is the standard DI solution for Android, integrates with ViewModel (`@HiltViewModel`),
and requires minimal boilerplate compared to manual Dagger. It also works with `@AndroidEntryPoint`
for `FirebaseMessagingService`.

---

## Networking: Retrofit 3.0 + OkHttp 5 + Kotlinx Serialization

**Reason:**
- Retrofit 3.0 ships Kotlin Coroutines support natively (no `CallAdapter` needed).
- OkHttp 5 is the latest stable.
- Kotlinx Serialization avoids reflection (R8-friendly) and handles `@SerialName` annotations cleanly.
- Moshi was considered but Kotlinx Serialization has first-class Kotlin support and is lighter on the R8 keep rules.

---

## Fake Backend: OkHttp Interceptor

**Reason:** Placing the fake at the interceptor level means the app's real networking stack
(OkHttp, Retrofit, serialization, error handling) runs against it. This is exactly what the brief requires.
An alternative (MockWebServer) would need a local server port and doesn't survive process boundary well.
The interceptor is controlled by `BuildConfig.USE_FAKE_BACKEND` (true in debug, false in release).

---

## Image Loading: Coil 3

**Reason:** Coil 3 has first-class Compose support and is Kotlin-first. Glide and Picasso are less
Compose-idiomatic. (Note: news stories don't have images in the API, so Coil is included for future use.)

---

## Navigation: Navigation Compose with type-safe routes

**Reason:** Navigation Compose 2.8+ supports type-safe routes via Kotlin Serialization annotations on
sealed objects. This eliminates stringly-typed route definitions and argument parsing boilerplate.

---

## State Restoration: `SavedStateHandle`

**Decision:** ViewModels receive their route arguments via `SavedStateHandle` (injected by Hilt).
This survives process death automatically — the Navigation library restores the back stack and Hilt
re-injects the handle with the correct arguments.

For the subscription form, all form field state lives in the `SubscriptionFormState` data class inside
the ViewModel's `MutableStateFlow`. The ViewModel itself survives rotation. For process death, the
`savedStateHandle` has the `subscriptionId` so the form can reload. Field-level process-death survival
would require `rememberSaveable` in the UI or `SavedStateHandle` serialization — not implemented for
brevity but the architecture supports it.

---

## Concurrency: Coroutines + Flow

- All network calls are `suspend fun` in repositories.
- ViewModels use `viewModelScope`.
- The token provider uses a `Mutex` to serialize concurrent refresh requests.
- The pause/resume toggle uses a per-subscription `Mutex` and cancels the previous job on rapid taps,
  ensuring the UI and server always converge.
- Search debounce uses `Flow.debounce` + `collectLatest` so stale search results are always discarded.

---

## Authentication

**Decision:** `TokenProvider` is an interface. `FakeTokenProvider` generates tokens that expire every
3 minutes and refreshing takes ~1.2s (simulated). `AuthInterceptor` adds the header and retries once
on 401 (after refresh). Switching to a real auth provider (e.g., Firebase Auth, OAuth) means
implementing `TokenProvider` and binding it in the DI module.

---

## Firebase Crashlytics Usage

Beyond default crash collection:
- `setCustomKey("app_version", ...)` and `"build_type"` on startup.
- `setCustomKey("device_id", ...)` after FCM registration.
- `setCustomKey("push_window_id", ...)` and `"push_subscription_id"` when a push arrives.
- `setCustomKey("api_error_type", ...)` and `"api_error_message"` on every API failure.
- `setCustomKey("feed_error", ...)` on feed load failure.
- Non-fatal `recordException()` on every caught error in repositories and ViewModels.
- `keepattributes SourceFile,LineNumberTable` and `renamesourcefileattribute SourceFile` in ProGuard
  so release stack traces remain readable in Crashlytics.

---

## Switching to the Real Backend

Change one line in `app/build.gradle.kts`:

```kotlin
// In defaultConfig or a "real" build variant:
buildConfigField("boolean", "USE_FAKE_BACKEND", "false")
buildConfigField("String", "BASE_URL", "\"https://your.real.api.host/api/v1/\"")
```

The `FakeBackendInterceptor` is only added to `OkHttpClient` when `USE_FAKE_BACKEND` is `true`.
No other code changes are needed.

---

## Theme

Custom dark "cyber" colour palette (dark navy + cyan primary) using Material 3. Dynamic colour
is disabled by default to preserve the security-analyst aesthetic. Users on Android 12+ can still
enable it by toggling `dynamicColor = true`.

---

## Accessibility

- All `Icon` usages have `contentDescription` or `null` with a sibling text label.
- `FilterChip`, `AssistChip`, `SuggestionChip` are used for semantic meaning.
- `collectAsStateWithLifecycle` is used throughout for lifecycle-aware collection.

---

## Parallel Loading & Partial Failure Handling: Subscription Details

**Decision:**
The subscription details and its current matches preview (`GET /news-subscriptions/{id}/matches`) are dispatched **in parallel** via `async { ... }` within `SubscriptionDetailViewModel`. Both operations are independently tracked with their own loading and error states (`isLoadingSubscription`, `isLoadingMatches`, `subscriptionError`, `matchesError`).

### Screen Behaviour Matrix

| Subscription Request | Matches Request | Screen Display Behaviour |
|---|---|---|
| **Succeeds** | **Succeeds** | Full subscription hero card, status badge, schedule, Monitored Intelligence Criteria tags, full matches list preview with counter, and pause/delete actions. |
| **Succeeds** | **Fails** | Full subscription details and tags remain visible and functional. In the matches section, a highlighted error banner is displayed (`Could not load matches: ...`) with a dedicated **"Retry Matches"** button (`retryMatches()`). |
| **Fails** | **Succeeds** | A prominent warning banner is displayed at the top (`Subscription details unavailable: ...`) with a **"Retry Details"** button (`retrySubscription()`). Below it, the **Matches preview is fully visible and interactive** — the analyst can see matching stories and tap on any match to open it. |
| **Fails** | **Fails** | Full-screen `ErrorScreen` is shown (`Failed to load subscription details & matches`) with a unified **"Retry"** button (`retry()`). |
| **Loading** | **Loading** | Full-screen `LoadingScreen` is shown initially until the first response arrives. |

---

## Subscription Match Story Navigation (`GET /news-subscriptions/{id}/matches/{activity_key}`)

**Decision:**
When an analyst taps any matched story card within `SubscriptionDetailScreen` or `WindowMatchesScreen`, the app navigates with `Screen.StoryDetail(activityKey, subscriptionId)`.
- `StoryDetailViewModel` checks `subscriptionId`.
- If present, it executes `SubscriptionRepository.getSubscriptionMatchStory(subscriptionId, activityKey)`.
- If absent (e.g. from general news feed), it falls back to `NewsRepository.getStory(activityKey)`.
- `StoryDetailScreen` displays an extra visual badge `"Subscription Match"` indicating the threat intelligence context.

---

## Subscription Push Notification & Fake Backend Integration

**Decision:**
When the user creates a new subscription via `POST /news-subscriptions`:
- In `FakeBackendInterceptor`, after persisting the subscription, the fake backend automatically calculates matching threat stories via `filterStoriesBySubscription(sub)` and triggers an instant push notification via `NotificationHelper.showNotification(...)`.
- The notification displays the subscription title, match count, and passes `extra_subscription_id` and a unique `extra_window_id`.
- Tapping the notification routes directly to `WindowMatchesScreen(subscriptionId, windowId)`, allowing immediate inspection of threat intelligence stories matching the new subscription.

---

## Android 13+ Notification Permission & Denial Handling

**Decision:**
On Android 13+ (API 33+), `POST_NOTIFICATIONS` is a runtime permission:
1. **Graceful Non-Blocking Flow:** The app requests permission on first launch (`MainActivity`), but never blocks core browsing if denied.
2. **Contextual In-App Banners:** In `SubscriptionFormScreen` (when configuring alerts) and `SubscriptionsListScreen`, if notification permission is not granted, a non-intrusive banner informs the user: *"Push alerts are disabled. Enable notifications so you don't miss critical threat intelligence."*
3. **Explicit Denial Rationale Dialog:** If the user denies the permission prompt in `SubscriptionFormScreen`, a clear explanation dialog explains why notifications are needed and provides an action button to open **App Settings** (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`) or continue without alerts.
4. **Defensive Notification Dispatching:** `NotificationHelper.showNotification()` validates `areNotificationsEnabled()` before posting, ensuring no `SecurityException` or silent system failures occur when permission is revoked.

---

## Centralized Resource Extraction & Global Constants

**Decision:**
To ensure strict adherence to Android localization, theming, and layout consistency guidelines:
1. **String Localization (`strings.xml`):** All static UI strings, headers, button labels, screen titles, dialog copies, snackbar messages, and relative time format patterns are extracted into `res/values/strings.xml`.
2. **Dimension Standard (`dimens.xml` + `textDimensionResource`):** All layout spacing, margins, padding, corner radii, borders, and component sizes (`.dp`) as well as typography font sizes and line heights (`.sp`) are extracted to `res/values/dimens.xml`. A Compose helper `textDimensionResource(id)` safely converts dimension definitions into `TextUnit` (`.sp`) using `LocalDensity`.
3. **Color Palette Standardization (`colors.xml`):** Hardcoded hex codes and color literals are moved to `res/values/colors.xml` and referenced using `colorResource(id)` across Compose screens.
4. **Global Constants (`AppConstants.kt`):** Application-wide constants (such as timeouts, search debounce durations, carousel auto-swipe delays, seeded testing IDs, and DataStore preference keys) are unified under `com.example.threatlens.common.AppConstants`.

---

## What was not implemented (and why)

| Feature | Status |
|---|---|
| Type-ahead filter-options search | Skipped — UI would add complexity without changing the architecture |
| Offline cache | Skipped — requires Room + a caching strategy; architecture supports adding it as a data source |
| Multi-module | Single-module for speed; the package structure (`data/`, `ui/`, `di/`) mirrors a multi-module layout |
| CI workflow | Skipped — no CI environment available |
| Compose UI tests | Unit tests cover the critical concurrent behaviours; Compose UI tests skipped for time |
