package com.zerospin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0E1426)
val CardBg = Color(0xFF16203D)
val Cyan = Color(0xFF3FE0F0)
val Violet = Color(0xFF7B6CF6)
val Muted = Color(0xFF8A96B8)
val TextMain = Color(0xFFE6EEFF)

private val ZeroSpinScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Bg,
    secondary = Violet,
    background = Bg,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
    surfaceVariant = CardBg,
    onSurfaceVariant = Muted,
)

@Composable
fun ZeroSpinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ZeroSpinScheme, content = content)
}
