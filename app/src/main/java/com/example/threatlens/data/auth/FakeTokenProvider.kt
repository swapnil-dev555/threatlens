package com.example.threatlens.data.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fake token provider: tokens expire every 3 minutes, refreshing takes ~1.2s.
 */
@Singleton
class FakeTokenProvider @Inject constructor() : TokenProvider {

    private val mutex = Mutex()
    private var token: String? = null
    private var expiresAtMillis: Long = 0L
    private val _currentToken = MutableStateFlow<String?>(null)
    override val currentToken: StateFlow<String?> = _currentToken.asStateFlow()

    companion object {
        private const val TOKEN_LIFETIME_MS = com.example.threatlens.common.AppConstants.TOKEN_LIFETIME_MS
        private const val REFRESH_DELAY_MS = com.example.threatlens.common.AppConstants.REFRESH_DELAY_MS
    }

    override suspend fun getAccessToken(): String = mutex.withLock {
        val existing = token
        if (existing != null && System.currentTimeMillis() < expiresAtMillis) return existing
        return refreshTokenInternal()
    }

    override suspend fun refreshToken(): String = mutex.withLock { refreshTokenInternal() }

    private suspend fun refreshTokenInternal(): String {
        delay(REFRESH_DELAY_MS.milliseconds)
        val newToken = "fake_token_${UUID.randomUUID()}"
        token = newToken
        expiresAtMillis = System.currentTimeMillis() + TOKEN_LIFETIME_MS
        _currentToken.value = newToken
        return newToken
    }
}
