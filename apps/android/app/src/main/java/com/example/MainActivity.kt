package com.example

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PlaybackScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.PlaybackState
import com.example.ui.viewmodel.SecureStreamViewModel

private const val ScreenTransitionMillis = 220

class MainActivity : ComponentActivity() {

    private val viewModel: SecureStreamViewModel by viewModels()
    private var isPlayingVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyVisibleSystemBars()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    SecureStreamApp(viewModel)
                }
            }
        }
    }

    fun enterFullscreenPlayback() {
        isPlayingVideo = true
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun exitFullscreenPlayback() {
        isPlayingVideo = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        applyVisibleSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isPlayingVideo) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else if (hasFocus) {
            applyVisibleSystemBars()
        }
    }

    private fun applyVisibleSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    fun enterProtectedPlayback() {
        enterFullscreenPlayback()
    }

    fun leaveProtectedPlayback() {
        exitFullscreenPlayback()
    }
}

@Composable
fun SecureStreamApp(viewModel: SecureStreamViewModel) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()
    val selectedMovie by viewModel.selectedMovie.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    // Listen to Auth State to handle top-level redirection
    androidx.compose.runtime.LaunchedEffect(authState) {
        if (authState is AuthState.Idle || authState is AuthState.Error) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else if (authState is AuthState.Success) {
            navController.navigate("catalog") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authState is AuthState.Success) "catalog" else "login",
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(ScreenTransitionMillis)
            ) + fadeIn(animationSpec = tween(ScreenTransitionMillis))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(ScreenTransitionMillis)
            ) + fadeOut(animationSpec = tween(ScreenTransitionMillis))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(ScreenTransitionMillis)
            ) + fadeIn(animationSpec = tween(ScreenTransitionMillis))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(ScreenTransitionMillis)
            ) + fadeOut(animationSpec = tween(ScreenTransitionMillis))
        }
    ) {
        composable("login") {
            LoginScreen(viewModel = viewModel)
        }

        composable("catalog") {
            CatalogScreen(
                viewModel = viewModel,
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToDownloads = { navController.navigate("downloads") },
                onNavigateToDetail = { movie ->
                    viewModel.selectMovie(movie)
                    navController.navigate("detail")
                },
                onNavigateToDashboard = { navController.navigate("dashboard") }
            )
        }

        composable("downloads") {
            DownloadsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("player") }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { movie ->
                    viewModel.selectMovie(movie)
                    navController.navigate("detail")
                }
            )
        }

        composable("detail") {
            val movie = selectedMovie
            if (movie != null) {
                DetailScreen(
                    viewModel = viewModel,
                    movie = movie,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = {
                        navController.navigate("player")
                    }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable("player") {
            val movie = selectedMovie
            val pState = playbackState
            if (movie != null && pState is PlaybackState.Success) {
                PlaybackScreen(
                    viewModel = viewModel,
                    videoId = movie.id,
                    title = movie.title,
                    playbackSession = pState.response,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // If direct entry or invalid state, pop back
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}
