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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.CoinStore
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import com.zerospin.app.widget.CoinWidget
import kotlin.math.abs
import kotlin.math.roundToInt

/** Монетка Zero Spin. Счёт и история общие с виджетом на рабочем столе. */
@Composable
fun CoinScreen() {
    val context = LocalContext.current

    // Меняется после каждого броска или сброса, чтобы перечитать счёт из хранилища
    var revision by rememberSaveable { mutableIntStateOf(0) }
    var rotTarget by remember { mutableFloatStateOf(0f) }

    val rotation by animateFloatAsState(
        targetValue = rotTarget,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "coin",
    )
    val spinning = abs(rotation - rotTarget) > 0.5f

    val side = remember(revision) { CoinStore.last(context) }
    val countA = remember(revision) { CoinStore.countA(context) }
    val countB = remember(revision) { CoinStore.countB(context) }
    val history = remember(revision) { CoinStore.history(context) }
    val total = countA + countB

    fun toss() {
        if (spinning) return
        val from = CoinStore.last(context)
        val result = CoinStore.toss()
        // Полуобороты: 10 или 12, плюс один, если сторона должна смениться
        var halfTurns = 10 + 2 * kotlin.random.Random.nextInt(2)
        if (result != from) halfTurns += 1
        rotTarget += halfTurns * 180f
        CoinStore.record(context, result)
        CoinWidget.refreshAll(context)
        revision++
    }

    fun reset() {
        if (spinning) return
        CoinStore.reset(context)
        CoinWidget.refreshAll(context)
        revision++
    }

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
                        side == CoinStore.SIDE_A -> "AVRORA"
                        else -> "ZERO\nSPIN"
                    },
                    color = if (side == CoinStore.SIDE_A) Cyan else Violet,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = if (CoinStore.hasFlipped(context)) {
                "Последний бросок: ${if (side == CoinStore.SIDE_A) "AvrorA" else "Zero Spin"}"
            } else {
                "Нажмите монетку или кнопку ниже"
            },
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            history.forEach { c ->
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (c == 'a') Cyan else Violet),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Последние ${CoinStore.HISTORY_SIZE} бросков · счёт общий с виджетом",
            color = Muted,
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "СБРОСИТЬ СЧЁТ",
            color = Muted,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.clickable { reset() }.padding(8.dp),
        )
    }
}

@Composable
private fun CountCell(label: String, count: Int, total: Int, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(count.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(
            if (total == 0) "—" else "${(count * 100f / total).roundToInt()}%",
            color = Muted,
            fontSize = 11.sp,
        )
    }
}
