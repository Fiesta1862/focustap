package com.focustap.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Canvas = Color(0xFFFAF8F3)
val Charcoal = Color(0xFF1C1C1E)
val Accent = Color(0xFFC85A3C)
val Muted = Color(0xFF8A8781)
val Surface = Color(0xFFFFFFFF)
val Line = Color(0xFFE6E2DA)
val Success = Color(0xFF3E7A5E)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun FocusTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            secondary = Charcoal,
            onSecondary = Canvas,
            background = Canvas,
            onBackground = Charcoal,
            surface = Surface,
            onSurface = Charcoal,
            surfaceVariant = Color(0xFFF1EEE7),
            onSurfaceVariant = Muted,
            outline = Line,
            error = Accent,
        ),
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
