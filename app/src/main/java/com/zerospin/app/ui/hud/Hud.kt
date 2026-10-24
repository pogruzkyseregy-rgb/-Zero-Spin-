package com.zerospin.app.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.R
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.FluoriteColors
import com.zerospin.app.ui.theme.Mint
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import com.zerospin.app.ui.theme.fluoriteBrush
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TAU = (2 * PI).toFloat()

/** Моноширинный шрифт для HUD-подписей. */
val HudMono = FontFamily.Monospace

// ---------- Геометрия: панели со срезанными углами ----------

private fun chamferPath(size: Size, cut: Float): Path = Path().apply {
    moveTo(cut, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(0f, size.height)
    lineTo(0f, cut)
    close()
}

/**
 * Панель со срезанными углами и тонкой неоновой линией.
 * glow — мягкое свечение вокруг линии, brush — заливка градиентом вместо цвета.
 */
fun Modifier.hudPanel(
    line: Color = Cyan,
    fill: Color = Panel,
    cut: Dp = 12.dp,
    glow: Boolean = false,
    brush: Brush? = null,
): Modifier =
    this.drawBehind {
        val path = chamferPath(size, cut.toPx())
        if (brush != null) drawPath(path, brush) else drawPath(path, fill.copy(alpha = fill.alpha * 0.92f))
        if (glow) drawPath(path, line.copy(alpha = 0.18f), style = Stroke(width = 6.dp.toPx()))
        drawPath(path, line.copy(alpha = 0.55f), style = Stroke(width = 1.dp.toPx()))
        // Короткая яркая засечка на срезе: «неоновый» штрих, как на HUD
        val c = cut.toPx()
        drawLine(line, Offset(0f, c), Offset(c, 0f), strokeWidth = 1.6.dp.toPx())
    }

// ---------- Фон: квантовое поле ----------

private fun frac(x: Float): Float = x - floor(x)

/** Время для фона: от 0 до 1 за полминуты, по кругу. */
@Composable
private fun rememberQuantumTime(): State<Float> {
    val transition = rememberInfiniteTransition(label = "quantum")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing)),
        label = "time",
    )
}

/**
 * Фон экрана: глубокая пустота, мягкие свечения флюорита, волны интерференции
 * и частицы, медленно всплывающие вверх. animated = false — неподвижная картинка.
 */
