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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * Глитч забаненного: ник постоянно ломается. Символы на миг подменяются шумом,
 * красная и фиолетовая копии расходятся, строка дёргается.
 */
@Composable
fun BannedGlitchText(text: String, color: Color, fontSize: TextUnit, modifier: Modifier = Modifier) {
    var shown by remember(text) { androidx.compose.runtime.mutableStateOf(text) }
    var shift by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(text) {
        val junk = "▓▒░#%&@$!?"
        while (true) {
            delay(Random.nextLong(120, 420))
            shown = text.map { if (it != ' ' && Random.nextInt(4) == 0) junk[Random.nextInt(junk.length)] else it }.joinToString("")
            shift = Random.nextFloat() * 8f - 4f
            delay(Random.nextLong(60, 140))
            shown = text
            shift = 0f
        }
    }
    Box(modifier) {
        Text(shown, color = Red.copy(alpha = 0.85f), fontSize = fontSize, modifier = Modifier.offset(x = shift.dp))
        Text(shown, color = Violet.copy(alpha = 0.85f), fontSize = fontSize, modifier = Modifier.offset(x = (-shift).dp))
        Text(shown, color = color, fontSize = fontSize)
    }
}

/** Глитч картинки забаненного: дрожание, красноватый налёт и рваные полосы. */
fun Modifier.bannedGlitch(): Modifier = composed {
    var shift by remember { mutableFloatStateOf(0f) }
    var seed by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(150, 500))
            repeat(Random.nextInt(2, 5)) {
                shift = Random.nextFloat() * 10f - 5f
                seed = Random.nextInt(1, 1_000_000)
                delay(Random.nextLong(40, 90))
            }
            shift = 0f
            seed = 0
        }
    }
    this
        .graphicsLayer { translationX = shift * density }
        .drawWithContent {
            drawContent()
            drawRect(Red.copy(alpha = 0.10f))
            if (seed != 0) {
                val r = Random(seed)
                repeat(4 + r.nextInt(5)) {
                    val y = r.nextFloat() * size.height
                    val h = 2f + r.nextFloat() * size.height * 0.06f
                    drawRect(
                        (if (r.nextBoolean()) Red else Violet).copy(alpha = 0.55f),
                        topLeft = Offset(r.nextFloat() * size.width * 0.3f, y),
                        size = Size(size.width * (0.4f + r.nextFloat() * 0.6f), h),
                    )
                }
            }
        }
}
