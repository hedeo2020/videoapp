package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AdminTrimJobRequest
import com.example.data.api.LoginRequest
import com.example.data.api.AdminConversationDto
import com.example.data.api.AdminDeviceSessionDto
import com.example.data.api.AdminMessageRequest
import com.example.data.api.AdminNotificationRequest
import com.example.data.api.AdminSettingsUpdateRequest
import com.example.data.api.AdminSystemStatusDto
import com.example.data.api.AdminUserDto
import com.example.data.api.AdminUserStatusRequest
import com.example.data.api.MovieCardDto
import com.example.data.api.OfflineDownloadRequest
import com.example.data.api.OfflineDownloadResponse
import com.example.data.api.PlaybackSessionRequest
import com.example.data.api.PlaybackSessionResponse
import com.example.data.api.ProgressRequest
import com.example.data.api.RailDto
import com.example.data.api.SecureStreamApi
import com.example.data.api.UpdateAccountRequest
import com.example.data.api.SendMessageRequest
import com.example.data.api.MessageDto
import com.example.data.api.NotificationDto
import com.example.data.api.DashboardSummaryDto
import com.example.data.storage.AppDatabase
import com.example.data.storage.DownloadedVideo
import com.example.data.storage.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.util.UUID

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val email: String, val displayName: String, val role: String? = null) : AuthState
    data class Error(val message: String) : AuthState
}

sealed interface CatalogState {
    object Loading : CatalogState
    data class Success(val rails: List<RailDto>) : CatalogState
    data class Error(val message: String) : CatalogState
}

sealed interface SearchState {
    object Idle : SearchState
    object Loading : SearchState
    data class Success(val results: List<MovieCardDto>) : SearchState
    data class Error(val message: String) : SearchState
}

sealed interface PlaybackState {
    object Idle : PlaybackState
    object Loading : PlaybackState
    data class Success(val response: PlaybackSessionResponse) : PlaybackState
    data class Error(val message: String) : PlaybackState
}

sealed interface DownloadStatus {
    object Idle : DownloadStatus
    data class Downloading(val progress: Float, val displayText: String = "") : DownloadStatus
    object Completed : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}

sealed interface AdminDashboardState {
    object Idle : AdminDashboardState
    object Loading : AdminDashboardState
    data class Success(
        val systemStatus: AdminSystemStatusDto?,
        val users: List<AdminUserDto>,
        val conversations: List<AdminConversationDto>,
        val deviceSessions: List<AdminDeviceSessionDto>,
        val panels: Map<String, AdminPanelData> = emptyMap()
    ) : AdminDashboardState
    data class Error(val message: String) : AdminDashboardState
}

data class AdminPanelData(
    val title: String,
    val subtitle: String,
    val rows: List<Map<String, Any?>> = emptyList(),
    val details: Map<String, Any?> = emptyMap(),
    val mobileNote: String? = null
)

data class AdminUploadState(
    val running: Boolean = false,
    val progress: Int = 0,
    val phase: String = ""
)

class SecureStreamViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val api = SecureStreamApi.create(application, tokenManager)

    private val database = AppDatabase.getDatabase(application)
    private val downloadDao = database.downloadedVideoDao()

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun checkInitialNetworkState(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    private val _isOnline = MutableStateFlow(checkInitialNetworkState())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            onNetworkReturned()
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
        }
    }

    val downloadedVideos: StateFlow<List<DownloadedVideo>> = downloadDao.getAllDownloadedVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalogState: StateFlow<CatalogState> = _catalogState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Screen navigation helpers & detail screen selected movie
    private val _selectedMovie = MutableStateFlow<MovieCardDto?>(null)
    val selectedMovie: StateFlow<MovieCardDto?> = _selectedMovie.asStateFlow()

    // Dashboard, messages, and notifications states
    private val _dashboardSummary = MutableStateFlow<DashboardSummaryDto?>(null)
    val dashboardSummary: StateFlow<DashboardSummaryDto?> = _dashboardSummary.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationDto>>(emptyList())
    val notifications: StateFlow<List<NotificationDto>> = _notifications.asStateFlow()

    private val _dashboardLoading = MutableStateFlow(false)
    val dashboardLoading: StateFlow<Boolean> = _dashboardLoading.asStateFlow()

    private val _dashboardError = MutableStateFlow<String?>(null)
    val dashboardError: StateFlow<String?> = _dashboardError.asStateFlow()

    private val _adminDashboardState = MutableStateFlow<AdminDashboardState>(AdminDashboardState.Idle)
    val adminDashboardState: StateFlow<AdminDashboardState> = _adminDashboardState.asStateFlow()

    private val _adminUploadState = MutableStateFlow(AdminUploadState())
    val adminUploadState: StateFlow<AdminUploadState> = _adminUploadState.asStateFlow()

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e("SecureStreamVM", "Failed to register network callback", e)
        }

        // Set up the unauthorized callback first to redirect to login on failure
        tokenManager.onUnauthorizedCallback = {
            viewModelScope.launch(Dispatchers.Main) {
                _authState.value = AuthState.Idle
                _catalogState.value = CatalogState.Loading
                _searchState.value = SearchState.Idle
                _selectedMovie.value = null
            }
        }

        // Check if already authenticated at startup
        if (tokenManager.isLoggedIn()) {
            _authState.value = AuthState.Success(
                email = tokenManager.getUserEmail() ?: "",
                displayName = tokenManager.getUserName() ?: (if (_isOnline.value) "Viewer" else "Offline Viewer"),
                role = tokenManager.getUserRole()
            )
            if (_isOnline.value) {
                loadCatalog()
                loadDashboardSummary()
            }
        } else {
            _authState.value = AuthState.Idle
        }
    }

    private fun onNetworkReturned() {
        viewModelScope.launch {
            if (tokenManager.isLoggedIn()) {
                _authState.value = AuthState.Success(
                    email = tokenManager.getUserEmail() ?: "",
                    displayName = tokenManager.getUserName() ?: "Viewer",
                    role = tokenManager.getUserRole()
                )
                loadCatalog()
                loadDashboardSummary()
            }
            syncPendingProgress()
        }
    }

    private fun syncPendingProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_isOnline.value) return@launch
            try {
                val pendingList = database.pendingProgressDao().getAllPendingProgress()
                for (pending in pendingList) {
                    try {
                        api.sendPlaybackProgress(
                            ProgressRequest(
                                videoAssetId = pending.videoAssetId,
                                positionSeconds = pending.positionSeconds,
                                completed = pending.completed
                            )
                        )
                        database.pendingProgressDao().deletePendingProgressById(pending.videoAssetId)
                    } catch (e: Exception) {
                        Log.e("SecureStreamVM", "Failed to sync pending progress for ${pending.videoAssetId}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Error querying pending progress", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.login(
                    LoginRequest(
                        email = email,
                        password = password,
                        deviceId = tokenManager.getDeviceId()
                    )
                )
                tokenManager.setAccessToken(response.accessToken)
                tokenManager.setRefreshToken(response.refreshToken)
                tokenManager.setUserEmail(response.user.email)
                tokenManager.setUserName(response.user.displayName)
                tokenManager.setUserId(response.user.id)
                tokenManager.setUserRole(response.user.role)

                _authState.value = AuthState.Success(
                    email = response.user.email,
                    displayName = response.user.displayName,
                    role = response.user.role
                )
                loadCatalog()
            } catch (e: Exception) {
                var errorMessageToShow = "Authentication failed"
                var handled = false
                if (e is retrofit2.HttpException) {
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        if (!errorBody.isNullOrBlank()) {
                            val moshi = com.squareup.moshi.Moshi.Builder()
                                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                                .build()
                            val adapter = moshi.adapter(com.example.data.api.LoginErrorResponse::class.java)
                            val errorJson = adapter.fromJson(errorBody)
                            if (errorJson != null && (errorJson.accountStatus != null || errorJson.adminMessage != null)) {
                                errorMessageToShow = errorJson.adminMessage ?: errorJson.accountStatus ?: "Authentication failed"
                                tokenManager.clear(notify = false)
                                handled = true
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e("SecureStreamVM", "Failed to parse error response")
                    }
                }
                if (!handled) {
                    errorMessageToShow = e.message ?: "Authentication failed"
                }
                _authState.value = AuthState.Error(errorMessageToShow)
            }
        }
    }

    fun logout() {
        tokenManager.clear()
        _authState.value = AuthState.Idle
        _catalogState.value = CatalogState.Loading
        _searchState.value = SearchState.Idle
        _selectedMovie.value = null
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _catalogState.value = CatalogState.Loading
            try {
                val response = api.getCatalog()
                _catalogState.value = CatalogState.Success(response.rails)
            } catch (e: Exception) {
                _catalogState.value = CatalogState.Error(e.message ?: "Failed to load catalog")
            }
        }
    }

    fun searchMovies(query: String) {
        if (query.length < 2) {
            _searchState.value = SearchState.Idle
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                val response = api.search(query)
                _searchState.value = SearchState.Success(response)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun selectMovie(movie: MovieCardDto?) {
        _selectedMovie.value = movie
    }

    fun startPlaybackSession(videoId: String, onSuccess: (PlaybackSessionResponse) -> Unit) {
        viewModelScope.launch {
            _playbackState.value = PlaybackState.Loading
            if (!_isOnline.value) {
                val downloadedVideo = downloadDao.getDownloadedVideoById(videoId)
                if (downloadedVideo != null) {
                    startOfflinePlayback(downloadedVideo, onSuccess)
                    return@launch
                } else {
                    _playbackState.value = PlaybackState.Error("You are offline and this video is not downloaded.")
                    return@launch
                }
            }
            try {
                val response = api.createPlaybackSession(
                    PlaybackSessionRequest(
                        videoId = videoId,
                        deviceId = tokenManager.getDeviceId()
                    )
                )
                _playbackState.value = PlaybackState.Success(response)
                onSuccess(response)
            } catch (e: Exception) {
                _playbackState.value = PlaybackState.Error(e.message ?: "Failed to initialize secure session")
            }
        }
    }

    fun reportPlaybackProgress(videoAssetId: String, positionSeconds: Int, completed: Boolean) {
        viewModelScope.launch {
            if (!_isOnline.value) {
                viewModelScope.launch(Dispatchers.IO) {
                    database.pendingProgressDao().insertPendingProgress(
                        com.example.data.storage.PendingProgress(
                            videoAssetId = videoAssetId,
                            positionSeconds = positionSeconds,
                            completed = completed
                        )
                    )
                }
                return@launch
            }
            try {
                api.sendPlaybackProgress(
                    ProgressRequest(
                        videoAssetId = videoAssetId,
                        positionSeconds = positionSeconds,
                        completed = completed
                    )
                )
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Failed to report progress: ${e.message}")
                viewModelScope.launch(Dispatchers.IO) {
                    database.pendingProgressDao().insertPendingProgress(
                        com.example.data.storage.PendingProgress(
                            videoAssetId = videoAssetId,
                            positionSeconds = positionSeconds,
                            completed = completed
                        )
                    )
                }
            }
        }
    }

    fun clearPlaybackState() {
        _playbackState.value = PlaybackState.Idle
    }

    fun downloadVideo(movie: MovieCardDto) {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadStates.value = _downloadStates.value + (movie.id to DownloadStatus.Downloading(0f, "Downloading... 0%"))
            try {
                val deviceId = tokenManager.getDeviceId()
                val downloadResponse = api.getOfflineDownloadUrl(
                    OfflineDownloadRequest(
                        videoId = movie.id,
                        deviceId = deviceId
                    )
                )
                val downloadUrl = downloadResponse.downloadUrl

                var localFile: java.io.File? = null
                try {
                    val offlineDir = java.io.File(getApplication<Application>().filesDir, "securestream_offline")
                    if (!offlineDir.exists()) {
                        offlineDir.mkdirs()
                    }
                    val file = java.io.File(offlineDir, "video_${movie.id}.mp4")
                    localFile = file

                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url(downloadUrl)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw java.io.IOException("Failed to download file: $response")
                        }
                        val body = response.body ?: throw java.io.IOException("Empty response body")
                        
                        val totalBytes = downloadResponse.bytesExpected ?: body.contentLength()

                        body.byteStream().use { inputStream ->
                            file.outputStream().use { outputStream ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var bytesDownloaded = 0L
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    bytesDownloaded += bytesRead
                                    
                                    val displayText = if (totalBytes > 0L) {
                                        val percent = bytesDownloaded * 100 / totalBytes
                                        "Downloading... $percent%"
                                    } else {
                                        val mbDownloaded = bytesDownloaded.toDouble() / (1024.0 * 1024.0)
                                        String.format(java.util.Locale.US, "Downloading... %.1f MB", mbDownloaded)
                                    }
                                    
                                    val progress = if (totalBytes > 0L) {
                                        bytesDownloaded.toFloat() / totalBytes
                                    } else {
                                        0f
                                    }
                                    
                                    _downloadStates.value = _downloadStates.value + (movie.id to DownloadStatus.Downloading(progress, displayText))
                                }
                            }
                        }
                    }

                    val durationText = when {
                        !movie.durationText.isNullOrBlank() -> movie.durationText
                        movie.durationSeconds != null && movie.durationSeconds > 0 -> "${movie.durationSeconds / 60}m"
                        else -> "N/A"
                    }

                    val downloadedVideo = DownloadedVideo(
                        videoAssetId = movie.id,
                        title = movie.title,
                        artworkUrl = movie.artworkUrl,
                        duration = durationText,
                        localPath = file.absolutePath,
                        downloadedAt = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
                    )

                    downloadDao.insertDownloadedVideo(downloadedVideo)
                    _downloadStates.value = _downloadStates.value + (movie.id to DownloadStatus.Completed)
                } catch (t: Throwable) {
                    try {
                        localFile?.let {
                            if (it.exists()) {
                                it.delete()
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e("SecureStreamVM", "Failed to delete partial file", ex)
                    }
                    _downloadStates.value = _downloadStates.value + (movie.id to DownloadStatus.Error(t.message ?: "Download failed"))
                    Log.e("SecureStreamVM", "Error downloading video", t)
                    if (t is kotlinx.coroutines.CancellationException) {
                        throw t
                    }
                }
            } catch (e: Exception) {
                _downloadStates.value = _downloadStates.value + (movie.id to DownloadStatus.Error(e.message ?: "Download failed"))
                Log.e("SecureStreamVM", "Error fetching download URL", e)
            }
        }
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val video = downloadDao.getDownloadedVideoById(videoId)
                if (video != null) {
                    val file = java.io.File(video.localPath)
                    if (file.exists()) {
                        file.delete()
                    }
                    downloadDao.deleteDownloadedVideoById(videoId)
                }
                _downloadStates.value = _downloadStates.value - videoId
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Failed to delete download: ${e.message}")
            }
        }
    }

    fun startOfflinePlayback(video: DownloadedVideo, onSuccess: (PlaybackSessionResponse) -> Unit) {
        val mockResponse = PlaybackSessionResponse(
            sessionId = "offline_${video.videoAssetId}",
            manifestUrl = video.localPath,
            licenseUrl = null,
            headers = null,
            watermark = null,
            expiresAt = ""
        )
        _playbackState.value = PlaybackState.Success(mockResponse)
        val mockMovie = MovieCardDto(
            id = video.videoAssetId,
            title = video.title,
            slug = "",
            artworkUrl = video.artworkUrl,
            synopsis = "Offline Playback",
            maturityRating = "PG-13",
            kind = "VIDEO",
            durationSeconds = null,
            durationText = video.duration
        )
        selectMovie(mockMovie)
        onSuccess(mockResponse)
    }

    fun getUserId(): String? = tokenManager.getUserId()

    fun isAdminUser(): Boolean = tokenManager.getUserRole() in setOf("SUPER_ADMIN", "ADMIN", "EDITOR")

    fun loadDashboardSummary() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            try {
                val summary = api.getDashboardSummary()
                _dashboardSummary.value = summary
                
                // Keep tokenManager user updated if necessary
                tokenManager.setUserName(summary.user.displayName)
                tokenManager.setUserEmail(summary.user.email)
                
                // Update AuthState with the latest user details
                if (_authState.value is AuthState.Success) {
                    _authState.value = AuthState.Success(
                        email = summary.user.email,
                        displayName = summary.user.displayName,
                        role = summary.user.role
                    )
                }
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Failed to load dashboard summary")
            }
        }
    }

    fun updateAccount(
        displayName: String,
        password: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!_isOnline.value) {
            onError("Cannot update profile while offline")
            return
        }
        viewModelScope.launch {
            _dashboardLoading.value = true
            _dashboardError.value = null
            try {
                val updatedUser = api.updateAccount(
                    UpdateAccountRequest(
                        displayName = displayName,
                        password = if (password.isNullOrBlank()) null else password
                    )
                )
                // Save updated info locally
                tokenManager.setUserName(updatedUser.displayName)
                tokenManager.setUserEmail(updatedUser.email)
                
                // Update AuthState so UI shows new name instantly
                _authState.value = AuthState.Success(
                    email = updatedUser.email,
                    displayName = updatedUser.displayName,
                    role = updatedUser.role
                )
                
                // Refresh dashboard summary
                loadDashboardSummary()
                
                _dashboardLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _dashboardLoading.value = false
                val errMsg = e.message ?: "Failed to update account"
                _dashboardError.value = errMsg
                onError(errMsg)
            }
        }
    }

    fun loadMessages() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            try {
                val response = api.getMessages()
                val list = response.messages ?: response.items ?: emptyList()
                val sortedList = list.sortedBy { it.createdAt ?: "" }
                _messages.value = sortedList
                // Refresh dashboard summary to update unread badge
                loadDashboardSummary()
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Failed to load messages")
            }
        }
    }

    fun sendMessage(body: String) {
        if (body.isBlank() || !_isOnline.value) return
        viewModelScope.launch {
            try {
                api.sendMessage(SendMessageRequest(body = body))
                loadMessages() // Refresh messages after sending
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Failed to send message")
            }
        }
    }

    fun loadNotifications() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            try {
                val list = api.getNotifications()
                _notifications.value = list
                // Refresh dashboard summary to update unread badge
                loadDashboardSummary()
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Failed to load notifications")
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        if (!_isOnline.value) return
        viewModelScope.launch {
            try {
                api.markNotificationAsRead(id)
                loadNotifications()
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Failed to mark notification as read")
            }
        }
    }

    fun markAllNotificationsAsRead() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            try {
                api.markAllNotificationsAsRead()
                loadNotifications()
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Failed to mark all notifications as read")
            }
        }
    }

    fun loadAdminDashboard(silent: Boolean = false) {
        if (!_isOnline.value) return
        if (!isAdminUser()) {
            _adminDashboardState.value = AdminDashboardState.Error("Administrator access is required")
            return
        }
        viewModelScope.launch {
            val currentState = _adminDashboardState.value
            if (!silent || currentState !is AdminDashboardState.Success) {
                _adminDashboardState.value = AdminDashboardState.Loading
            }
            try {
                val systemStatus = try { api.getAdminSystemStatus() } catch (e: Exception) { null }
                val users = try { api.getAdminUsers() } catch (e: Exception) { emptyList() }
                val conversations = try { api.getAdminConversations() } catch (e: Exception) { emptyList() }
                val deviceSessions = try { api.getAdminDeviceSessions() } catch (e: Exception) { emptyList() }
                val panels = buildMap {
                    put("Catalog", AdminPanelData("Catalog", "Folders and published catalog organization", rows = runCatching { api.getAdminCollectionsRaw() }.getOrDefault(emptyList())))
                    put("Videos", AdminPanelData("Videos", "Video records, status, and assets", rows = runCatching { api.getAdminMoviesRaw() }.getOrDefault(emptyList())))
                    put("Video Editor", AdminPanelData("Video Editor", "Trim jobs and editor queue", rows = runCatching { api.getAdminEditorJobsRaw() }.getOrDefault(emptyList()), details = mapOf("assets" to runCatching { api.getAdminFilesRaw() }.getOrDefault(emptyList()))))
                    put("File Manager", AdminPanelData("File Manager", "Stored media files and generated previews", rows = runCatching { api.getAdminFilesRaw() }.getOrDefault(emptyList())))
                    put("Storage", AdminPanelData("Storage", "Storage breakdown and cleanup information", details = runCatching { api.getAdminStorageBreakdownRaw() }.getOrDefault(emptyMap())))
                    put("Series", AdminPanelData("Series", "Series, seasons, and episodes", rows = runCatching { api.getAdminSeriesRaw() }.getOrDefault(emptyList())))
                    put("Uploads", AdminPanelData("Uploads", "Upload center", rows = runCatching { api.getAdminMoviesRaw() }.getOrDefault(emptyList()), mobileNote = "Large files can take time on mobile. Keep this screen open until upload and preview conversion finish."))
                    put("Processing", AdminPanelData("Processing", "Transcoding and worker queue", details = runCatching { api.getAdminProcessingRaw() }.getOrDefault(emptyMap())))
                    put("Collections", AdminPanelData("Collections", "Folders, subfolders, and ordering", rows = runCatching { api.getAdminCollectionsRaw() }.getOrDefault(emptyList())))
                    put("Notifications", AdminPanelData("Notifications", "Admin announcements sent to users", rows = runCatching { api.getAdminNotificationsRaw() }.getOrDefault(emptyList())))
                    put("API Tokens", AdminPanelData("API Tokens", "Automation tokens for n8n and integrations", rows = runCatching { api.getAdminApiTokensRaw() }.getOrDefault(emptyList()), mobileNote = "Creating/revealing tokens should be done carefully from the web panel."))
                    put("Playback sessions", AdminPanelData("Playback sessions", "Recent playback activity", rows = runCatching { api.getAdminPlaybackRaw() }.getOrDefault(emptyList())))
                    put("Watermark Trace", AdminPanelData("Watermark Trace", "Trace visible playback watermarks", rows = runCatching { api.getAdminPlaybackRaw() }.getOrDefault(emptyList()), mobileNote = "Use the web panel for exact watermark paste/search; mobile shows source sessions."))
                    put("Backup & Restore", AdminPanelData("Backup & Restore", "Portable backups and Google Drive status", details = runCatching { api.getAdminBackupsRaw() }.getOrDefault(emptyMap()), mobileNote = "Restore uploads are intentionally left to the web panel for safety."))
                    put("Activity", AdminPanelData("Activity", "Recent admin activity", rows = runCatching { api.getAdminActivityRaw() }.getOrDefault(emptyList())))
                    put("Trash", AdminPanelData("Trash", "Deleted users and videos", details = runCatching { api.getAdminTrashRaw() }.getOrDefault(emptyMap())))
                    put("Audit logs", AdminPanelData("Audit logs", "Audit trail", rows = runCatching { api.getAdminAuditLogsRaw() }.getOrDefault(emptyList())))
                    put("Security", AdminPanelData("Security", "Security events", rows = runCatching { api.getAdminSecurityEventsRaw() }.getOrDefault(emptyList())))
                    put("Settings", AdminPanelData("Settings", "Platform settings and maintenance mode", details = runCatching { api.getAdminSettingsRaw() }.getOrDefault(emptyMap()), mobileNote = "Editing settings from mobile can be added after confirmation; current view is read-only."))
                }
                _adminDashboardState.value = AdminDashboardState.Success(
                    systemStatus = systemStatus,
                    users = users,
                    conversations = conversations,
                    deviceSessions = deviceSessions,
                    panels = panels
                )
            } catch (e: Exception) {
                if (!silent || _adminDashboardState.value !is AdminDashboardState.Success) {
                    _adminDashboardState.value = AdminDashboardState.Error(e.message ?: "Failed to load admin dashboard")
                }
            }
        }
    }

    private fun adminAction(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        successMessage: String,
        block: suspend () -> Unit
    ) {
        if (!_isOnline.value) {
            onError("Admin action unavailable while offline")
            return
        }
        if (!isAdminUser()) {
            onError("Administrator access is required")
            return
        }
        viewModelScope.launch {
            try {
                block()
                loadAdminDashboard(silent = true)
                onSuccess(successMessage)
            } catch (e: Exception) {
                Log.e("SecureStreamVM", "Admin action failed", e)
                onError(e.message ?: "Admin action failed")
            }
        }
    }

    fun adminSetUserStatus(userId: String, status: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) =
        adminAction(onSuccess, onError, "User status updated") {
            api.updateAdminUserStatus(userId, AdminUserStatusRequest(status))
        }

    fun adminDeleteUser(userId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) =
        adminAction(onSuccess, onError, "User deleted") {
            api.deleteAdminUser(userId)
        }

    fun adminRevokeDeviceSession(sessionId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) =
        adminAction(onSuccess, onError, "Device session revoked") {
            api.revokeAdminDeviceSession(sessionId)
        }

    fun adminReplyToConversation(conversationId: String, body: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) =
        adminAction(onSuccess, onError, "Reply sent") {
            api.sendAdminConversationMessage(conversationId, AdminMessageRequest(body.trim()))
        }

    fun adminSendNotification(title: String, body: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) =
        adminAction(onSuccess, onError, "Notification sent") {
            api.sendAdminNotification(AdminNotificationRequest(title = title.trim(), body = body.trim(), allUsers = true))
        }

    fun adminCreateBackup(onSuccess: (String) -> Unit, onError: (String) -> Unit) =
        adminAction(onSuccess, onError, "Backup started") {
            api.createAdminBackup()
        }

    fun adminRunScheduledBackupNow(onSuccess: (String) -> Unit, onError: (String) -> Unit) =
        adminAction(onSuccess, onError, "Scheduled backup started") {
            api.runAdminScheduledBackupNow()
        }

    fun adminTestAlert(onSuccess: (String) -> Unit, onError: (String) -> Unit) =
        adminAction(onSuccess, onError, "Test alert sent") {
            api.testAdminAlert()
        }

    fun adminUpdateSettings(
        deleteOriginalAfterPreview: Boolean? = null,
        maintenanceMode: Boolean? = null,
        maintenanceMessage: String? = null,
        backupScheduleEnabled: Boolean? = null,
        backupScheduleDrive: Boolean? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) = adminAction(onSuccess, onError, "Settings updated") {
        api.updateAdminSettings(
            AdminSettingsUpdateRequest(
                deleteOriginalAfterPreview = deleteOriginalAfterPreview,
                maintenanceMode = maintenanceMode,
                maintenanceMessage = maintenanceMessage,
                backupScheduleEnabled = backupScheduleEnabled,
                backupScheduleDrive = backupScheduleDrive
            )
        )
    }

    fun adminUploadVideo(
        uri: Uri,
        title: String,
        synopsis: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!_isOnline.value) {
            onError("Upload unavailable while offline")
            return
        }
        if (!isAdminUser()) {
            onError("Administrator access is required")
            return
        }
        if (title.isBlank()) {
            onError("Title is required")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            var progressPoller: kotlinx.coroutines.Job? = null
            try {
                val uploadId = UUID.randomUUID().toString()
                val fileName = displayNameForUri(uri) ?: "securestream-upload.mp4"
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                val size = sizeForUri(uri)
                _adminUploadState.value = AdminUploadState(running = true, progress = 1, phase = "Preparing upload")
                progressPoller = launch {
                    while (isActive) {
                        delay(1000)
                        val progress = runCatching { api.getAdminUploadProgress(uploadId) }.getOrNull() ?: continue
                        val phase = (progress["phase"] as? String).orEmpty()
                        val serverProgress = when (val value = progress["progress"]) {
                            is Number -> value.toInt()
                            is String -> value.toIntOrNull() ?: 0
                            else -> 0
                        }.coerceIn(0, 100)
                        if (phase.contains("convert", ignoreCase = true) || phase.contains("ready", ignoreCase = true)) {
                            _adminUploadState.value = AdminUploadState(
                                running = true,
                                progress = serverProgress,
                                phase = phase.ifBlank { "Converting preview" }
                            )
                        }
                    }
                }
                val body = StreamingUriRequestBody(
                    context = getApplication(),
                    uri = uri,
                    mimeType = mimeType,
                    size = size
                ) { sent, total ->
                    val percent = if (total > 0) ((sent * 94) / total).toInt().coerceIn(1, 94) else 50
                    _adminUploadState.value = AdminUploadState(running = true, progress = percent, phase = "Uploading")
                }
                val part = MultipartBody.Part.createFormData("file", fileName, body)
                val textType = "text/plain".toMediaTypeOrNull()
                api.uploadAdminVideoDirect(
                    uploadId = uploadId,
                    file = part,
                    title = title.trim().toRequestBody(textType),
                    synopsis = synopsis.trim().toRequestBody(textType),
                    maturityRating = "".toRequestBody(textType)
                )
                progressPoller?.cancel()
                _adminUploadState.value = AdminUploadState(running = true, progress = 98, phase = "Processing preview")
                loadAdminDashboard(silent = true)
                _adminUploadState.value = AdminUploadState(running = false, progress = 100, phase = "Upload complete")
                onSuccess("Video uploaded as draft")
            } catch (e: Exception) {
                progressPoller?.cancel()
                Log.e("SecureStreamVM", "Admin upload failed", e)
                _adminUploadState.value = AdminUploadState()
                onError(e.message ?: "Upload failed")
            }
        }
    }

    fun adminCreateTrimJob(
        assetId: String,
        startSeconds: Double,
        endSeconds: Double,
        title: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) = adminAction(onSuccess, onError, "Trim job queued") {
        if (assetId.isBlank()) error("Choose a video asset first")
        if (endSeconds <= startSeconds) error("End time must be after start time")
        api.createAdminTrimJob(
            AdminTrimJobRequest(
                assetId = assetId.trim(),
                startSeconds = startSeconds,
                endSeconds = endSeconds,
                title = title.trim().ifBlank { null }
            )
        )
    }

    private fun displayNameForUri(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun sizeForUri(uri: Uri): Long {
        val resolver = getApplication<Application>().contentResolver
        return resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        } ?: -1L
    }
}

private class StreamingUriRequestBody(
    private val context: Context,
    private val uri: Uri,
    private val mimeType: String,
    private val size: Long,
    private val onProgress: (sent: Long, total: Long) -> Unit
) : RequestBody() {
    override fun contentType() = mimeType.toMediaTypeOrNull()
    override fun contentLength() = size

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded, size)
            }
        } ?: error("Cannot open selected file")
    }
}