@Composable
fun HudBackground(animated: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val time: State<Float> = if (animated) rememberQuantumTime() else remember { mutableFloatStateOf(0.2f) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                val p = time.value
                val w = size.width
                val h = size.height

                // Свечения: фиолетовое сверху справа и циановое снизу слева
                drawRect(
                    Brush.radialGradient(
                        listOf(Violet.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(w * (0.85f + 0.06f * sin(p * TAU)), h * 0.06f),
                        radius = w * 0.95f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Cyan.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(w * 0.1f, h * (0.92f + 0.03f * cos(p * TAU))),
                        radius = w * 0.85f,
                    ),
                )

                // Редкая сетка
                val step = 44.dp.toPx()
                val grid = Violet.copy(alpha = 0.035f)
                var gx = 0f
                while (gx <= w) {
                    drawLine(grid, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                    gx += step
                }
                var gy = 0f
                while (gy <= h) {
                    drawLine(grid, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                    gy += step
                }

                // Волны интерференции: три волновые функции разных частот
                val dx = 10.dp.toPx()
                for (k in 0 until 3) {
                    val baseY = h * (0.22f + 0.28f * k)
                    val amp = (14f + 8f * k).dp.toPx()
                    val freq = 1.2f + 0.6f * k
                    val dir = if (k % 2 == 0) 1f else -1f
                    val path = Path()
                    var x = 0f
                    while (x <= w + dx) {
                        val u = x / w
                        val envelope = 0.6f + 0.4f * sin(u * TAU * 0.5f + p * TAU)
                        val y = baseY + amp * envelope * sin(u * TAU * freq + dir * p * TAU * (k + 1))
                        if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                        x += dx
                    }
                    drawPath(path, FluoriteColors[k].copy(alpha = 0.09f), style = Stroke(width = 1.2.dp.toPx()))
                }

                // Частицы: всплывают вверх и мерцают
                for (i in 0 until 26) {
                    val seedX = frac(sin(i * 12.9898f) * 43758.547f)
                    val seedS = frac(sin(i * 78.233f) * 12345.679f)
                    val speed = if (i % 3 == 0) 2f else 1f
                    val y = h * (1f - frac(seedS + p * speed))
                    val x = w * seedX + 8.dp.toPx() * sin(p * TAU * 2f + i)
                    val twinkle = 0.25f + 0.75f * abs(sin(p * TAU * 3f + i))
                    drawCircle(
                        FluoriteColors[i % 3].copy(alpha = 0.4f * twinkle),
                        radius = (0.8f + seedS * 1.6f).dp.toPx(),
                        center = Offset(x, y),
                    )
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
            .defaultMinSize(minHeight = 46.dp)
            .hudPanel(
                line = Cyan,
                cut = 10.dp,
                fill = Panel,
                glow = primary,
                brush = if (primary) fluoriteBrush() else null,
            )
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

/** Сегментированная полоса: сегменты окрашены по полосам флюорита. */
@Composable
fun SegmentedBar(progress: Float, modifier: Modifier = Modifier, segments: Int = 14, color: Color = Cyan) {
    val lit = (progress.coerceIn(0f, 1f) * segments).roundToInt()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { i ->
            val c = if (color == Cyan) FluoriteColors[(i * 3) / segments] else color
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(if (i < lit) c else c.copy(alpha = 0.12f)),
            )
        }
    }
}

/**
 * Кольцо-прицел. ears = true дорисовывает лисьи ушки сверху — знак Авроры.
 * Внутри можно разместить содержимое (инициал, фото).
 */
@Composable
fun TargetRing(
    modifier: Modifier = Modifier,
    color: Color = Cyan,
    ears: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.drawBehind {
            val r = size.minDimension / 2f
            val c = center
            val w = 1.dp.toPx()
            drawCircle(color.copy(alpha = 0.12f), radius = r, center = c, style = Stroke(width = 5.dp.toPx()))
            drawCircle(color, radius = r - w, center = c, style = Stroke(width = w))
            drawCircle(
                Violet.copy(alpha = 0.6f),
                radius = r * 0.72f,
                center = c,
                style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
            )
            val tick = w * 1.5f
            drawLine(color, Offset(c.x - r, c.y), Offset(c.x - r * 0.8f, c.y), strokeWidth = tick)
            drawLine(color, Offset(c.x + r, c.y), Offset(c.x + r * 0.8f, c.y), strokeWidth = tick)
            drawLine(color, Offset(c.x, c.y + r), Offset(c.x, c.y + r * 0.8f), strokeWidth = tick)
            if (ears) {
                // Два треугольных уха над кольцом, слегка наклонены наружу
                for (side in listOf(-1f, 1f)) {
                    val baseX = c.x + side * r * 0.55f
                    val baseY = c.y - r * 0.78f
                    val ear = Path().apply {
                        moveTo(baseX - r * 0.28f, baseY + r * 0.1f)
                        lineTo(baseX + side * r * 0.18f, baseY - r * 0.55f)
                        lineTo(baseX + r * 0.28f, baseY + r * 0.1f)
                    }
                    drawPath(ear, Violet.copy(alpha = 0.25f))
                    drawPath(ear, color, style = Stroke(width = w * 1.3f))
                }
            } else {
                drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y - r * 0.8f), strokeWidth = tick)
            }
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Квантовый «атом»: три орбиты под углом и электроны на них. Для пустых экранов и загрузки. */
@Composable
fun QuantumAtom(modifier: Modifier = Modifier.size(84.dp)) {
    val transition = rememberInfiniteTransition(label = "atom")
    val spin = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "spin",
    )
    Canvas(modifier) {
        val c = center
        val rx = size.minDimension * 0.46f
        val ry = rx * 0.36f
        val p = spin.value
        for (k in 0 until 3) {
            rotate(degrees = k * 60f + p * 40f, pivot = c) {
                drawOval(
                    FluoriteColors[k].copy(alpha = 0.7f),
                    topLeft = Offset(c.x - rx, c.y - ry),
                    size = Size(rx * 2, ry * 2),
                    style = Stroke(width = 1.2.dp.toPx()),
                )
                val a = (p * (k + 1) + k / 3f) * TAU
                drawCircle(
                    FluoriteColors[k],
                    radius = 2.6.dp.toPx(),
                    center = Offset(c.x + rx * cos(a), c.y + ry * sin(a)),
                )
            }
        }
        drawCircle(Brush.radialGradient(listOf(Cyan, Violet.copy(alpha = 0f)), center = c, radius = 10.dp.toPx()), radius = 10.dp.toPx(), center = c)
    }
}

/** Пульсирующая точка статуса (в сети, печатает). */
@Composable
fun PulseDot(color: Color = Mint, modifier: Modifier = Modifier.size(8.dp)) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val a = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "a",
    )
    Canvas(modifier) {
        drawCircle(color.copy(alpha = 0.25f * a.value), radius = size.minDimension / 2f)
        drawCircle(color.copy(alpha = a.value), radius = size.minDimension / 4f)
    }
}

enum class HudIconKind { FEED, CHAT, APPS, PROFILE, RING, PLUS, SEND, BACK, SEARCH }

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
            HudIconKind.SEARCH -> {
                drawCircle(color, radius = w * 0.3f, center = Offset(w * 0.42f, h * 0.42f), style = Stroke(width = s))
                drawLine(color, Offset(w * 0.64f, h * 0.64f), Offset(w * 0.95f, h * 0.95f), strokeWidth = s * 1.3f)
            }
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
                Modifier.size(width = 52.dp, height = 40.dp).hudPanel(cut = 9.dp, glow = true),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot(Mint, Modifier.size(8.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(status, color = Muted, fontSize = 10.sp, letterSpacing = 1.sp, fontFamily = HudMono)
                }
            }
            // Настройки: кольцо с лисьими ушками — знак Авроры
            TargetRing(Modifier.size(42.dp).clickable(onClick = onSettings), ears = true) {
                HudIcon(HudIconKind.RING, Cyan, Modifier.size(16.dp))
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Violet, Cyan, Mint, Color.Transparent))),
        )
    }
}

