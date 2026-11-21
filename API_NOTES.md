# CyberSentinel — API Notes

Gaps and inconsistencies found in `news-api.md` and the decisions made.

---

## 1. `GET /cyber-news/{activity_key}` — `is_expand=false` returns a card

**Gap:** The endpoint says `is_expand=false` returns a card as in endpoint 3.
Both endpoint 3 (card) and endpoint 4 (story) share the same `activity_key` path parameter but return different shapes.

**Decision:** The API surface returns a `NewsStory` object by default (since `is_expand` defaults to `true`).
The `NewsCard` variant is only used when the caller explicitly requests `is_expand=false`. The app always
calls with `is_expand=true` for story detail, so only `NewsStory` is deserialized here.

---

## 2. `GET /news-subscriptions/{id}/matches/{activity_key}` — same shape as endpoint 4

**Gap:** Endpoint 12 says it returns "a full story (as in endpoint 4)." The response shape is identical to
endpoint 4 and 3 depending on `is_expand`.

**Decision:** Reuse the same `NewsStory` model and the existing `getStory()` repository call. The
`subscription_id` context is passed in the URL but doesn't change the response shape, so we just
navigate to the standard `StoryDetailScreen`.

---

## 3. Cadence field validation — "must be unset" vs. nullable

**Gap:** The API doc says cadence fields that "must be unset" for a given cadence. The doc doesn't say
whether to send `null` or omit the key entirely.

**Decision:** Use `@kotlinx.serialization.Serializable` with `explicitNulls = false` in the `Json`
instance used in `NetworkModule`. This means fields with `null` values are **omitted** from the JSON
body, satisfying "must be unset." This matches common REST API conventions.

---

## 4. `PATCH` — empty list clears filter, omitted field leaves it alone

**Gap:** The spec says "An omitted field is left alone. An empty list clears that filter."
Kotlin's `null` vs empty `List` is ambiguous without careful serialization.

**Decision:** In `PatchSubscriptionRequest`, a `null` list field means "omit from JSON" (leave alone)
and an explicit `emptyList()` means "send `[]`" (clear). The `SubscriptionFormViewModel.buildPatch()`
method only sets a field when it differs from the existing subscription, never sending `null` lists.
An explicit clear (setting a filter to empty) is not yet surfaced in the UI but the data model supports it.

---

## 5. `POST /notification-devices` — 200 not 201

**Gap:** Endpoint 5 shows `200` as the response code (not `201` like `POST /news-subscriptions`).
This is inconsistent but intentional per the API doc (registering a token is idempotent — it moves
ownership if the token already exists).

**Decision:** Accept 200 for device registration, 201 for subscription creation. Both are handled by
Retrofit's `Response<T>` wrapper in the repository.

---

## 6. `DELETE /news-subscriptions/{id}` — 204 No Content

**Gap:** 204 means no body. Retrofit by default will fail if you use a typed return but get an empty body.

**Decision:** Use `Response<Unit>` as the return type for the delete endpoint. The repository checks
`response.isSuccessful` (204 qualifies) rather than parsing the body.

---

## 7. Push notification — `matched_count` is a string in FCM data payload

**Gap:** FCM data payloads are always `Map<String, String>`. The API doc shows `"matched_count": "3"` as a
string even though conceptually it's a number.

**Decision:** Parse `data["matched_count"]?.toIntOrNull() ?: 0` in `CyberNewsMessagingService`.

---

## 8. `next_fire_at` recalculation

**Gap:** The doc says "resuming a paused subscription recomputes `next_fire_at`." The fake backend
recalculates `next_fire_at` when `cadence` changes or when `is_paused` becomes `false`. The real backend
presumably does the same.

**Decision:** The fake backend applies this rule in `handlePatchSubscription()`.

---

## 9. Subscription list — `next_fire_at` timezone display

**Gap:** `next_fire_at` is stored as UTC but should be displayed in the subscription's `timezone`.

**Decision:** `nextFireFormatted()` in `SubscriptionsListViewModel.kt` converts the UTC instant to the
subscription's `timezone` using `java.time.ZoneId.of(timezone)`. If the timezone is invalid, we fall
back to raw string display.
