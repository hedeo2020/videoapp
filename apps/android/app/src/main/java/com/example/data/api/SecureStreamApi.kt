package com.example.data.api

import android.content.Context
import com.example.config.AppConfig
import com.example.data.storage.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PATCH
import retrofit2.http.Path

interface SecureStreamApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: TokenRefreshRequest
    ): TokenRefreshResponse

    @GET("catalog")
    suspend fun getCatalog(): CatalogResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String
    ): List<MovieCardDto>

    @POST("playback/sessions")
    suspend fun createPlaybackSession(
        @Body request: PlaybackSessionRequest
    ): PlaybackSessionResponse

    @POST("playback/progress")
    suspend fun sendPlaybackProgress(
        @Body request: ProgressRequest
    ): Any

    @POST("offline/downloads")
    suspend fun getOfflineDownloadUrl(
        @Body request: OfflineDownloadRequest
    ): OfflineDownloadResponse

    @PATCH("account/me")
    suspend fun updateAccount(
        @Body request: UpdateAccountRequest
    ): UserDto

    @GET("messages")
    suspend fun getMessages(): MessagesResponse

    @POST("messages")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): MessageDto

    @GET("notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Path("id") id: String
    ): Any

    @PATCH("notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Any

    @GET("account/dashboard")
    suspend fun getDashboardSummary(): DashboardSummaryDto

    @GET("admin/system-status")
    suspend fun getAdminSystemStatus(): AdminSystemStatusDto

    @GET("admin/users")
    suspend fun getAdminUsers(): List<AdminUserDto>

    @GET("admin/conversations")
    suspend fun getAdminConversations(): List<AdminConversationDto>

    @GET("admin/device-sessions")
    suspend fun getAdminDeviceSessions(): List<AdminDeviceSessionDto>


    companion object {
        private const val BASE_URL = AppConfig.API_BASE_URL

        fun create(context: Context, tokenManager: TokenManager): SecureStreamApi {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val token = tokenManager.getAccessToken()

                    val requestBuilder = original.newBuilder()
                    if (token != null) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }

                    val request = requestBuilder.build()
                    val response = chain.proceed(request)

                    // If unauthorized, attempt to refresh once synchronously
                    if (response.code == 401 && tokenManager.getRefreshToken() != null) {
                        response.close() // Close the original response

                        // Build a temporary Api client just for refreshing to avoid recursive intercepts
                        val refreshApi = Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(MoshiConverterFactory.create(moshi))
                            .build()
                            .create(SecureStreamApi::class.java)

                        try {
                            val refreshResponse = kotlinx.coroutines.runBlocking {
                                refreshApi.refresh(
                                    TokenRefreshRequest(
                                        refreshToken = tokenManager.getRefreshToken()!!,
                                        deviceId = tokenManager.getDeviceId()
                                    )
                                )
                            }
                            // Save new tokens and user info
                            tokenManager.setAccessToken(refreshResponse.accessToken)
                            tokenManager.setRefreshToken(refreshResponse.refreshToken)
                            tokenManager.setUserEmail(refreshResponse.user.email)
                            tokenManager.setUserName(refreshResponse.user.displayName)
                            tokenManager.setUserId(refreshResponse.user.id)
                            tokenManager.setUserRole(refreshResponse.user.role)

                            // Retry original request with the new access token
                            val newRequest = original.newBuilder()
                                .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                                .build()
                            chain.proceed(newRequest)
                        } catch (e: Exception) {
                            // If refresh fails, clear tokens to redirect to login
                            tokenManager.clear()
                            chain.proceed(request) // will return original 401 or similar
                        }
                    } else {
                        response
                    }
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SecureStreamApi::class.java)
        }
    }
}
