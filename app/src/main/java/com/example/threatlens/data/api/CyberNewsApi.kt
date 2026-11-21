package com.example.threatlens.data.api

import com.example.threatlens.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface CyberNewsApi {
    @GET("cyber-news/sections")
    suspend fun getSections(@Query("sort") sort: String? = null): SectionsResponse

    @GET("cyber-news/filter-options")
    suspend fun getFilterOptions(
        @Query("field") field: String,
        @Query("sort") sort: String? = null,
        @Query("q") query: String? = null
    ): FilterOptionsResponse

    @GET("cyber-news")
    suspend fun getNews(
        @Query("is_expand") isExpand: Boolean = false,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("section_key") sectionKey: String? = null,
        @Query("q") query: String? = null
    ): PaginatedResponse<NewsCard>

    @GET("cyber-news/{activity_key}")
    suspend fun getStory(
        @Path("activity_key") activityKey: String,
        @Query("is_expand") isExpand: Boolean = true
    ): NewsStory

    @POST("notification-devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): DeviceResponse

    @DELETE("notification-devices/{device_id}")
    suspend fun revokeDevice(@Path("device_id") deviceId: String): RevokeResponse

    @GET("news-subscriptions")
    suspend fun getSubscriptions(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): PaginatedResponse<Subscription>

    @POST("news-subscriptions")
    suspend fun createSubscription(@Body request: CreateSubscriptionRequest): Response<Subscription>

    @PATCH("news-subscriptions/{id}")
    suspend fun patchSubscription(
        @Path("id") id: String,
        @Body request: PatchSubscriptionRequest
    ): Subscription

    @DELETE("news-subscriptions/{id}")
    suspend fun deleteSubscription(@Path("id") id: String): Response<Unit>

    @GET("news-subscriptions/{id}/matches")
    suspend fun getSubscriptionMatches(
        @Path("id") id: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): PaginatedResponse<NewsCard>

    @GET("news-subscriptions/{id}/matches/{activity_key}")
    suspend fun getSubscriptionMatchStory(
        @Path("id") subscriptionId: String,
        @Path("activity_key") activityKey: String,
        @Query("is_expand") isExpand: Boolean = true
    ): NewsStory

    @GET("news-subscriptions/{id}/windows/{window_id}/matches")
    suspend fun getWindowMatches(
        @Path("id") subscriptionId: String,
        @Path("window_id") windowId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): WindowMatchesResponse
}
