package com.zerospin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Палитра в стиле Авроры: тёмно-синий фон, неоновый циан, фиолетовый и розовый акценты
val Bg = Color(0xFF0B1126)
val CardBg = Color(0xFF141E3C)
val Cyan = Color(0xFF33E1F5)
val Violet = Color(0xFF8B6CFF)
val Pink = Color(0xFFFF5DA2)
val Muted = Color(0xFF7F8BB0)
val TextMain = Color(0xFFE6EEFF)

/** Градиентная линия под шапкой и над нижней панелью */
val AuroraBrush = Brush.horizontalGradient(listOf(Cyan, Violet, Pink))

private val ZeroSpinScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Bg,
    secondary = Violet,
    tertiary = Pink,
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
