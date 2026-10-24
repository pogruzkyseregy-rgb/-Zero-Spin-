package com.zerospin.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import kotlin.math.abs
import kotlin.random.Random

private const val SIDE_AVRORA = 0
private const val SIDE_ZERO = 1
private const val HISTORY_LIMIT = 20

/** Монетка Zero Spin: подброс, счётчики и история. Данные живут, пока открыто приложение. */
@Composable
fun CoinScreen() {
    var side by rememberSaveable { mutableIntStateOf(SIDE_AVRORA) }
    var history by rememberSaveable { mutableStateOf("") }
    var rotTarget by remember { mutableFloatStateOf(0f) }

    val rotation by animateFloatAsState(
        targetValue = rotTarget,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "coin",
    )
    val spinning = abs(rotation - rotTarget) > 0.5f

    fun toss() {
        if (spinning) return
        val result = Random.nextInt(2)
        // Половины оборота: 10 или 12 плюс один, если сторона должна смениться
        var halfTurns = 10 + 2 * Random.nextInt(2)
        if (result != side) halfTurns += 1
        rotTarget += halfTurns * 180f
        side = result
        history = (history + if (result == SIDE_AVRORA) "a" else "b").takeLast(HISTORY_LIMIT)
    }

    val countA = history.count { it == 'a' }
    val countB = history.count { it == 'b' }
    val total = countA + countB

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .size(220.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 16f * density
                }
                .clickable { toss() },
            contentAlignment = Alignment.Center,
        ) {
            TargetRing(Modifier.size(220.dp)) {
                Text(
                    text = when {
                        spinning -> "…"
                        side == SIDE_AVRORA -> "AVRORA"
                        else -> "ZERO\nSPIN"
                    },
                    color = if (side == SIDE_AVRORA) Cyan else Violet,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = if (history.isEmpty()) "Нажмите монетку или кнопку ниже" else "Последний бросок: ${if (side == SIDE_AVRORA) "AvrorA" else "Zero Spin"}",
            color = TextMain,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(16.dp))

        HudButton(text = "Подбросить", onClick = { toss() }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp)) {
            CountCell("AVRORA", countA, total, Cyan, Modifier.weight(1f))
            CountCell("ZERO SPIN", countB, total, Violet, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            history.forEach { c ->
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (c == 'a') Cyan else Violet),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Счётчики за текущий сеанс, до $HISTORY_LIMIT последних бросков",
            color = Muted,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun CountCell(
    label: String,
    count: Int,
    total: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(count.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(
            if (total == 0) "—" else "${Math.round(count * 100f / total)}%",
            color = Muted,
            fontSize = 11.sp,
        )
    }
}
