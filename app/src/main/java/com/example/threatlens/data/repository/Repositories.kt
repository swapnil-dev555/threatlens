package com.example.threatlens.data.repository

import com.example.threatlens.data.api.CyberNewsApi
import com.example.threatlens.data.model.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val api: CyberNewsApi
) {
    suspend fun getSections(sort: String? = null): Result<SectionsResponse> = apiCall {
        api.getSections(sort)
    }

    suspend fun getFilterOptions(field: String, query: String? = null): Result<FilterOptionsResponse> = apiCall {
        api.getFilterOptions(field = field, query = query)
    }

    suspend fun getNews(
        limit: Int = 20,
        offset: Int = 0,
        sectionKey: String? = null,
        query: String? = null
    ): Result<PaginatedResponse<NewsCard>> = apiCall {
        api.getNews(limit = limit, offset = offset, sectionKey = sectionKey, query = query)
    }

    suspend fun getStory(activityKey: String): Result<NewsStory> = apiCall {
        api.getStory(activityKey)
    }
}

@Singleton
class SubscriptionRepository @Inject constructor(
    private val api: CyberNewsApi
) {
    suspend fun getSubscriptions(
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<Subscription>> = apiCall {
        api.getSubscriptions(limit = limit, offset = offset)
    }

    suspend fun createSubscription(request: CreateSubscriptionRequest): Result<Subscription> = apiCall {
        val response = api.createSubscription(request)
        response.body() ?: throw Exception("Empty response body")
    }

    suspend fun patchSubscription(id: String, request: PatchSubscriptionRequest): Result<Subscription> = apiCall {
        api.patchSubscription(id, request)
    }

    suspend fun deleteSubscription(id: String): Result<Unit> = apiCall {
        val response = api.deleteSubscription(id)
        if (!response.isSuccessful) throw Exception("Delete failed: ${response.code()}")
    }

    suspend fun getSubscriptionMatches(
        id: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<PaginatedResponse<NewsCard>> = apiCall {
        api.getSubscriptionMatches(id, limit, offset)
    }

    suspend fun getSubscriptionMatchStory(
        subscriptionId: String,
        activityKey: String
    ): Result<NewsStory> = apiCall {
        api.getSubscriptionMatchStory(subscriptionId, activityKey)
    }

    suspend fun getWindowMatches(
        subscriptionId: String,
        windowId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<WindowMatchesResponse> = apiCall {
        api.getWindowMatches(subscriptionId, windowId, limit, offset)
    }
}

@Singleton
class DeviceRepository @Inject constructor(
    private val api: CyberNewsApi
) {
    suspend fun registerDevice(request: RegisterDeviceRequest): Result<DeviceResponse> = apiCall {
        api.registerDevice(request)
    }

    suspend fun revokeDevice(deviceId: String): Result<RevokeResponse> = apiCall {
        api.revokeDevice(deviceId)
    }
}

/**
 * Wraps an API call with error handling and Crashlytics non-fatal logging.
 */
private suspend inline fun <T> apiCall(crossinline block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("api_error_type", e.javaClass.simpleName)
            setCustomKey("api_error_message", e.message ?: "unknown")
            recordException(e)
        }
        Result.failure(e)
    }
}
