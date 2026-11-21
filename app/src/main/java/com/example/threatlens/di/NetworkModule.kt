package com.example.threatlens.di

import com.example.threatlens.BuildConfig
import com.example.threatlens.data.api.CyberNewsApi
import com.example.threatlens.data.auth.AuthInterceptor
import com.example.threatlens.data.auth.FakeTokenProvider
import com.example.threatlens.data.auth.TokenProvider
import com.example.threatlens.data.debug.DebugSettings
import com.example.threatlens.data.fake.FakeBackendInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = FakeTokenProvider()

    @Provides
    @Singleton
    fun provideFakeBackendInterceptor(
        debugSettings: DebugSettings,
        notificationHelper: com.example.threatlens.data.fcm.NotificationHelper
    ): FakeBackendInterceptor = FakeBackendInterceptor(debugSettings, notificationHelper)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        fakeBackendInterceptor: FakeBackendInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(com.example.threatlens.common.AppConstants.DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(com.example.threatlens.common.AppConstants.DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(com.example.threatlens.common.AppConstants.DEFAULT_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Add auth interceptor first (adds Authorization header)
        builder.addInterceptor(authInterceptor)

        // In debug builds, intercept at the network level with the fake backend
        if (BuildConfig.USE_FAKE_BACKEND) {
            builder.addInterceptor(fakeBackendInterceptor)
        }

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideCyberNewsApi(retrofit: Retrofit): CyberNewsApi {
        return retrofit.create(CyberNewsApi::class.java)
    }
}
