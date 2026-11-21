package com.example.threatlens

import com.example.threatlens.data.debug.DebugSettings
import com.example.threatlens.data.fake.FakeBackendInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBackendInterceptorTest {

    private lateinit var debugSettings: DebugSettings
    private lateinit var interceptor: FakeBackendInterceptor
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        debugSettings = DebugSettings().apply {
            setLatency(0)
        }
        interceptor = FakeBackendInterceptor(debugSettings)
        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @Test
    fun `getSections returns 200 with non-null request`() {
        val request = Request.Builder()
            .url("https://example.com/api/v1/cyber-news/sections")
            .addHeader("Authorization", "Bearer fake_token")
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertNotNull(response.request)
        assertEquals("https://example.com/api/v1/cyber-news/sections", response.request.url.toString())
    }

    @Test
    fun `deleteSubscription returns 204 with non-null request`() {
        val request = Request.Builder()
            .url("https://example.com/api/v1/news-subscriptions/${FakeBackendInterceptor.SEEDED_SUB_ID}")
            .delete()
            .addHeader("Authorization", "Bearer fake_token")
            .build()

        val response = client.newCall(request).execute()

        assertEquals(204, response.code)
        assertNotNull(response.request)
    }

    @Test
    fun `force500 returns 500 response`() {
        debugSettings.setForce500(true)
        val request = Request.Builder()
            .url("https://example.com/api/v1/cyber-news")
            .addHeader("Authorization", "Bearer fake_token")
            .build()

        val response = client.newCall(request).execute()

        assertEquals(500, response.code)
        assertNotNull(response.request)
    }

    @Test
    fun `patchSubscription updates name and cadenceTime successfully`() {
        val patchJson = """{"name":"Updated Name","cadence_time":"11:30:00"}"""
        val request = Request.Builder()
            .url("https://example.com/api/v1/news-subscriptions/${FakeBackendInterceptor.SEEDED_SUB_ID}")
            .patch(patchJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer fake_token")
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body.string()
        assertTrue(body.contains("Updated Name"))
        assertTrue(body.contains("11:30:00"))
    }

    @Test
    fun `getSubscription returns single subscription 200`() {
        val request = Request.Builder()
            .url("https://example.com/api/v1/news-subscriptions/${FakeBackendInterceptor.SEEDED_SUB_ID}")
            .get()
            .addHeader("Authorization", "Bearer fake_token")
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body.string()
        assertTrue(body.contains(FakeBackendInterceptor.SEEDED_SUB_ID))
    }
}
