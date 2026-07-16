package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SecureBrandAccent
import com.example.ui.theme.SecureDarkGray

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val transition = rememberInfiniteTransition(label = "securestream_shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -350f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "securestream_shimmer_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SecureDarkGray.copy(alpha = 0.72f),
                        SecureBrandAccent.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.08f),
                        SecureDarkGray.copy(alpha = 0.72f)
                    ),
                    start = Offset(shimmerOffset, shimmerOffset),
                    end = Offset(shimmerOffset + 420f, shimmerOffset + 420f)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    )
}
