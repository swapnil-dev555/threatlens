package com.example.threatlens.data.fake

import com.example.threatlens.data.debug.DebugSettings
import com.example.threatlens.data.fcm.NotificationHelper
import com.example.threatlens.data.model.*
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that intercepts ALL requests and returns fake responses
 * matching the news-api.md contract. This is the "fake backend at the HTTP level."
 *
 * Seeded data:
 * - 65 news stories across all 7 sections
 * - 2 seeded subscriptions
 * - 1 seeded push window (window_id & subscription_id documented in README)
 *
 * Fixed IDs for testing:
 * - Subscription ID: "8127922c-a1f5-47c1-ac3e-9886058a9dce"
 * - Window ID: "0d6b5c0e-9d57-4c8a-bd5a-6f3a7a3f2b10"
 */
@Singleton
class FakeBackendInterceptor(
    private val debugSettings: DebugSettings,
    private val notificationHelper: NotificationHelper? = null
) : Interceptor {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
    }
    private val jsonMediaType = "application/json".toMediaType()

    // ── Mutable in-memory state ──
    private val subscriptions = mutableListOf<Subscription>()
    private var registeredDevice: DeviceResponse? = null
    // Seeded fixed IDs
    companion object {
        const val SEEDED_SUB_ID = com.example.threatlens.common.AppConstants.SEEDED_SUB_ID
        const val SEEDED_SUB_ID_2 = com.example.threatlens.common.AppConstants.SEEDED_SUB_ID_2
        const val SEEDED_WINDOW_ID = com.example.threatlens.common.AppConstants.SEEDED_WINDOW_ID
    }

    init {
        seedData()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        val url = request.url

        // Apply debug overrides
        if (debugSettings.forceTimeout) {
            Thread.sleep(35_000)
            throw SocketTimeoutException("Forced timeout by debug settings")
        }

        // Apply configurable latency
        val latency = debugSettings.latencyMs.value
        if (latency > 0) {
            Thread.sleep(latency)
        }

        if (debugSettings.force500) {
            return request.response(500, """{"error":"Internal Server Error (forced by debug)"}""")
        }

        // Check auth (unless registering device)
        val authHeader = request.header("Authorization")
        if (debugSettings.force401 || authHeader == null || !authHeader.startsWith("Bearer ")) {
            return request.response(401, """{"error":"Unauthorized"}""")
        }

        // Route the request
        val apiPath = path.removePrefix("/api/v1")

        return when {
            // 1. GET /cyber-news/sections
            method == "GET" && apiPath == "/cyber-news/sections" -> {
                request.handleGetSections()
            }
            // 2. GET /cyber-news/filter-options
            method == "GET" && apiPath == "/cyber-news/filter-options" -> {
                val field = url.queryParameter("field") ?: return request.response(400, """{"error":"field is required"}""")
                val q = url.queryParameter("q")
                request.handleGetFilterOptions(field, q)
            }
            // 3. GET /cyber-news
            method == "GET" && apiPath == "/cyber-news" -> {
                val limit = url.queryParameter("limit")?.toIntOrNull() ?: 20
                val offset = url.queryParameter("offset")?.toIntOrNull() ?: 0
                val sectionKey = url.queryParameter("section_key")
                val query = url.queryParameter("q")
                request.handleGetNews(limit, offset, sectionKey, query)
            }
            // 4. GET /cyber-news/{activity_key}
            method == "GET" && apiPath.startsWith("/cyber-news/") && !apiPath.contains("sections") && !apiPath.contains("filter") -> {
                val activityKey = apiPath.removePrefix("/cyber-news/")
                request.handleGetStory(activityKey)
            }
            // 5. POST /notification-devices
            method == "POST" && apiPath == "/notification-devices" -> {
                val body = request.body?.let {
                    val buffer = okio.Buffer()
                    it.writeTo(buffer)
                    buffer.readUtf8()
                } ?: ""
                request.handleRegisterDevice(body)
            }
            // 6. DELETE /notification-devices/{id}
            method == "DELETE" && apiPath.startsWith("/notification-devices/") -> {
                request.handleRevokeDevice()
            }
            // 7. GET /news-subscriptions
            method == "GET" && apiPath == "/news-subscriptions" -> {
                val limit = url.queryParameter("limit")?.toIntOrNull() ?: 20
                val offset = url.queryParameter("offset")?.toIntOrNull() ?: 0
                request.handleGetSubscriptions(limit, offset)
            }
            // 8. POST /news-subscriptions
            method == "POST" && apiPath == "/news-subscriptions" -> {
                val body = request.body?.let {
                    val buffer = okio.Buffer()
                    it.writeTo(buffer)
                    buffer.readUtf8()
                } ?: ""
                request.handleCreateSubscription(body)
            }
            // 13. GET /news-subscriptions/{id}/windows/{window_id}/matches  (must come before matches)
            method == "GET" && Regex("/news-subscriptions/[^/]+/windows/[^/]+/matches").matches(apiPath) -> {
                val parts = apiPath.split("/").filter { it.isNotEmpty() }
                val subId = parts[1]
                val windowId = parts[3]
                val limit = url.queryParameter("limit")?.toIntOrNull() ?: 20
                val offset = url.queryParameter("offset")?.toIntOrNull() ?: 0
                request.handleGetWindowMatches(subId, windowId, limit, offset)
            }
            // 12. GET /news-subscriptions/{id}/matches/{activity_key}
            method == "GET" && Regex("/news-subscriptions/[^/]+/matches/[^/]+").matches(apiPath) -> {
                val parts = apiPath.split("/").filter { it.isNotEmpty() }
                val activityKey = parts.last()
                request.handleGetStory(activityKey)
            }
            // 11. GET /news-subscriptions/{id}/matches
            method == "GET" && apiPath.matches(Regex("/news-subscriptions/[^/]+/matches")) -> {
                val subId = apiPath.split("/")[2]
                val limit = url.queryParameter("limit")?.toIntOrNull() ?: 20
                val offset = url.queryParameter("offset")?.toIntOrNull() ?: 0
                request.handleGetSubscriptionMatches(subId, limit, offset)
            }
            // GET /news-subscriptions/{id} (single subscription)
            method == "GET" && apiPath.matches(Regex("/news-subscriptions/[^/]+")) -> {
                val id = apiPath.split("/").last()
                val sub = subscriptions.find { it.id == id }
                if (sub != null) {
                    request.jsonResponse(200, json.encodeToString(sub))
                } else {
                    request.jsonResponse(404, """{"error":"Subscription not found"}""")
                }
            }
            // 9. PATCH /news-subscriptions/{id}
            method == "PATCH" && apiPath.matches(Regex("/news-subscriptions/[^/]+")) -> {
                val id = apiPath.split("/").last()
                val body = request.body?.let {
                    val buffer = okio.Buffer()
                    it.writeTo(buffer)
                    buffer.readUtf8()
                } ?: ""
                request.handlePatchSubscription(id, body)
            }
            // 10. DELETE /news-subscriptions/{id}
            method == "DELETE" && apiPath.matches(Regex("/news-subscriptions/[^/]+")) -> {
                val id = apiPath.split("/").last()
                request.handleDeleteSubscription(id)
            }

            else -> request.response(404, """{"error":"Not found: $method $apiPath"}""")
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Handlers
    // ══════════════════════════════════════════════════════════════

    private fun Request.handleGetSections(): Response {
        val sections = SectionsResponse(
            items = listOf(
                Section("general_threat_activity", "General Threat Activity"),
                Section("geopolitical_nation_state_watch", "Geopolitical / Nation-State Watch"),
                Section("incidents_and_breaches", "Incidents and Breaches"),
                Section("law_enforcement_takedowns", "Law Enforcement & Takedowns"),
                Section("notable_vulnerabilities", "Notable Vulnerabilities"),
                Section("ransomware_activity", "Ransomware Activity"),
                Section("third_party_supply_chain_risk", "Third-Party / Supply Chain Risk")
            )
        )
        return jsonResponse(200, json.encodeToString(sections))
    }

    private fun Request.handleGetFilterOptions(field: String, q: String?): Response {
        val allOptions = when (field) {
            "categories" -> listOf(
                FilterOption("general_threat_activity", "General Threat Activity"),
                FilterOption("geopolitical_nation_state_watch", "Geopolitical / Nation-State Watch"),
                FilterOption("incidents_and_breaches", "Incidents and Breaches"),
                FilterOption("law_enforcement_takedowns", "Law Enforcement & Takedowns"),
                FilterOption("notable_vulnerabilities", "Notable Vulnerabilities"),
                FilterOption("ransomware_activity", "Ransomware Activity"),
                FilterOption("third_party_supply_chain_risk", "Third-Party / Supply Chain Risk")
            )
            "actors" -> listOf(
                FilterOption("LockBit", "LockBit"), FilterOption("APT28", "APT28"),
                FilterOption("Lazarus Group", "Lazarus Group"), FilterOption("APT29", "APT29"),
                FilterOption("Cl0p", "Cl0p"), FilterOption("BlackCat", "BlackCat"),
                FilterOption("Volt Typhoon", "Volt Typhoon"), FilterOption("Kimsuky", "Kimsuky")
            )
            "ransomware_families" -> listOf(
                FilterOption("LockBit 3.0", "LockBit 3.0"), FilterOption("BlackCat/ALPHV", "BlackCat/ALPHV"),
                FilterOption("Cl0p", "Cl0p"), FilterOption("Royal", "Royal"),
                FilterOption("Play", "Play"), FilterOption("Akira", "Akira")
            )
            "target_sectors" -> listOf(
                FilterOption("Healthcare", "Healthcare"), FilterOption("Finance", "Finance"),
                FilterOption("Government", "Government"), FilterOption("Education", "Education"),
                FilterOption("Technology", "Technology"), FilterOption("Energy", "Energy"),
                FilterOption("Manufacturing", "Manufacturing"), FilterOption("Retail", "Retail")
            )
            "target_countries" -> listOf(
                FilterOption("India", "India"), FilterOption("United States", "United States"),
                FilterOption("United Kingdom", "United Kingdom"), FilterOption("Germany", "Germany"),
                FilterOption("Japan", "Japan"), FilterOption("Australia", "Australia"),
                FilterOption("France", "France"), FilterOption("Indonesia", "Indonesia"),
                FilterOption("Brazil", "Brazil"), FilterOption("Canada", "Canada")
            )
            "vendors" -> listOf(
                FilterOption("Fortinet", "Fortinet"), FilterOption("Cisco", "Cisco"),
                FilterOption("Microsoft", "Microsoft"), FilterOption("Palo Alto Networks", "Palo Alto Networks"),
                FilterOption("VMware", "VMware"), FilterOption("Ivanti", "Ivanti")
            )
            "products" -> listOf(
                FilterOption("FortiOS", "FortiOS"), FilterOption("Exchange Server", "Exchange Server"),
                FilterOption("Windows", "Windows"), FilterOption("Chrome", "Chrome"),
                FilterOption("vCenter", "vCenter"), FilterOption("PAN-OS", "PAN-OS")
            )
            "tools" -> listOf(
                FilterOption("Cobalt Strike", "Cobalt Strike"), FilterOption("Mimikatz", "Mimikatz"),
                FilterOption("Metasploit", "Metasploit"), FilterOption("Brute Ratel", "Brute Ratel"),
                FilterOption("Sliver", "Sliver")
            )
            "techniques" -> listOf(
                FilterOption("T1486", "T1486 - Data Encrypted for Impact"),
                FilterOption("T1059", "T1059 - Command and Scripting Interpreter"),
                FilterOption("T1078", "T1078 - Valid Accounts"),
                FilterOption("T1190", "T1190 - Exploit Public-Facing Application"),
                FilterOption("T1566", "T1566 - Phishing")
            )
            else -> emptyList()
        }

        val filtered = if (q.isNullOrBlank()) allOptions
        else allOptions.filter { it.label.startsWith(q, ignoreCase = true) }

        return jsonResponse(200, json.encodeToString(FilterOptionsResponse(filtered)))
    }

    private fun Request.handleGetNews(limit: Int, offset: Int, sectionKey: String?, query: String?): Response {
        var filtered = seededStories
        if (!sectionKey.isNullOrBlank()) {
            filtered = filtered.filter { it.sectionLabel.toSectionKey() == sectionKey }
        }
        if (!query.isNullOrBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.atAGlance.contains(query, ignoreCase = true)
            }
        }
        val cards = filtered.map { it.toCard() }
        val page = paginate(cards, limit, offset)
        return jsonResponse(200, json.encodeToString(page))
    }

    private fun Request.handleGetStory(activityKey: String): Response {
        val decoded = try { java.net.URLDecoder.decode(activityKey, "UTF-8") } catch (_: Exception) { activityKey }
        val story = seededStories.find { it.activityKey == activityKey || it.activityKey == decoded }
            ?: seededStories.find { it.activityKey.endsWith(activityKey.takeLast(12)) }
            ?: seededStories.firstOrNull()
            ?: return jsonResponse(404, """{"error":"Story not found"}""")
        return jsonResponse(200, json.encodeToString(story))
    }

    private fun Request.handleRegisterDevice(body: String): Response {
        val req = json.decodeFromString<RegisterDeviceRequest>(body)
        val device = DeviceResponse(
            id = registeredDevice?.id ?: UUID.randomUUID().toString(),
            platform = req.platform,
            deviceLabel = req.deviceLabel,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
            lastSeenAt = Instant.now().toString()
        )
        registeredDevice = device
        return jsonResponse(200, json.encodeToString(device))
    }

    private fun Request.handleRevokeDevice(): Response {
        registeredDevice = null
        return jsonResponse(200, json.encodeToString(RevokeResponse("revoked")))
    }

    private fun Request.handleGetSubscriptions(limit: Int, offset: Int): Response {
        val active = subscriptions.toList()
        val page = paginate(active, limit, offset)
        return jsonResponse(200, json.encodeToString(page))
    }

    private fun Request.handleCreateSubscription(body: String): Response {
        val req = json.decodeFromString<CreateSubscriptionRequest>(body)
        val now = Instant.now().toString()
        val sub = Subscription(
            id = UUID.randomUUID().toString(),
            name = req.name,
            categories = req.categories ?: emptyList(),
            actors = req.actors ?: emptyList(),
            ransomwareFamilies = req.ransomwareFamilies ?: emptyList(),
            products = req.products ?: emptyList(),
            tools = req.tools ?: emptyList(),
            techniques = req.techniques ?: emptyList(),
            vendors = req.vendors ?: emptyList(),
            targetSectors = req.targetSectors ?: emptyList(),
            targetCountries = req.targetCountries ?: emptyList(),
            cadence = req.cadence,
            cadenceMinute = req.cadenceMinute,
            cadenceTime = req.cadenceTime,
            cadenceDayOfWeek = req.cadenceDayOfWeek,
            cadenceDayOfMonth = req.cadenceDayOfMonth,
            cadenceMonth = req.cadenceMonth,
            timezone = req.timezone,
            nextFireAt = computeNextFire(req.cadence),
            isPaused = false,
            createdAt = now,
            updatedAt = now
        )
        subscriptions.add(sub)

        // Trigger push notification on subscription creation if notificationHelper is available
        val matchedStories = filterStoriesBySubscription(sub)
        val count = matchedStories.size
        val windowId = UUID.randomUUID().toString()
        val targetStory = matchedStories.firstOrNull() ?: seededStories.firstOrNull()
        notificationHelper?.showNotification(
            title = targetStory?.title ?: "New Subscription Active: ${sub.name}",
            body = targetStory?.summary?.ifBlank { targetStory.title }
                ?: if (count > 0) {
                    "$count matching threat intelligence stories found for your new subscription."
                } else {
                    "Subscription '${sub.name}' is now active and monitoring threats."
                },
            subscriptionId = sub.id,
            windowId = windowId,
            activityKey = targetStory?.activityKey,
            matchedCount = count
        )

        return jsonResponse(201, json.encodeToString(sub))
    }

    private fun Request.handlePatchSubscription(id: String, body: String): Response {
        val idx = subscriptions.indexOfFirst { it.id == id }
        if (idx == -1) return jsonResponse(404, """{"error":"Not found"}""")

        val patch = json.decodeFromString<PatchSubscriptionRequest>(body)
        val existing = subscriptions[idx]
        val updated = existing.copy(
            name = patch.name ?: existing.name,
            categories = patch.categories ?: existing.categories,
            actors = patch.actors ?: existing.actors,
            ransomwareFamilies = patch.ransomwareFamilies ?: existing.ransomwareFamilies,
            products = patch.products ?: existing.products,
            tools = patch.tools ?: existing.tools,
            techniques = patch.techniques ?: existing.techniques,
            vendors = patch.vendors ?: existing.vendors,
            targetSectors = patch.targetSectors ?: existing.targetSectors,
            targetCountries = patch.targetCountries ?: existing.targetCountries,
            cadence = patch.cadence ?: existing.cadence,
            cadenceMinute = patch.cadenceMinute ?: existing.cadenceMinute,
            cadenceTime = patch.cadenceTime ?: existing.cadenceTime,
            cadenceDayOfWeek = patch.cadenceDayOfWeek ?: existing.cadenceDayOfWeek,
            cadenceDayOfMonth = patch.cadenceDayOfMonth ?: existing.cadenceDayOfMonth,
            cadenceMonth = patch.cadenceMonth ?: existing.cadenceMonth,
            timezone = patch.timezone ?: existing.timezone,
            isPaused = patch.isPaused ?: existing.isPaused,
            nextFireAt = if (patch.cadence != null || patch.isPaused == false)
                computeNextFire(patch.cadence ?: existing.cadence) else existing.nextFireAt,
            updatedAt = Instant.now().toString()
        )
        subscriptions[idx] = updated
        return jsonResponse(200, json.encodeToString(updated))
    }

    private fun Request.handleDeleteSubscription(id: String): Response {
        subscriptions.removeAll { it.id == id }
        return jsonResponse(204, "")
    }

    private fun Request.handleGetSubscriptionMatches(subId: String, limit: Int, offset: Int): Response {
        val sub = subscriptions.find { it.id == subId }
            ?: return jsonResponse(404, """{"error":"Subscription not found"}""")
        val matched = filterStoriesBySubscription(sub)
        val cards = matched.map { it.toCard() }
        val page = paginate(cards, limit, offset)
        return jsonResponse(200, json.encodeToString(page))
    }

    private fun Request.handleGetWindowMatches(subId: String, windowId: String, limit: Int, offset: Int): Response {
        val sub = subscriptions.find { it.id == subId }
        // For the seeded window, return a fixed set of stories
        val matchedStories = if (windowId == SEEDED_WINDOW_ID) {
            seededStories.take(3).map { it.toCard() }
        } else {
            sub?.let { filterStoriesBySubscription(it).map { s -> s.toCard() } } ?: emptyList()
        }

        val paged = matchedStories.drop(offset).take(limit)
        val totalCount = matchedStories.size
        val totalPages = if (totalCount == 0) 1 else (totalCount + limit - 1) / limit

        val windowResponse = WindowMatchesResponse(
            count = totalCount,
            page = offset / limit + 1,
            size = limit,
            totalPages = totalPages,
            limit = limit,
            offset = offset,
            hasNext = offset + limit < totalCount,
            hasPrevious = offset > 0,
            window = PushWindow(
                id = windowId,
                windowStartAt = "2026-10-23T13:31:00Z",
                windowEndAt = "2026-10-23T13:33:00Z",
                matchedCount = totalCount,
                sentAt = "2026-10-23T17:23:56.633701Z"
            ),
            items = paged
        )
        return jsonResponse(200, json.encodeToString(windowResponse))
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════

    private fun filterStoriesBySubscription(sub: Subscription): List<NewsStory> {
        return seededStories.filter { story ->
            val catMatch = sub.categories.isEmpty() ||
                    sub.categories.any { it.equals(story.sectionLabel.toSectionKey(), ignoreCase = true) }
            val sectorMatch = sub.targetSectors.isEmpty() ||
                    story.targetSectors.any { ts -> sub.targetSectors.any { it.equals(ts, ignoreCase = true) } }
            val countryMatch = sub.targetCountries.isEmpty() ||
                    story.targetCountries.any { tc -> sub.targetCountries.any { it.equals(tc, ignoreCase = true) } }
            catMatch && sectorMatch && countryMatch
        }
    }

    private fun computeNextFire(cadence: String): String {
        val now = Instant.now()
        val next = when (cadence) {
            "realtime" -> now.plusSeconds(60)
            "hourly" -> now.plusSeconds(3600)
            "daily" -> now.plusSeconds(86400)
            "weekly" -> now.plusSeconds(604800)
            "monthly" -> now.plusSeconds(2592000)
            "yearly" -> now.plusSeconds(31536000)
            else -> now.plusSeconds(3600)
        }
        return next.toString()
    }

    private fun NewsStory.toCard() = NewsCard(
        activityKey = activityKey,
        title = title,
        atAGlance = atAGlance,
        publishedTs = publishedTs,
        sectionLabel = sectionLabel,
        readTimeMinutes = readTimeMinutes
    )

    private fun String.toSectionKey(): String = lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .replace(Regex("^_|_$"), "")

    private inline fun <reified T> paginate(items: List<T>, limit: Int, offset: Int): PaginatedResponse<T> {
        val total = items.size
        val paged = items.drop(offset).take(limit)
        val totalPages = if (total == 0) 1 else (total + limit - 1) / limit
        return PaginatedResponse(
            count = total,
            page = offset / limit + 1,
            size = limit,
            totalPages = totalPages,
            limit = limit,
            offset = offset,
            hasNext = offset + limit < total,
            hasPrevious = offset > 0,
            items = paged
        )
    }

    private fun Request.response(code: Int, body: String): Response {
        return jsonResponse(code, body)
    }

    private fun Request.jsonResponse(code: Int, body: String): Response {
        return Response.Builder()
            .request(this)
            .code(code)
            .message(when (code) {
                200 -> "OK"
                201 -> "Created"
                204 -> "No Content"
                else -> "Error"
            })
            .protocol(Protocol.HTTP_1_1)
            .body(body.toResponseBody(jsonMediaType))
            .build()
    }

    // ══════════════════════════════════════════════════════════════
    // Seeded Data — 65 stories across all 7 sections
    // ══════════════════════════════════════════════════════════════

    private fun seedData() {
        subscriptions.clear()
        val now = Instant.now().toString()

        subscriptions.add(
            Subscription(
                id = SEEDED_SUB_ID,
                name = "Incident alert",
                categories = listOf("incidents_and_breaches"),
                cadence = "daily",
                cadenceTime = "09:00:00",
                timezone = "Asia/Kolkata",
                nextFireAt = Instant.now().plusSeconds(120).toString(),
                createdAt = now, updatedAt = now
            )
        )
        subscriptions.add(
            Subscription(
                id = SEEDED_SUB_ID_2,
                name = "All threat intel",
                categories = listOf(
                    "general_threat_activity", "geopolitical_nation_state_watch",
                    "incidents_and_breaches", "law_enforcement_takedowns",
                    "notable_vulnerabilities", "ransomware_activity",
                    "third_party_supply_chain_risk"
                ),
                cadence = "daily",
                cadenceTime = "09:00:00",
                timezone = "Asia/Kolkata",
                nextFireAt = Instant.now().plusSeconds(3600).toString(),
                createdAt = now, updatedAt = now
            )
        )
    }

    val seededStories: List<NewsStory> by lazy { generateStories() }

    fun getSeededStory(): NewsStory? = seededStories.randomOrNull() ?: seededStories.firstOrNull()

    private fun generateStories(): List<NewsStory> {
        val stories = mutableListOf<NewsStory>()
        val baseTime = Instant.parse("2026-10-23T13:00:00Z")

        data class Template(
            val section: String,
            val title: String,
            val summary: String,
            val atAGlance: String,
            val actors: List<String> = emptyList(),
            val malware: List<String> = emptyList(),
            val tools: List<String> = emptyList(),
            val techniques: List<String> = emptyList(),
            val cves: List<String> = emptyList(),
            val vendors: List<String> = emptyList(),
            val products: List<String> = emptyList(),
            val sectors: List<String> = emptyList(),
            val countries: List<String> = emptyList(),
            val isRansomware: Boolean = false
        )

        val templates = listOf(
            // Incidents and Breaches (10)
            Template("Incidents and Breaches", "Ransomware group claims breach of regional hospital network", "The group posted the victim on its leak site and released sample records.", "A ransomware group listed the network on its leak site and published sample patient records.", actors = listOf("LockBit"), malware = listOf("LockBit 3.0"), tools = listOf("Cobalt Strike"), techniques = listOf("T1486"), cves = listOf("CVE-2026-12345"), vendors = listOf("Fortinet"), products = listOf("FortiOS"), sectors = listOf("Healthcare"), countries = listOf("India"), isRansomware = true),
            Template("Incidents and Breaches", "Major bank suffers data exfiltration through compromised API", "Attackers exploited an unauthenticated API endpoint to steal customer records.", "A major bank disclosed a data breach affecting millions of customers.", actors = listOf("APT28"), tools = listOf("Mimikatz"), techniques = listOf("T1078"), sectors = listOf("Finance"), countries = listOf("United States")),
            Template("Incidents and Breaches", "Telecom provider hit by espionage-linked intrusion", "Nation-state actors maintained persistent access for over six months.", "A telecom firm discovered long-running espionage in its network infrastructure.", actors = listOf("APT29"), techniques = listOf("T1059"), sectors = listOf("Technology"), countries = listOf("United Kingdom")),
            Template("Incidents and Breaches", "Manufacturing firm halted by wiper malware attack", "A wiper malware destroyed production systems across three factories.", "Manufacturing operations halted after destructive malware deployment.", sectors = listOf("Manufacturing"), countries = listOf("Germany")),
            Template("Incidents and Breaches", "Education ministry portal breached exposing student data", "Student records including grades and personal details were dumped online.", "A government education portal was breached exposing millions of student records.", sectors = listOf("Education", "Government"), countries = listOf("India")),
            Template("Incidents and Breaches", "Retail chain POS systems compromised in supply chain attack", "Payment card data was skimmed through malicious firmware updates.", "A retail chain's point-of-sale systems were compromised via a supply chain attack.", sectors = listOf("Retail"), countries = listOf("United States")),
            Template("Incidents and Breaches", "Cloud hosting provider reports unauthorized access to client data", "An attacker gained access to the management plane of multiple tenants.", "A cloud hosting provider disclosed unauthorized access to customer environments.", sectors = listOf("Technology"), countries = listOf("Japan")),
            Template("Incidents and Breaches", "Insurance company leaked policy holder data via misconfigured bucket", "An S3 bucket left public exposed 2 million policy records.", "An insurance company exposed policyholder data through cloud misconfiguration.", sectors = listOf("Finance"), countries = listOf("Australia")),
            Template("Incidents and Breaches", "Hospital chain pays ransom after patient data encrypted", "The healthcare group paid an undisclosed ransom to restore operations.", "A hospital chain paid ransom after ransomware encrypted critical patient systems.", actors = listOf("BlackCat"), malware = listOf("BlackCat/ALPHV"), sectors = listOf("Healthcare"), countries = listOf("United States"), isRansomware = true),
            Template("Incidents and Breaches", "Airline booking system breached exposing passport details", "Passport numbers and travel itineraries were exposed in a breach of the reservation system.", "An airline's booking system was breached, exposing passenger passport data.", sectors = listOf("Technology"), countries = listOf("France")),

            // Ransomware Activity (10)
            Template("Ransomware Activity", "LockBit affiliate targets energy sector with new variant", "A new LockBit variant includes faster encryption and anti-forensics capabilities.", "LockBit deploys new variant against energy companies.", actors = listOf("LockBit"), malware = listOf("LockBit 3.0"), sectors = listOf("Energy"), countries = listOf("United States"), isRansomware = true),
            Template("Ransomware Activity", "Cl0p exploits zero-day in file transfer appliance", "Cl0p leveraged a previously unknown vulnerability to mass-exfiltrate data.", "Cl0p ransomware group exploited a zero-day in a popular file transfer solution.", actors = listOf("Cl0p"), malware = listOf("Cl0p"), cves = listOf("CVE-2026-54321"), sectors = listOf("Finance", "Healthcare"), countries = listOf("Canada"), isRansomware = true),
            Template("Ransomware Activity", "Play ransomware targets Latin American government agencies", "Multiple government agencies in Brazil were hit simultaneously.", "Play ransomware simultaneously targeted several government agencies.", actors = listOf("Play"), malware = listOf("Play"), sectors = listOf("Government"), countries = listOf("Brazil"), isRansomware = true),
            Template("Ransomware Activity", "Akira ransomware adds Linux ESXi encryptor", "Akira expanded its capabilities with a dedicated Linux/ESXi variant.", "Akira ransomware now encrypts VMware ESXi virtual machines on Linux.", malware = listOf("Akira"), vendors = listOf("VMware"), products = listOf("vCenter"), isRansomware = true),
            Template("Ransomware Activity", "Royal ransomware rebrands operations under new name", "The group behind Royal ransomware rebranded but kept the same TTPs.", "Royal ransomware operators rebrand while maintaining same attack patterns.", malware = listOf("Royal"), isRansomware = true),
            Template("Ransomware Activity", "Healthcare ransomware attacks surge 45% in Q3", "Healthcare organizations saw a dramatic increase in ransomware incidents.", "Ransomware attacks against healthcare rose 45% compared to previous quarter.", sectors = listOf("Healthcare"), isRansomware = true),
            Template("Ransomware Activity", "New ransomware-as-a-service platform emerges on dark web", "A new RaaS platform offers 80/20 profit split to affiliates.", "A new ransomware-as-a-service operation launched with competitive affiliate terms.", isRansomware = true),
            Template("Ransomware Activity", "Double extortion attack exposes pharmaceutical research data", "Research data for three drug candidates was published on a leak site.", "Pharma company faces double extortion with stolen R&D data published online.", sectors = listOf("Healthcare"), countries = listOf("India"), isRansomware = true),
            Template("Ransomware Activity", "City government systems encrypted in coordinated attack", "Municipal services including water and power billing were disrupted.", "A coordinated ransomware attack encrypted city government systems.", sectors = listOf("Government"), countries = listOf("United States"), isRansomware = true),
            Template("Ransomware Activity", "Ransomware operator arrested in joint international operation", "Law enforcement from five countries cooperated in the arrest.", "A key ransomware operator was arrested in a multinational law enforcement action.", isRansomware = true),

            // General Threat Activity (10)
            Template("General Threat Activity", "New botnet targets IoT devices with default credentials", "A botnet named ZephyrBot is brute-forcing IoT devices globally.", "A new botnet is rapidly spreading by targeting IoT devices with default passwords.", techniques = listOf("T1078")),
            Template("General Threat Activity", "Phishing campaign uses AI-generated voice in vishing attacks", "Attackers use deepfake audio to impersonate executives in phone calls.", "AI-generated voice phishing attacks targeting corporate executives on the rise.", techniques = listOf("T1566")),
            Template("General Threat Activity", "Cryptojacking campaign targets Kubernetes clusters", "Misconfigured Kubernetes clusters are being exploited to mine cryptocurrency.", "A large-scale cryptojacking campaign targets exposed Kubernetes environments.", sectors = listOf("Technology")),
            Template("General Threat Activity", "Infostealer malware distributed through fake software updates", "A popular infostealer is being delivered via counterfeit browser update pages.", "Infostealer malware spreads through convincing fake software update notifications.", techniques = listOf("T1059")),
            Template("General Threat Activity", "DDoS attacks against financial institutions increase globally", "Volumetric DDoS attacks against banks reached record levels.", "Financial institutions worldwide face unprecedented DDoS attack volumes.", sectors = listOf("Finance")),
            Template("General Threat Activity", "Threat actors exploit misconfigured cloud storage at scale", "Automated scanners are finding and exfiltrating data from open cloud buckets.", "Mass exploitation of misconfigured cloud storage continues to accelerate."),
            Template("General Threat Activity", "New malware loader uses encrypted DNS for C2 communication", "A sophisticated loader uses DNS-over-HTTPS to evade network detection.", "A new malware loader leverages encrypted DNS to hide command and control traffic."),
            Template("General Threat Activity", "Supply chain compromise found in popular npm packages", "Three widely-used npm packages were found to contain backdoor code.", "Popular npm packages compromised with backdoor affecting thousands of projects.", sectors = listOf("Technology")),
            Template("General Threat Activity", "Credential stuffing attacks target streaming platforms", "Stolen credential databases are being used against entertainment services.", "Streaming platforms hit by massive credential stuffing campaigns.", sectors = listOf("Technology"), countries = listOf("United States")),
            Template("General Threat Activity", "Mobile banking trojans evolve with screen overlay techniques", "New variants can overlay fake login screens on top of banking apps.", "Mobile banking trojans add sophisticated screen overlay capabilities.", sectors = listOf("Finance")),

            // Notable Vulnerabilities (10)
            Template("Notable Vulnerabilities", "Critical RCE in widely-used Java framework disclosed", "A critical remote code execution flaw allows unauthenticated access.", "Critical RCE vulnerability disclosed in popular Java framework.", cves = listOf("CVE-2026-11111"), sectors = listOf("Technology")),
            Template("Notable Vulnerabilities", "Cisco patches actively exploited IOS XE vulnerability", "Cisco released an emergency patch for an actively exploited flaw.", "Cisco issues emergency patch for IOS XE vulnerability under active exploitation.", cves = listOf("CVE-2026-22222"), vendors = listOf("Cisco")),
            Template("Notable Vulnerabilities", "Microsoft Patch Tuesday fixes 97 vulnerabilities including 2 zero-days", "Two zero-days in Windows kernel were actively exploited before patching.", "Microsoft's monthly patch addresses 97 flaws including two zero-day exploits.", cves = listOf("CVE-2026-33333", "CVE-2026-33334"), vendors = listOf("Microsoft"), products = listOf("Windows")),
            Template("Notable Vulnerabilities", "Critical flaw in Palo Alto firewall allows authentication bypass", "An authentication bypass in PAN-OS could allow remote attackers full access.", "Palo Alto Networks patches critical auth bypass in PAN-OS firewall.", cves = listOf("CVE-2026-44444"), vendors = listOf("Palo Alto Networks"), products = listOf("PAN-OS")),
            Template("Notable Vulnerabilities", "Ivanti Connect Secure zero-day exploited in the wild", "A zero-day in Ivanti's VPN product is being actively exploited by APT groups.", "Ivanti VPN zero-day under active exploitation by nation-state actors.", cves = listOf("CVE-2026-55555"), vendors = listOf("Ivanti"), actors = listOf("Volt Typhoon")),
            Template("Notable Vulnerabilities", "Chrome emergency update patches high-severity V8 bug", "Google released an out-of-band update for a V8 engine vulnerability.", "Google issues emergency Chrome update for actively exploited V8 vulnerability.", vendors = listOf("Google"), products = listOf("Chrome")),
            Template("Notable Vulnerabilities", "VMware vCenter Server vulnerability allows privilege escalation", "A flaw in vCenter lets authenticated users escalate to admin privileges.", "VMware patches privilege escalation vulnerability in vCenter Server.", cves = listOf("CVE-2026-66666"), vendors = listOf("VMware"), products = listOf("vCenter")),
            Template("Notable Vulnerabilities", "Linux kernel vulnerability affects enterprise distributions", "A use-after-free in the kernel's netfilter subsystem enables local root.", "Linux kernel vulnerability in netfilter enables local privilege escalation."),
            Template("Notable Vulnerabilities", "WordPress plugin vulnerability exposes millions of sites", "A SQL injection in a popular plugin allows unauthenticated data extraction.", "Critical WordPress plugin vulnerability puts millions of websites at risk."),
            Template("Notable Vulnerabilities", "Exchange Server zero-day chain discovered by security researchers", "Two chained vulnerabilities enable remote code execution on Exchange.", "Microsoft Exchange Server faces new zero-day chain enabling remote code execution.", cves = listOf("CVE-2026-77777"), vendors = listOf("Microsoft"), products = listOf("Exchange Server")),

            // Geopolitical / Nation-State Watch (10)
            Template("Geopolitical / Nation-State Watch", "APT28 targets European defense contractors with new backdoor", "APT28 deployed a new modular backdoor in campaigns against defense firms.", "Russian-linked APT28 targets European defense sector with sophisticated backdoor.", actors = listOf("APT28"), tools = listOf("Cobalt Strike"), sectors = listOf("Government"), countries = listOf("Germany", "France")),
            Template("Geopolitical / Nation-State Watch", "Lazarus Group launders crypto through new mixer service", "North Korean hackers used a novel mixing service to launder stolen crypto.", "Lazarus Group launders hundreds of millions through new cryptocurrency mixer.", actors = listOf("Lazarus Group"), sectors = listOf("Finance")),
            Template("Geopolitical / Nation-State Watch", "Chinese APT conducts espionage on Southeast Asian telecom", "Volt Typhoon maintained stealthy access to telecom infrastructure.", "Chinese state-sponsored group conducts prolonged espionage on telecom networks.", actors = listOf("Volt Typhoon"), sectors = listOf("Technology"), countries = listOf("Indonesia")),
            Template("Geopolitical / Nation-State Watch", "Kimsuky targets South Korean research institutes", "North Korean hackers phished researchers with fake conference invitations.", "Kimsuky targets South Korean academic and research institutions.", actors = listOf("Kimsuky"), techniques = listOf("T1566"), sectors = listOf("Education")),
            Template("Geopolitical / Nation-State Watch", "Iran-linked group targets water infrastructure in Israel", "Attacks on water treatment SCADA systems attributed to Iranian hackers.", "Iranian cyber group targets Israeli water infrastructure control systems.", sectors = listOf("Energy", "Government"), countries = listOf("India")),
            Template("Geopolitical / Nation-State Watch", "Russian disinformation campaign targets EU elections", "A coordinated influence operation used fake news sites and social media bots.", "Russian-linked disinformation operation attempts to influence EU elections.", countries = listOf("France", "Germany")),
            Template("Geopolitical / Nation-State Watch", "Chinese cyber espionage campaign targets Australian government", "Advanced persistent threats targeted multiple Australian federal agencies.", "Chinese APT group conducts cyber espionage against Australian government systems.", actors = listOf("APT29"), sectors = listOf("Government"), countries = listOf("Australia")),
            Template("Geopolitical / Nation-State Watch", "North Korean IT workers infiltrate Western tech companies", "DPRK operatives used fake identities to get hired at tech companies.", "North Korean IT workers found working at Western technology companies under false identities.", countries = listOf("United States", "United Kingdom")),
            Template("Geopolitical / Nation-State Watch", "State-sponsored group targets satellite communication systems", "Critical satellite communication infrastructure was targeted by advanced threat actors.", "Nation-state actors target satellite communications for strategic intelligence.", sectors = listOf("Technology")),
            Template("Geopolitical / Nation-State Watch", "Cyber operations escalate amid regional territorial disputes", "Multiple state actors increased cyber operations during geopolitical tensions.", "Cyber operations intensify as geopolitical tensions rise in multiple regions.", countries = listOf("Japan", "India")),

            // Law Enforcement & Takedowns (8)
            Template("Law Enforcement & Takedowns", "FBI disrupts major botnet infrastructure in global operation", "A multinational effort took down a botnet controlling 500,000 devices.", "FBI-led operation dismantles major botnet infrastructure used for DDoS attacks."),
            Template("Law Enforcement & Takedowns", "Europol arrests ransomware affiliate network members", "Six suspects were arrested across four European countries.", "Europol arrests six suspects linked to major ransomware affiliate network.", isRansomware = true),
            Template("Law Enforcement & Takedowns", "Dark web marketplace seized by law enforcement coalition", "The marketplace facilitated sale of stolen credentials and malware.", "International law enforcement coalition seizes major dark web marketplace."),
            Template("Law Enforcement & Takedowns", "Interpol operation disrupts business email compromise ring", "A BEC ring responsible for $120M in losses was dismantled.", "Interpol disrupts business email compromise ring behind $120M in global losses."),
            Template("Law Enforcement & Takedowns", "US sanctions cryptocurrency exchange for ransomware laundering", "The Treasury Department sanctioned an exchange facilitating ransom payments.", "US Treasury sanctions crypto exchange used to launder ransomware proceeds.", isRansomware = true),
            Template("Law Enforcement & Takedowns", "Australian police arrest suspect in critical infrastructure attack", "A suspect was charged with unauthorized access to power grid systems.", "Australian Federal Police arrest suspect behind critical infrastructure cyberattack.", countries = listOf("Australia"), sectors = listOf("Energy")),
            Template("Law Enforcement & Takedowns", "International operation seizes phishing-as-a-service platform", "A platform used to create thousands of phishing kits was taken down.", "Law enforcement seizes phishing-as-a-service platform serving thousands of criminals."),
            Template("Law Enforcement & Takedowns", "UK NCA infiltrates and disrupts cybercrime forum", "The UK's National Crime Agency conducted a long-running infiltration operation.", "UK NCA successfully infiltrates major cybercrime forum leading to multiple arrests.", countries = listOf("United Kingdom")),

            // Third-Party / Supply Chain Risk (7)
            Template("Third-Party / Supply Chain Risk", "Major SaaS provider breach affects downstream customers", "A breach at a SaaS provider exposed data from hundreds of client organizations.", "SaaS provider breach cascades to affect hundreds of downstream customer organizations.", sectors = listOf("Technology")),
            Template("Third-Party / Supply Chain Risk", "Compromised code signing certificate used to distribute malware", "Attackers stole a valid code signing certificate to sign their malware.", "Stolen code signing certificate used to distribute trojanized software updates."),
            Template("Third-Party / Supply Chain Risk", "Open source library backdoor discovered after months", "A backdoor in a widely-used library went undetected for four months.", "Backdoor discovered in popular open source library after months of undetected operation.", sectors = listOf("Technology")),
            Template("Third-Party / Supply Chain Risk", "Managed service provider compromised to attack customers", "An MSP was breached, giving attackers access to dozens of client networks.", "Managed service provider compromised, enabling attacks on dozens of client organizations."),
            Template("Third-Party / Supply Chain Risk", "Hardware supply chain interdiction discovered in network equipment", "Modified firmware was found pre-installed on networking equipment.", "Network equipment found with pre-installed malicious firmware in supply chain attack."),
            Template("Third-Party / Supply Chain Risk", "CI/CD pipeline poisoning targets multiple software vendors", "Attackers injected malicious code into automated build pipelines.", "CI/CD pipeline attacks target software vendors to poison downstream applications.", sectors = listOf("Technology")),
            Template("Third-Party / Supply Chain Risk", "Third-party payment processor breach exposes transaction data", "A payment processor breach exposed card data from multiple merchant clients.", "Payment processor breach exposes transaction data across multiple retail clients.", sectors = listOf("Finance", "Retail"), countries = listOf("United States"))
        )

        templates.forEachIndexed { index, t ->
            val publishedTs = baseTime.plusSeconds((index * 600).toLong()).toString()
            val key = "source_campaign_shadow:srcdoc_rss_feed_%04d".format(index + 1)
            stories.add(
                NewsStory(
                    activityKey = key,
                    title = t.title,
                    summary = t.summary,
                    source = "rss_feed",
                    sourceUrl = "https://example.com/article/${index + 1}",
                    tlp = "Clear",
                    isRansomware = t.isRansomware,
                    actorNames = t.actors,
                    sectionLabel = t.section,
                    malware = t.malware,
                    tools = t.tools,
                    techniques = t.techniques,
                    cves = t.cves,
                    vendors = t.vendors,
                    products = t.products,
                    targetSectors = t.sectors,
                    targetCountries = t.countries,
                    publishedTs = publishedTs,
                    icounterObservation = "Automated analysis identified key indicators in this report.",
                    atAGlance = t.atAGlance,
                    howTheAttackWorks = "The attack leveraged a combination of social engineering and technical exploitation. ${t.summary}",
                    readTimeMinutes = (2..8).random()
                )
            )
        }

        return stories
    }
}
