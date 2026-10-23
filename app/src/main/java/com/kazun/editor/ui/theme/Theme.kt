package com.kazun.editor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val KazunRed = Color(0xFFFF4D5A)
val Surface0 = Color(0xFF0B0B0F)
val Surface1 = Color(0xFF14141B)
val Surface2 = Color(0xFF1D1D26)
val Surface3 = Color(0xFF262633)
val TextPrimary = Color(0xFFF2F2F7)
val TextSecondary = Color(0xFF9A9AA8)
val AccentTeal = Color(0xFF33D6C0)
val AccentAmber = Color(0xFFFFC24B)

private val DarkColors = darkColorScheme(
    primary = KazunRed,
    onPrimary = Color.White,
    secondary = AccentTeal,
    onSecondary = Color.Black,
    tertiary = AccentAmber,
    background = Surface0,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Surface3
)

private val KazunTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun KazunTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(colorScheme = DarkColors, typography = KazunTypography, content = content)
}
