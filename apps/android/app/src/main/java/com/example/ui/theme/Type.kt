package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val InterFamily = FontFamily.SansSerif

private fun secureTextStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0
) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.toFloat().sp
)

val Typography = Typography(
    displayLarge = secureTextStyle(FontWeight.Bold, 42, 48, -0.5),
    displayMedium = secureTextStyle(FontWeight.Bold, 36, 42, -0.3),
    displaySmall = secureTextStyle(FontWeight.Bold, 30, 36, -0.2),
    headlineLarge = secureTextStyle(FontWeight.Bold, 28, 34, -0.1),
    headlineMedium = secureTextStyle(FontWeight.Bold, 24, 30),
    headlineSmall = secureTextStyle(FontWeight.SemiBold, 22, 28),
    titleLarge = secureTextStyle(FontWeight.Bold, 20, 26),
    titleMedium = secureTextStyle(FontWeight.SemiBold, 16, 22, 0.1),
    titleSmall = secureTextStyle(FontWeight.SemiBold, 14, 20, 0.1),
    bodyLarge = secureTextStyle(FontWeight.Normal, 16, 24, 0.1),
    bodyMedium = secureTextStyle(FontWeight.Normal, 14, 20, 0.1),
    bodySmall = secureTextStyle(FontWeight.Normal, 12, 18, 0.2),
    labelLarge = secureTextStyle(FontWeight.SemiBold, 14, 20, 0.1),
    labelMedium = secureTextStyle(FontWeight.Medium, 12, 16, 0.4),
    labelSmall = secureTextStyle(FontWeight.Medium, 11, 14, 0.5)
)
