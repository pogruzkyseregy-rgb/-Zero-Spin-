package com.zerospin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Палитра Авроры: квантовая пустота, неон и полосы флюорита (фиолетовый → циан → мятный)
val Bg = Color(0xFF06040E)
val Panel = Color(0xFF0F0B1F)
val PanelHi = Color(0xFF171030)
val CardBg = Panel
val Cyan = Color(0xFF3CF2FF)
val Violet = Color(0xFFA970FF)
/** Зелёная полоса флюорита */
val Mint = Color(0xFF4DFFC3)
/** Неоновый розовый для редких акцентов */
val Magenta = Color(0xFFFF5BD8)
val Red = Color(0xFFFF4D6D)
val Muted = Color(0xFF7D7FA6)
val TextMain = Color(0xFFEAF2FF)

/** Цвета флюорита по порядку полос. */
val FluoriteColors = listOf(Violet, Cyan, Mint)

/** Градиент флюорита слева направо. */
fun fluoriteBrush(alpha: Float = 1f): Brush =
    Brush.linearGradient(FluoriteColors.map { it.copy(alpha = alpha) })

private val ZeroSpinScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Bg,
    secondary = Violet,
    tertiary = Mint,
    error = Red,
    background = Bg,
    onBackground = TextMain,
    surface = Panel,
    onSurface = TextMain,
    surfaceVariant = PanelHi,
    onSurfaceVariant = Muted,
)

@Composable
fun ZeroSpinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ZeroSpinScheme, content = content)
}