/**
 * Нижняя панель: активная вкладка подсвечена градиентом флюорита и свечением под иконкой.
 * badges — число на значке вкладки (например, непрочитанные в чатах); 0 — без значка.
 */
@Composable
fun HudTabBar(tabs: List<HudTab>, selected: Int, onSelect: (Int) -> Unit, badges: List<Int> = emptyList()) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .hudPanel(cut = 14.dp, fill = Panel)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val active = index == selected
            val color = if (active) Cyan else Muted
            val badge = badges.getOrElse(index) { 0 }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(30.dp)
                        .height(2.dp)
                        .background(if (active) fluoriteBrush() else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))),
                )
                Spacer(Modifier.height(6.dp))
                Box(contentAlignment = Alignment.Center) {
                    if (active) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.25f), Color.Transparent))),
                        )
                    }
                    HudIcon(tab.icon, color, Modifier.size(22.dp))
                    if (badge > 0) {
                        Text(
                            if (badge > 99) "99+" else badge.toString(),
                            color = Bg,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 10.dp, y = (-4).dp)
                                .background(Mint)
                                .padding(horizontal = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tab.title,
                    color = color,
                    fontSize = 9.sp,
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
        QuantumAtom(Modifier.size(84.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

/** Цвет предупреждения — оставлен для старых экранов. */
val HudWarn = Red
