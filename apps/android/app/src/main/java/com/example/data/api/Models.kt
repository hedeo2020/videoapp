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


