package com.example.ui.screens

import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.MainActivity
import com.example.data.api.PlaybackSessionResponse
import com.example.ui.theme.SecureMintAccent
import com.example.ui.theme.SecureTextWhite
import com.example.ui.viewmodel.SecureStreamViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(UnstableApi::class)
@Composable
fun PlaybackScreen(
    viewModel: SecureStreamViewModel,
    videoId: String,
    title: String,
    playbackSession: PlaybackSessionResponse,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    var isFullscreen by remember { mutableStateOf(false) }
    var isFitMode by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }

    // Intercept standard Android physical back button to exit fullscreen first
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // Apply FLAG_SECURE to prevent screenshots/recording and handle lifecycle restoration
    DisposableEffect(Unit) {
        activity?.enterFullscreenPlayback()
        onDispose {
            activity?.exitFullscreenPlayback()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            viewModel.clearPlaybackState()
        }
    }

    // Handle Immersive Mode & Landscape requestedOrientation dynamically
    LaunchedEffect(isFullscreen) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                // Set requestedOrientation to sensor-driven landscape
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                // Restore portrait
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        val headers = playbackSession.headers ?: emptyMap()
        httpDataSourceFactory.setDefaultRequestProperties(headers)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    var isPlayerLoading by remember { mutableStateOf(true) }

    // Setup player state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isPlayerLoading = state == Player.STATE_BUFFERING
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Prepare and Start Playback
    LaunchedEffect(playbackSession) {
        val origin = "https://compreface.3dbpoint.com"
        val fullUrl = when {
            playbackSession.manifestUrl.startsWith("http") -> playbackSession.manifestUrl
            playbackSession.manifestUrl.startsWith("/") || playbackSession.manifestUrl.startsWith("file:") -> playbackSession.manifestUrl
            else -> origin + playbackSession.manifestUrl
        }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(fullUrl))

        // Configure Widevine DRM if licenseUrl is returned
        if (!playbackSession.licenseUrl.isNullOrEmpty()) {
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(playbackSession.licenseUrl)
                    .build()
            )
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Periodic watch progress reporting (every 10 seconds)
    LaunchedEffect(exoPlayer) {
        if (playbackSession.sessionId.startsWith("offline")) return@LaunchedEffect
        while (true) {
            delay(10000)
            val currentPosSeconds = (exoPlayer.currentPosition / 1000).toInt()
            if (currentPosSeconds > 0 && exoPlayer.playbackState != Player.STATE_IDLE) {
                viewModel.reportPlaybackProgress(
                    videoAssetId = videoId,
                    positionSeconds = currentPosSeconds,
                    completed = exoPlayer.playbackState == Player.STATE_ENDED
                )
            }
        }
    }

    // Report final watch progress on exit
    DisposableEffect(Unit) {
        onDispose {
            if (!playbackSession.sessionId.startsWith("offline")) {
                val finalPosSeconds = (exoPlayer.currentPosition / 1000).toInt()
                if (finalPosSeconds > 0) {
                    viewModel.reportPlaybackProgress(
                        videoAssetId = videoId,
                        positionSeconds = finalPosSeconds,
                        completed = exoPlayer.playbackState == Player.STATE_ENDED
                    )
                }
            }
        }
    }

    // Forensic Watermark Motion Logic
    val watermark = playbackSession.watermark
    var watermarkOffset by remember { mutableStateOf(Offset(0.5f, 0.5f)) }

    if (watermark != null) {
        LaunchedEffect(watermark.moveEverySeconds) {
            val moveIntervalMs = watermark.moveEverySeconds * 1000L
            while (true) {
                delay(moveIntervalMs)
                // Random position offsets between 10% and 80% of container size
                val randomX = Random.nextFloat() * 0.7f + 0.1f
                val randomY = Random.nextFloat() * 0.7f + 0.1f
                watermarkOffset = Offset(randomX, randomY)
            }
        }
    }

    // Render Player Layout
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_container")
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        // Media3 Android View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Sync player controls visibility with our Compose state
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        showControls = (visibility == View.VISIBLE)
                    })
                    // Sync the native controller fullscreen button clicks
                    setFullscreenButtonClickListener { isFullscreenClick ->
                        isFullscreen = isFullscreenClick
                    }
                }
            },
            update = { playerView ->
                playerView.resizeMode = if (isFitMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Overlay Controls (back button, title, aspect ratio, fullscreen button)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Custom Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = if (isFullscreen) 16.dp else 24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isFullscreen) {
                                isFullscreen = false
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Player",
                            tint = SecureMintAccent
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = SecureTextWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Fit / Fill scaling selector
                    IconButton(
                        onClick = { isFitMode = !isFitMode },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_aspect_ratio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AspectRatio,
                            contentDescription = "Toggle Scaling",
                            tint = if (isFitMode) SecureMintAccent else Color.White
                        )
                    }

                    Text(
                        text = if (isFitMode) "FIT" else "FILL",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFitMode) SecureMintAccent else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                // Bottom Right Floating Fullscreen Controls (positioned elegantly above the seekbar)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 96.dp, end = 24.dp)
                ) {
                    IconButton(
                        onClick = { isFullscreen = !isFullscreen },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, SecureMintAccent.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                            .testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                            tint = SecureMintAccent
                        )
                    }
                }
            }
        }

        // Buffer Loading overlay
        if (isPlayerLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SecureMintAccent)
            }
        }

        // Forensic Watermark overlay (smooth floating relative to layout constraints)
        if (watermark != null) {
            val xPos = containerWidth * watermarkOffset.x
            val yPos = containerHeight * watermarkOffset.y

            val animatedOffset by animateOffsetAsState(
                targetValue = Offset(xPos.value, yPos.value),
                animationSpec = tween(durationMillis = 1000)
            )

            Text(
                text = watermark.text,
                color = Color.White.copy(alpha = watermark.opacity),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            animatedOffset.x.toInt(),
                            animatedOffset.y.toInt()
                        )
                    }
                    .testTag("forensic_watermark")
            )
        }
    }
}
