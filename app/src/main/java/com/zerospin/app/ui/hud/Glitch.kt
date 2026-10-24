package com.zerospin.app.ui.hud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Текст с редкими глитчами: сдвиг красной и фиолетовой копий.
 * Глитч случается раз в несколько секунд на короткое время, без постоянного мерцания.
 */
@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Cyan,
    fontSize: TextUnit = 14.sp,
    enabled: Boolean = true,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    var shift by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            shift = 0f
            return@LaunchedEffect
        }
        while (true) {
            delay(Random.nextLong(3000, 8000))
            repeat(Random.nextInt(2, 5)) {
                shift = Random.nextFloat() * 6f - 3f
                delay(Random.nextLong(40, 90))
            }
            shift = 0f
        }
    }

    val style = TextStyle(fontWeight = fontWeight, letterSpacing = letterSpacing, fontSize = fontSize)

    Box(modifier) {
        if (enabled && shift != 0f) {
            Text(text, color = Red.copy(alpha = 0.8f), style = style, modifier = Modifier.offset(x = shift.dp))
            Text(text, color = Violet.copy(alpha = 0.8f), style = style, modifier = Modifier.offset(x = -shift.dp))
        }
        Text(text, color = color, style = style)
    }
}
