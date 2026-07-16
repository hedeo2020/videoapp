package com.example.data.api

import android.content.Context
import com.example.config.AppConfig
import com.example.data.storage.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
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

    @PATCH("admin/users/{id}/status")
    suspend fun updateAdminUserStatus(
        @Path("id") id: String,
        @Body request: AdminUserStatusRequest
    ): UserDto

    @DELETE("admin/users/{id}")
    suspend fun deleteAdminUser(
        @Path("id") id: String
    ): Any

    @DELETE("admin/device-sessions/{id}")
    suspend fun revokeAdminDeviceSession(
        @Path("id") id: String
    ): Any

    @POST("admin/conversations/{id}/messages")
    suspend fun sendAdminConversationMessage(
        @Path("id") id: String,
        @Body request: AdminMessageRequest
    ): MessageDto

    @POST("admin/notifications")
    suspend fun sendAdminNotification(
        @Body request: AdminNotificationRequest
    ): Map<String, @JvmSuppressWildcards Any?>

    @POST("admin/backups")
    suspend fun createAdminBackup(): Map<String, @JvmSuppressWildcards Any?>

    @POST("admin/backups/run-scheduled-now")
    suspend fun runAdminScheduledBackupNow(): Map<String, @JvmSuppressWildcards Any?>

    @POST("admin/alerts/test")
    suspend fun testAdminAlert(): Map<String, @JvmSuppressWildcards Any?>

    @PATCH("admin/settings")
    suspend fun updateAdminSettings(
        @Body request: AdminSettingsUpdateRequest
    ): Map<String, @JvmSuppressWildcards Any?>

    @GET("admin/movies")
    suspend fun getAdminMoviesRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/series")
    suspend fun getAdminSeriesRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/collections")
    suspend fun getAdminCollectionsRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/files")
    suspend fun getAdminFilesRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/storage-breakdown")
    suspend fun getAdminStorageBreakdownRaw(): Map<String, @JvmSuppressWildcards Any?>

    @GET("admin/processing/jobs")
    suspend fun getAdminProcessingRaw(): Map<String, @JvmSuppressWildcards Any?>

    @GET("admin/notifications")
    suspend fun getAdminNotificationsRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/api-tokens")
    suspend fun getAdminApiTokensRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/playback-sessions")
    suspend fun getAdminPlaybackRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/backups")
    suspend fun getAdminBackupsRaw(): Map<String, @JvmSuppressWildcards Any?>

    @GET("admin/activity")
    suspend fun getAdminActivityRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/trash")
    suspend fun getAdminTrashRaw(): Map<String, @JvmSuppressWildcards Any?>

    @GET("admin/audit-logs")
    suspend fun getAdminAuditLogsRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/security-events")
    suspend fun getAdminSecurityEventsRaw(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("admin/settings")
    suspend fun getAdminSettingsRaw(): Map<String, @JvmSuppressWildcards Any?>

    @Multipart
    @POST("admin/uploads/direct")
    suspend fun uploadAdminVideoDirect(
        @Header("x-upload-id") uploadId: String,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("synopsis") synopsis: RequestBody,
        @Part("maturityRating") maturityRating: RequestBody
    ): ResponseBody

    @GET("admin/uploads/{id}/progress")
    suspend fun getAdminUploadProgress(
        @Path("id") id: String
    ): Map<String, @JvmSuppressWildcards Any?>

    @POST("admin/editor/jobs/trim")
    suspend fun createAdminTrimJob(
        @Body request: AdminTrimJobRequest
    ): ResponseBody

    @GET("admin/editor/jobs")
    suspend fun getAdminEditorJobsRaw(): List<Map<String, @JvmSuppressWildcards Any?>>


    companion object {
        private val BASE_URL = AppConfig.API_BASE_URL

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
