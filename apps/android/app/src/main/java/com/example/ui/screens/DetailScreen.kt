package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.api.MovieCardDto
import com.example.data.api.PlaybackSessionResponse
import com.example.ui.theme.SecureDarkBackground
import com.example.ui.theme.SecureDarkGray
import com.example.ui.theme.SecureDarkSurface
import com.example.ui.theme.SecureMintAccent
import com.example.ui.theme.SecureTextGray
import com.example.ui.theme.SecureTextWhite
import com.example.ui.viewmodel.DownloadStatus
import com.example.ui.viewmodel.PlaybackState
import com.example.ui.viewmodel.SecureStreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: SecureStreamViewModel,
    movie: MovieCardDto,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (PlaybackSessionResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    val isDownloaded = downloadedVideos.any { it.videoAssetId == movie.id }
    val downloadState = downloadStates[movie.id] ?: DownloadStatus.Idle

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (movie.kind == "SERIES") "Series Details" else "Video Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecureTextWhite
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SecureMintAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SecureDarkBackground,
                    titleContentColor = SecureTextWhite
                )
            )
        },
        containerColor = SecureDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Image Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(SecureDarkGray)
            ) {
                val imageUrl = normalizeImageUrl(bestFeaturedImageUrl(movie))
                if (imageUrl != null) {
                    val isError = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    if (!isError.value) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Cover photo of ${movie.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onState = { state ->
                                isError.value = state is coil.compose.AsyncImagePainter.State.Error
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = null,
                                tint = SecureMintAccent,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = SecureMintAccent,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // Text Metadata Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SecureTextWhite,
                    modifier = Modifier.testTag("detail_title")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    movie.maturityRating?.let {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SecureDarkGray
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = SecureTextWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val durationToShow = when {
                        !movie.durationText.isNullOrBlank() -> movie.durationText
                        movie.durationSeconds != null && movie.durationSeconds > 0 -> "${movie.durationSeconds / 60}m"
                        else -> null
                    }

                    durationToShow?.let { duration ->
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecureTextGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Play Button Block
                Button(
                    onClick = {
                        viewModel.startPlaybackSession(movie.id, onNavigateToPlayer)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("play_button"),
                    enabled = playbackState !is PlaybackState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecureMintAccent,
                        contentColor = SecureDarkBackground,
                        disabledContainerColor = SecureMintAccent.copy(alpha = 0.3f),
                        disabledContentColor = SecureDarkBackground.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (playbackState is PlaybackState.Loading) {
                        CircularProgressIndicator(
                            color = SecureDarkBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play video",
                            tint = SecureDarkBackground,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play Now",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Download Button Block
                if (isDownloaded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val downloadedVideo = downloadedVideos.firstOrNull { it.videoAssetId == movie.id }
                                if (downloadedVideo != null) {
                                    viewModel.startOfflinePlayback(downloadedVideo, onNavigateToPlayer)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("play_offline_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecureDarkGray,
                                contentColor = SecureMintAccent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play Offline",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Play Offline",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.deleteDownload(movie.id)
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("delete_download_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Download",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    when (downloadState) {
                        is DownloadStatus.Downloading -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("download_progress_button"),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = SecureDarkGray,
                                    disabledContentColor = SecureMintAccent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = SecureMintAccent,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = downloadState.displayText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is DownloadStatus.Error -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { viewModel.downloadVideo(movie) },
                                    enabled = isOnline,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("download_retry_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SecureDarkGray,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Retry Download",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Retry Download",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = downloadState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = { viewModel.downloadVideo(movie) },
                                enabled = isOnline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("download_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SecureDarkGray,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Download video",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Download Offline",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (playbackState is PlaybackState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = (playbackState as PlaybackState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Synopsis/Storyline
                Text(
                    text = "Storyline",
                    style = MaterialTheme.typography.titleLarge,
                    color = SecureTextWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = movie.synopsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SecureTextWhite.copy(alpha = 0.85f),
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Metadata disclaimer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = SecureTextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted protected playback. No local caching allowed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecureTextGray
                    )
                }
            }
        }
    }
}
