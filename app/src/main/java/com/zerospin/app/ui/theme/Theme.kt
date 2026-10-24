package com.zerospin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Палитра HUD-интерфейса Авроры: глубокий чёрно-синий фон, холодный циан, фиолетовый и красный для сигналов
val Bg = Color(0xFF05080F)
val Panel = Color(0xFF0A1322)
val CardBg = Panel
val Cyan = Color(0xFF22E6FF)
val Violet = Color(0xFFA855FF)
val Red = Color(0xFFFF3B5C)
val Muted = Color(0xFF5E7C8C)
val TextMain = Color(0xFFD9F6FF)

private val ZeroSpinScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Bg,
    secondary = Violet,
    error = Red,
    background = Bg,
    onBackground = TextMain,
    surface = Panel,
    onSurface = TextMain,
    surfaceVariant = Panel,
    onSurfaceVariant = Muted,
)

@Composable
fun ZeroSpinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ZeroSpinScheme, content = content)
}
