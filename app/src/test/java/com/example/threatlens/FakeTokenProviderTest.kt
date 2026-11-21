package com.example.threatlens

import com.example.threatlens.data.auth.FakeTokenProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class FakeTokenProviderTest {

    @Test
    fun `getAccessToken returns a token`() = runTest {
        val provider = FakeTokenProvider()
        val token = provider.getAccessToken()
        assertNotNull(token)
        assert(token.startsWith("fake_token_"))
    }

    @Test
    fun `refreshToken returns new token`() = runTest {
        val provider = FakeTokenProvider()
        val token1 = provider.getAccessToken()
        val token2 = provider.refreshToken()
        assertNotEquals(token1, token2)
    }

    @Test
    fun `concurrent getAccessToken calls return same token without multiple refreshes`() = runTest {
        val provider = FakeTokenProvider()
        // Warm up with one call first
        val initial = provider.getAccessToken()

        // Concurrent calls should all return the same cached token
        val tokens = (1..5).map {
            async { provider.getAccessToken() }
        }.awaitAll()

        // All should be equal (the cached token) since it hasn't expired
        assert(tokens.all { it == initial })
    }
}
