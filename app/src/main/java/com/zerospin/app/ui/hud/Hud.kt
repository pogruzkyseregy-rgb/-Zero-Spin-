package com.zerospin.app.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.R
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import kotlin.math.roundToInt

// ---------- Геометрия: панели со срезанными углами, как в HUD-референсах ----------

private fun chamferPath(size: Size, cut: Float): Path = Path().apply {
    moveTo(cut, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(0f, size.height)
    lineTo(0f, cut)
    close()
}

/** Панель со срезанным углом и тонкой светящейся линией. */
fun Modifier.hudPanel(line: Color = Cyan, fill: Color = Panel, cut: Dp = 12.dp): Modifier =
    this.drawBehind {
        val path = chamferPath(size, cut.toPx())
        drawPath(path, fill)
        drawPath(path, line.copy(alpha = 0.7f), style = Stroke(width = 1.dp.toPx()))
    }

/** Фон экрана: тёмная сетка, как на HUD-референсах. */
@Composable
fun HudBackground(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                val step = 36.dp.toPx()
                val grid = Cyan.copy(alpha = 0.06f)
                var x = 0f
                while (x <= size.width) {
                    drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            },
        content = content,
    )
}

// ---------- Кнопки и поля ----------

@Composable
fun HudButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .hudPanel(cut = 10.dp, fill = if (primary) Cyan else Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = if (primary) Bg else Cyan,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
fun hudFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    focusedBorderColor = Cyan,
    unfocusedBorderColor = Cyan.copy(alpha = 0.3f),
    cursorColor = Cyan,
    focusedContainerColor = Bg,
    unfocusedContainerColor = Bg,
)

// ---------- Элементы индикации ----------

/** Сегментированная полоса, как индикатор загрузки на референсах. */
@Composable
fun SegmentedBar(progress: Float, modifier: Modifier = Modifier, segments: Int = 14, color: Color = Cyan) {
    val lit = (progress.coerceIn(0f, 1f) * segments).roundToInt()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(if (i < lit) color else color.copy(alpha = 0.12f)),
            )
        }
    }
}

/** Кольцо-прицел из HUD-референсов. Внутри можно разместить содержимое (например, инициал). */
@Composable
fun TargetRing(
    modifier: Modifier = Modifier,
    color: Color = Cyan,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.drawBehind {
            val r = size.minDimension / 2f
            val c = center
            val w = 1.dp.toPx()
            drawCircle(color, radius = r - w, center = c, style = Stroke(width = w))
            drawCircle(
                color.copy(alpha = 0.5f),
                radius = r * 0.7f,
                center = c,
                style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
            )
            val tick = w * 1.5f
            drawLine(color, Offset(c.x - r, c.y), Offset(c.x - r * 0.8f, c.y), strokeWidth = tick)
            drawLine(color, Offset(c.x + r, c.y), Offset(c.x + r * 0.8f, c.y), strokeWidth = tick)
            drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y - r * 0.8f), strokeWidth = tick)
            drawLine(color, Offset(c.x, c.y + r), Offset(c.x, c.y + r * 0.8f), strokeWidth = tick)
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

enum class HudIconKind { FEED, CHAT, APPS, PROFILE, RING, PLUS, SEND, BACK }

/** Иконки в линию, нарисованные вручную в стиле HUD. */
@Composable
fun HudIcon(kind: HudIconKind, color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val s = 1.6.dp.toPx()
        when (kind) {
            HudIconKind.FEED -> {
                drawLine(color, Offset(0f, h * 0.2f), Offset(w, h * 0.2f), strokeWidth = s)
                drawLine(color, Offset(0f, h * 0.5f), Offset(w * 0.7f, h * 0.5f), strokeWidth = s)
                drawLine(color, Offset(0f, h * 0.8f), Offset(w * 0.45f, h * 0.8f), strokeWidth = s)
            }
            HudIconKind.CHAT -> drawPath(chamferPath(size, w * 0.28f), color, style = Stroke(width = s))
            HudIconKind.APPS -> {
                val g = w * 0.42f
                for (x in listOf(0f, w - g)) {
                    for (y in listOf(0f, h - g)) {
                        drawRect(color, Offset(x, y), Size(g, g), style = Stroke(width = s))
                    }
                }
            }
            HudIconKind.PROFILE -> {
                drawCircle(color, radius = w * 0.18f, center = Offset(w / 2f, h * 0.33f), style = Stroke(width = s))
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.6f),
                    size = Size(w * 0.64f, h * 0.5f),
                    style = Stroke(width = s),
                )
            }
            HudIconKind.RING -> {
                drawCircle(color, radius = w * 0.44f, center = Offset(w / 2f, h / 2f), style = Stroke(width = s))
                drawCircle(color, radius = w * 0.1f, center = Offset(w / 2f, h / 2f))
                drawLine(color, Offset(w / 2f, 0f), Offset(w / 2f, h * 0.14f), strokeWidth = s)
                drawLine(color, Offset(w / 2f, h), Offset(w / 2f, h * 0.86f), strokeWidth = s)
                drawLine(color, Offset(0f, h / 2f), Offset(w * 0.14f, h / 2f), strokeWidth = s)
                drawLine(color, Offset(w, h / 2f), Offset(w * 0.86f, h / 2f), strokeWidth = s)
            }
            HudIconKind.PLUS -> {
                drawLine(color, Offset(w / 2f, h * 0.15f), Offset(w / 2f, h * 0.85f), strokeWidth = s)
                drawLine(color, Offset(w * 0.15f, h / 2f), Offset(w * 0.85f, h / 2f), strokeWidth = s)
            }
            HudIconKind.SEND -> drawPath(
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, h / 2f)
                    lineTo(0f, h)
                    lineTo(w * 0.25f, h / 2f)
                    close()
                },
                color,
            )
            HudIconKind.BACK -> drawPath(
                Path().apply {
                    moveTo(w * 0.7f, h * 0.15f)
                    lineTo(w * 0.3f, h / 2f)
                    lineTo(w * 0.7f, h * 0.85f)
                },
                color,
                style = Stroke(width = s),
            )
        }
    }
}

// ---------- Шапка и нижняя панель ----------

data class HudTab(val title: String, val icon: HudIconKind)

@Composable
fun HudTopBar(title: String, status: String, onSettings: () -> Unit, glitch: Boolean = false) {
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(width = 52.dp, height = 40.dp).hudPanel(cut = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_av),
                    contentDescription = "AV",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 34.dp, height = 24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                GlitchText(
                    text = title,
                    color = Cyan,
                    fontSize = 17.sp,
                    enabled = glitch,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text(status, color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            TargetRing(Modifier.size(42.dp).clickable(onClick = onSettings)) {
                HudIcon(HudIconKind.RING, Cyan, Modifier.size(16.dp))
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Cyan, Cyan.copy(alpha = 0f)))),
        )
    }
}

@Composable
fun HudTabBar(tabs: List<HudTab>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .hudPanel(cut = 14.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val active = index == selected
            val color = if (active) Cyan else Muted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(28.dp).height(2.dp).background(if (active) Cyan else Color.Transparent))
                Spacer(Modifier.height(6.dp))
                HudIcon(tab.icon, color, Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tab.title,
                    color = color,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ---------- Пустые состояния ----------

@Composable
fun HudEmptyState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TargetRing(Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}
