package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.config.AppConfig
import com.example.data.api.MovieCardDto
import com.example.data.api.RailDto
import com.example.ui.components.ShimmerPlaceholder
import com.example.ui.theme.SecureDarkBackground
import com.example.ui.theme.SecureDarkGray
import com.example.ui.theme.SecureDarkSurface
import com.example.ui.theme.SecureMintAccent
import com.example.ui.theme.SecureTextGray
import com.example.ui.theme.SecureTextWhite
import com.example.ui.viewmodel.CatalogState
import com.example.ui.viewmodel.SecureStreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: SecureStreamViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToDetail: (MovieCardDto) -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catalogState by viewModel.catalogState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()
    val totalUnread = (summary?.unreadNotifications ?: 0) + (summary?.unreadMessages ?: 0)

    val offlineMovies = remember(downloadedVideos) {
        downloadedVideos.map { video ->
            MovieCardDto(
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
        }
    }

    // Load catalog and poll dashboard summary when entering screen only if online
    LaunchedEffect(isOnline) {
        if (isOnline) {
            viewModel.loadCatalog()
            viewModel.loadDashboardSummary()
            while (true) {
                kotlinx.coroutines.delay(5000)
                viewModel.loadDashboardSummary()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SECURESTREAM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecureMintAccent
                    )
                },
                actions = {
                    if (isOnline) {
                        IconButton(
                            onClick = onNavigateToDashboard,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dashboard_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (totalUnread > 0) {
                                        Badge(
                                            containerColor = SecureMintAccent,
                                            contentColor = SecureDarkBackground
                                        ) {
                                            Text(totalUnread.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "User Dashboard",
                                    tint = SecureMintAccent
                                )
                            }
                        }
                        IconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search videos",
                                tint = SecureMintAccent
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToDownloads,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("downloads_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Downloads",
                            tint = SecureMintAccent
                        )
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log out",
                            tint = SecureTextGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SecureDarkBackground,
                    titleContentColor = SecureMintAccent
                )
            )
        },
        containerColor = SecureDarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isOnline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OFFLINE MODE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (!isOnline) {
                    if (downloadedVideos.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Offline icon",
                                tint = SecureTextGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No downloaded videos yet. Connect to the internet and download videos to watch offline.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SecureTextWhite,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "Downloaded videos",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = SecureTextWhite,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                    )

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        itemsIndexed(offlineMovies, key = { _, item -> item.id }) { index, item ->
                                            StaggeredRailItem(index = index) {
                                                MovieCatalogCard(
                                                    movie = item,
                                                    onClick = { onNavigateToDetail(item) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Online mode content
                    when (val state = catalogState) {
                        is CatalogState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SecureMintAccent)
                            }
                        }
                        is CatalogState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Error Loading Catalog",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecureTextGray,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Button(
                                    onClick = { viewModel.loadCatalog() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecureMintAccent, contentColor = SecureDarkBackground)
                                ) {
                                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Retry")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Retry")
                                }
                            }
                        }
                        is CatalogState.Success -> {
                            if (state.rails.isEmpty()) {
                                EmptyCatalogState()
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    item {
                                        FeaturedHeroBanner(
                                            onMovieClick = onNavigateToDetail,
                                            state = state
                                        )
                                    }
                                    items(state.rails) { rail ->
                                        CatalogRailSection(
                                            rail = rail,
                                            onMovieClick = onNavigateToDetail
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogRailSection(
    rail: RailDto,
    onMovieClick: (MovieCardDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = rail.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SecureTextWhite,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(rail.items, key = { _, item -> item.id }) { index, item ->
                StaggeredRailItem(index = index) {
                    MovieCatalogCard(
                        movie = item,
                        onClick = { onMovieClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun StaggeredRailItem(
    index: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 240, delayMillis = (index * 40).coerceAtMost(280))
        ) + slideInHorizontally(
            animationSpec = tween(durationMillis = 240, delayMillis = (index * 40).coerceAtMost(280)),
            initialOffsetX = { it / 5 }
        )
    ) {
        content()
    }
}

fun bestImageUrl(item: MovieCardDto): String? {
    return item.artworkUrl
        ?: item.posterUrl
        ?: item.backdropUrl
        ?: item.heroImageUrl
}

fun bestFeaturedImageUrl(item: MovieCardDto): String? {
    return item.heroImageUrl
        ?: item.backdropUrl
        ?: item.artworkUrl
        ?: item.posterUrl
}

fun normalizeImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("/") -> "${AppConfig.WEB_BASE_URL}$url"
        else -> url
    }
}

@Composable
fun PlaceholderCardContent(movie: MovieCardDto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = SecureMintAccent,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = SecureTextWhite,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MovieCatalogCard(
    movie: MovieCardDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "movie_card_press_scale"
    )

    Card(
        modifier = modifier
            .width(160.dp)
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("movie_card_${movie.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SecureDarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(SecureDarkGray)
            ) {
                val imageUrl = normalizeImageUrl(bestImageUrl(movie))
                if (imageUrl != null) {
                    val isError = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    val isLoading = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
                    if (!isError.value) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isLoading.value) {
                                ShimmerPlaceholder(
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 0.dp
                                )
                            }
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Poster of ${movie.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onState = { state ->
                                    isError.value = state is coil.compose.AsyncImagePainter.State.Error
                                    isLoading.value = state is coil.compose.AsyncImagePainter.State.Loading
                                }
                            )
                            // Subtle dark gradient overlay so text at the bottom is highly readable
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                            startY = 150f
                                        )
                                    )
                            )
                        }
                    } else {
                        PlaceholderCardContent(movie)
                    }
                } else {
                    PlaceholderCardContent(movie)
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SecureTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (movie.kind == "SERIES") "Series" else "Video",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecureTextGray
                    )
                    movie.maturityRating?.let {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.DarkGray,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = SecureTextWhite,
                                modifier = Modifier.padding(horizontal = 4.add(2).dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom extension for ease of int adding
private fun Int.add(i: Int): Int = this + i

@Composable
fun EmptyCatalogState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = SecureMintAccent,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your catalog is empty",
            style = MaterialTheme.typography.titleLarge,
            color = SecureTextWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your catalog is configured by your administrator.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecureTextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FeaturedHeroBanner(
    onMovieClick: (MovieCardDto) -> Unit,
    state: CatalogState.Success,
    modifier: Modifier = Modifier
) {
    // Pick the very first movie in any rail to feature, or default if none
    val featuredMovie = state.rails.flatMap { it.items }.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SecureDarkSurface)
            .clickable {
                featuredMovie?.let { onMovieClick(it) }
            }
            .testTag("featured_hero_banner")
    ) {
        // Hero background image
        val heroUrl = featuredMovie?.let { normalizeImageUrl(bestFeaturedImageUrl(it)) }
        if (heroUrl != null) {
            val isError = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            val isLoading = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            if (!isError.value) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading.value) {
                        ShimmerPlaceholder(
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 18.dp
                        )
                    }
                    AsyncImage(
                        model = heroUrl,
                        contentDescription = "Featured Video Backdrop",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            isError.value = state is coil.compose.AsyncImagePainter.State.Error
                            isLoading.value = state is coil.compose.AsyncImagePainter.State.Loading
                        }
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1783901726010),
                    contentDescription = "Featured Video Backdrop",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1783901726010),
                contentDescription = "Featured Video Backdrop",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Sleek gradient overlay from bottom to top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 150f
                    )
                )
        )

        // Info text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = SecureMintAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = SecureMintAccent.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "FEATURED",
                    color = SecureMintAccent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = featuredMovie?.title ?: "Nebula Protocol",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = if (featuredMovie != null) "Premium Protected Stream • 4K UHD" else "Sci-Fi • Thriller • 2h 14m",
                style = MaterialTheme.typography.bodySmall,
                color = SecureTextGray
            )
        }
    }
}
