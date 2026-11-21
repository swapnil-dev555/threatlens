package com.example.threatlens.data.auth

import kotlinx.coroutines.flow.StateFlow

interface TokenProvider {
    suspend fun getAccessToken(): String
    suspend fun refreshToken(): String
    val currentToken: StateFlow<String?>
}
