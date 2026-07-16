package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SecureColorScheme = darkColorScheme(
    primary = SecureBrandAccent,
    onPrimary = SecureTextWhite,
    primaryContainer = SecureBrandAccentContainer,
    onPrimaryContainer = SecureTextWhite,
    secondary = SecureBrandAccentSoft,
    onSecondary = SecureDarkBackground,
    secondaryContainer = SecureBrandAccentContainer,
    onSecondaryContainer = SecureTextWhite,
    background = SecureDarkBackground,
    onBackground = SecureTextWhite,
    surface = SecureDarkSurface,
    onSurface = SecureTextWhite,
    surfaceVariant = SecureDarkGray,
    onSurfaceVariant = SecureTextWhite,
    outline = SecureTextGray
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SecureColorScheme,
        typography = Typography,
        content = content
    )
}
