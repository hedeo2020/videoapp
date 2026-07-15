package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val status: String,
    val emailVerified: Boolean
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
    val user: UserDto
)

@JsonClass(generateAdapter = true)
data class TokenRefreshRequest(
    val refreshToken: String,
    val deviceId: String
)

@JsonClass(generateAdapter = true)
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
    val user: UserDto
)

@JsonClass(generateAdapter = true)
data class MovieCardDto(
    val id: String,
    val kind: String, // "MOVIE" or "SERIES"
    val title: String,
    val slug: String,
    val synopsis: String,
    val maturityRating: String?,
    val artworkUrl: String?,
    val durationSeconds: Int?,
    val durationText: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val heroImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RailDto(
    val id: String,
    val name: String,
    val slug: String,
    val items: List<MovieCardDto>
)

@JsonClass(generateAdapter = true)
data class CatalogResponse(
    val rails: List<RailDto>
)

@JsonClass(generateAdapter = true)
data class PlaybackSessionRequest(
    val videoId: String,
    val deviceId: String,
    val riskSignals: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WatermarkDto(
    val text: String,
    val opacity: Float,
    val moveEverySeconds: Int
)

@JsonClass(generateAdapter = true)
data class PlaybackSessionResponse(
    val sessionId: String,
    val manifestUrl: String,
    val licenseUrl: String?,
    val headers: Map<String, String>?,
    val watermark: WatermarkDto?,
    val expiresAt: String
)

@JsonClass(generateAdapter = true)
data class ProgressRequest(
    val videoAssetId: String,
    val positionSeconds: Int,
    val completed: Boolean
)

@JsonClass(generateAdapter = true)
data class OfflineDownloadRequest(
    val videoId: String,
    val deviceId: String
)

@JsonClass(generateAdapter = true)
data class OfflineDownloadResponse(
    val downloadUrl: String,
    val bytesExpected: Long? = null
)

@JsonClass(generateAdapter = true)
data class UpdateAccountRequest(
    val displayName: String,
    val password: String? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    val body: String
)

@JsonClass(generateAdapter = true)
data class MessageSenderDto(
    val id: String,
    val displayName: String? = null,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageDto(
    val id: String,
    val senderId: String? = null,
    val receiverId: String? = null,
    val sender: MessageSenderDto? = null,
    val body: String,
    val isFromUser: Boolean? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MessagesResponse(
    val messages: List<MessageDto>? = null,
    val items: List<MessageDto>? = null
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val read: Boolean? = null,
    val readAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DashboardSummaryDto(
    val user: UserDto,
    val unreadNotifications: Int,
    val unreadMessages: Int
)

@JsonClass(generateAdapter = true)
data class LoginErrorResponse(
    val accountStatus: String? = null,
    val adminMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminHostDto(
    val hostname: String? = null,
    val platform: String? = null,
    val arch: String? = null,
    val uptimeSeconds: Long? = null,
    val processUptimeSeconds: Long? = null
)

@JsonClass(generateAdapter = true)
data class AdminCpuDto(
    val cores: Int? = null,
    val loadAverage: List<Double>? = null,
    val usedPercent: Int? = null
)

@JsonClass(generateAdapter = true)
data class AdminMemoryDto(
    val totalBytes: Long? = null,
    val freeBytes: Long? = null,
    val usedBytes: Long? = null,
    val usedPercent: Int? = null
)

@JsonClass(generateAdapter = true)
data class AdminStorageDto(
    val path: String? = null,
    val totalBytes: Long? = null,
    val freeBytes: Long? = null,
    val usedBytes: Long? = null,
    val usedPercent: Int? = null
)

@JsonClass(generateAdapter = true)
data class AdminNetworkDto(
    val interfaceCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class AdminSystemStatusDto(
    val checkedAt: String? = null,
    val host: AdminHostDto? = null,
    val cpu: AdminCpuDto? = null,
    val memory: AdminMemoryDto? = null,
    val storage: AdminStorageDto? = null,
    val network: AdminNetworkDto? = null
)

@JsonClass(generateAdapter = true)
data class AdminUserDto(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val role: String? = null,
    val status: String? = null,
    val accessRestricted: Boolean? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminBasicUserDto(
    val id: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val role: String? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminConversationDto(
    val id: String,
    val userId: String? = null,
    val user: AdminBasicUserDto? = null,
    val unreadCount: Int? = null,
    val updatedAt: String? = null,
    val lastMessage: MessageDto? = null
)

@JsonClass(generateAdapter = true)
data class AdminDeviceSessionDto(
    val id: String,
    val deviceId: String? = null,
    val userAgent: String? = null,
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
    val expiresAt: String? = null,
    val user: AdminBasicUserDto? = null
)

@JsonClass(generateAdapter = true)
data class AdminUserStatusRequest(
    val status: String
)

@JsonClass(generateAdapter = true)
data class AdminMessageRequest(
    val body: String
)

@JsonClass(generateAdapter = true)
data class AdminNotificationRequest(
    val title: String,
    val body: String,
    val allUsers: Boolean = true,
    val userIds: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AdminSettingsUpdateRequest(
    val deleteOriginalAfterPreview: Boolean? = null,
    val maintenanceMode: Boolean? = null,
    val maintenanceMessage: String? = null,
    val backupScheduleEnabled: Boolean? = null,
    val backupScheduleDrive: Boolean? = null
)


