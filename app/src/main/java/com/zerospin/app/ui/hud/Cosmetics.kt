package com.zerospin.app.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.drawscope.translate
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Magenta
import com.zerospin.app.ui.theme.Mint
import com.zerospin.app.ui.theme.Violet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Украшения аватара для всех. Лисьих ушек тут нет: они только у Авроры.
 * id совпадают с сервером (zs_core.COSMETICS).
 */
val AvatarCosmetics: List<Pair<String, String>> = listOf(
    "" to "Без украшения",
    "halo" to "Нимб ангела",
    "demon" to "Нимб демона",
    "orbit" to "Орбита",
    "hat" to "Шляпа",
    "crown" to "Неоновая корона",
    "stars" to "Звёздочки",
    "sparkle" to "Искра",
    "comet" to "Комета",
    "crown2" to "Острая корона",
    "starcrown" to "Звёздный венец",
    "thornhalo" to "Терновый нимб",
    "thorns" to "Терновник",
    "eclipse" to "Затмение",
    "radiation" to "Радиация",
    "dna" to "ДНК",
    "claws" to "Когти",
    "glitch404" to "404",
    "loser" to "Loser / Lover",
)

/** Украшения, которые рисуются позади аватара (нимб из шипов, затмение). */
private val BackCosmetics = setOf("thornhalo", "eclipse")

private val HaloGold = Color(0xFFFFF0A8)
private val DemonRed = Color(0xFFFF2E4D)

/**
 * Украшение поверх круглого аватара размером size. Рисуется в тех же границах,
 * но может немного выходить вверх (нимб, корона, шляпа) — как ушки у Авроры.
 */
@Composable
fun CosmeticOverlay(kind: String, size: Int, modifier: Modifier = Modifier) {
    if (kind.isBlank() || kind in BackCosmetics || AvatarCosmetics.none { it.first == kind }) return
    val t = rememberInfiniteTransition(label = "cosmetic")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "phase")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bob")
    val text = rememberTextMeasurer()
    Canvas(modifier.size(size.dp)) {
        when (kind) {
            "halo" -> halo(bob, HaloGold, Color(0xFFFFFFFF))
            "demon" -> demon(bob)
            "orbit" -> orbit(phase)
            "hat" -> hat()
            "crown" -> crown(bob)
            "stars" -> stars(phase)
            "sparkle" -> sparkleCluster(bob)
            "comet" -> comet(phase)
            "crown2" -> sharpCrown(bob)
            "starcrown" -> starCrown(phase)
            "thorns" -> thornVine()
            "radiation" -> radiation(phase)
            "dna" -> dna(phase)
            "claws" -> claws(bob)
            "glitch404" -> glitch404(text, phase)
            "loser" -> loser(text, bob)
        }
    }
}

/** Задний слой: рисуется до кольца аватара, чтобы оказаться за ним. */
@Composable
fun CosmeticBack(kind: String, size: Int, modifier: Modifier = Modifier) {
    if (kind !in BackCosmetics) return
    val t = rememberInfiniteTransition(label = "cosmeticBack")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "phase")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "bob")
    Canvas(modifier.size(size.dp)) {
        when (kind) {
            "thornhalo" -> thornHalo(phase, bob)
            "eclipse" -> eclipse(bob)
        }
    }
}

private fun DrawScope.halo(bob: Float, color: Color, core: Color) {
    val s = size.width
    val cy = -s * 0.04f - bob * s * 0.03f
    val w = s * 0.58f
    val h = s * 0.15f
    val tl = Offset(s / 2 - w / 2, cy - h / 2)
    drawOval(color.copy(alpha = 0.25f), tl, Size(w, h), style = Stroke(width = s * 0.09f))
    drawOval(color, tl, Size(w, h), style = Stroke(width = s * 0.035f))
    drawOval(core.copy(alpha = 0.8f), tl, Size(w, h), style = Stroke(width = s * 0.012f))
}

private fun DrawScope.demon(bob: Float) {
    val s = size.width
    // Рожки по бокам макушки, острыми концами наружу
    for (side in listOf(-1f, 1f)) {
        val bx = s / 2 + side * s * 0.25f
        val by = s * 0.12f
        val horn = Path().apply {
            moveTo(bx - side * s * 0.08f, by + s * 0.03f)
            quadraticBezierTo(bx + side * s * 0.02f, by - s * 0.06f, bx + side * s * 0.10f, by - s * 0.17f)
            quadraticBezierTo(bx + side * s * 0.06f, by - s * 0.02f, bx + side * s * 0.07f, by + s * 0.05f)
            close()
        }
        drawPath(horn, Brush.verticalGradient(listOf(DemonRed, Color(0xFF7A0B22)), startY = by - s * 0.17f, endY = by + s * 0.05f))
        drawPath(horn, Magenta.copy(alpha = 0.7f), style = Stroke(width = s * 0.012f, join = StrokeJoin.Round))
    }
    halo(bob, DemonRed, Color(0xFFFF9FB0))
}

private fun DrawScope.orbit(phase: Float) {
    val s = size.width
    val c = Offset(s / 2, s / 2)
    val rx = s * 0.62f
    val ry = s * 0.2f
    rotate(-22f, c) {
        drawOval(Cyan.copy(alpha = 0.15f), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(width = s * 0.06f))
        drawOval(Cyan.copy(alpha = 0.7f), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(width = s * 0.014f))
        // Частица бежит по орбите
        val a = phase * 2 * PI
        val p = Offset(c.x + rx * cos(a).toFloat(), c.y + ry * sin(a).toFloat())
        drawCircle(Mint.copy(alpha = 0.35f), radius = s * 0.06f, center = p)
        drawCircle(Color.White, radius = s * 0.028f, center = p)
        val a2 = a + PI
        val p2 = Offset(c.x + rx * cos(a2).toFloat(), c.y + ry * sin(a2).toFloat())
        drawCircle(Violet, radius = s * 0.02f, center = p2)
    }
}

private fun DrawScope.hat() {
    val s = size.width
    val pivot = Offset(s * 0.62f, s * 0.1f)
    rotate(16f, pivot) {
        val brimW = s * 0.52f
        val brimH = s * 0.1f
        val topW = s * 0.32f
        val topH = s * 0.3f
        val crownRect = Path().apply {
            moveTo(pivot.x - topW / 2, pivot.y)
            lineTo(pivot.x - topW / 2 * 0.92f, pivot.y - topH)
            lineTo(pivot.x + topW / 2 * 0.92f, pivot.y - topH)
            lineTo(pivot.x + topW / 2, pivot.y)
            close()
        }
        drawPath(crownRect, Color(0xFF1A1030))
        drawPath(crownRect, Violet, style = Stroke(width = s * 0.014f, join = StrokeJoin.Round))
        // Лента
        drawRect(Magenta, Offset(pivot.x - topW / 2, pivot.y - s * 0.075f), Size(topW, s * 0.05f))
        drawOval(Color(0xFF1A1030), Offset(pivot.x - brimW / 2, pivot.y - brimH / 2), Size(brimW, brimH))
        drawOval(Violet, Offset(pivot.x - brimW / 2, pivot.y - brimH / 2), Size(brimW, brimH), style = Stroke(width = s * 0.014f))
    }
}

private fun DrawScope.crown(bob: Float) {
    val s = size.width
    val w = s * 0.5f
    val h = s * 0.22f
    val baseY = s * 0.08f - bob * s * 0.015f
    val left = s / 2 - w / 2
    val pts = listOf(
        Offset(left, baseY), Offset(left, baseY - h * 0.75f), Offset(left + w * 0.25f, baseY - h * 0.35f),
        Offset(left + w * 0.5f, baseY - h), Offset(left + w * 0.75f, baseY - h * 0.35f),
        Offset(left + w, baseY - h * 0.75f), Offset(left + w, baseY),
    )
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        pts.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    val neon = Brush.horizontalGradient(listOf(Violet, Cyan, Mint), startX = left, endX = left + w)
    drawPath(path, Color(0xFF0B1A2A).copy(alpha = 0.85f))
    drawPath(path, Cyan.copy(alpha = 0.25f), style = Stroke(width = s * 0.07f, join = StrokeJoin.Round))
    drawPath(path, neon, style = Stroke(width = s * 0.022f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    listOf(pts[1], pts[3], pts[5]).forEach { drawCircle(Color.White, radius = s * 0.022f, center = it) }
}

private fun DrawScope.stars(phase: Float) {
    val s = size.width
    // Звёздочки вокруг кольца, каждая мерцает в свой момент
    val spots = listOf(
        Triple(0.10f, 0.12f, Cyan), Triple(0.90f, 0.18f, Violet), Triple(0.97f, 0.62f, Mint),
        Triple(0.06f, 0.70f, Magenta), Triple(0.78f, 0.95f, Cyan), Triple(0.50f, -0.04f, Color.White),
    )
    spots.forEachIndexed { i, (x, y, color) ->
        val tw = ((sin((phase + i / 6f) * 2 * PI) + 1) / 2).toFloat()
        val r = s * (0.05f + 0.035f * tw)
        sparkle(Offset(s * x, s * y), r, color.copy(alpha = 0.45f + 0.55f * tw))
    }
}

private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    val k = r * 0.28f
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticBezierTo(c.x + k * 0.3f, c.y - k * 0.3f, c.x + r, c.y)
        quadraticBezierTo(c.x + k * 0.3f, c.y + k * 0.3f, c.x, c.y + r)
        quadraticBezierTo(c.x - k * 0.3f, c.y + k * 0.3f, c.x - r, c.y)
        quadraticBezierTo(c.x - k * 0.3f, c.y - k * 0.3f, c.x, c.y - r)
        close()
    }
    drawCircle(color.copy(alpha = color.alpha * 0.25f), radius = r * 0.9f, center = c)
    drawPath(p, color)
}


// ---------- вторая партия: нарисовано заново в неоне по эскизам Адониса ----------

private fun star4(c: Offset, r: Float, thin: Float = 0.22f) = Path().apply {
    val k = r * thin
    moveTo(c.x, c.y - r)
    quadraticBezierTo(c.x + k * 0.35f, c.y - k * 0.35f, c.x + r, c.y)
    quadraticBezierTo(c.x + k * 0.35f, c.y + k * 0.35f, c.x, c.y + r)
    quadraticBezierTo(c.x - k * 0.35f, c.y + k * 0.35f, c.x - r, c.y)
    quadraticBezierTo(c.x - k * 0.35f, c.y - k * 0.35f, c.x, c.y - r)
    close()
}

private fun DrawScope.sparkleCluster(bob: Float) {
    val s = size.width
    val big = Offset(s * 0.86f, s * 0.12f)
    rotate(-18f, big) {
        drawOval(Cyan.copy(alpha = 0.8f), Offset(big.x - s * 0.24f, big.y - s * 0.07f), Size(s * 0.48f, s * 0.14f),
            style = Stroke(width = s * 0.018f, cap = StrokeCap.Round))
    }
    val r = s * (0.15f + 0.02f * bob)
    drawCircle(Cyan.copy(alpha = 0.18f), radius = r * 0.9f, center = big)
    drawPath(star4(big, r), Color.White)
    drawPath(star4(Offset(s * 0.66f, s * 0.24f), s * (0.065f + 0.015f * (1 - bob))), Violet)
}

private fun star5(c: Offset, r: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.42f
        val a = -PI / 2 + i * PI / 5
        val x = c.x + rr * cos(a).toFloat()
        val y = c.y + rr * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.comet(phase: Float) {
    val s = size.width
    val head = Offset(s * 0.86f, s * 0.08f)
    val tailEnd = Offset(s * 0.05f, s * 0.16f)
    // Хвост: клин, который тает к концу
    val tail = Path().apply {
        moveTo(tailEnd.x, tailEnd.y)
        lineTo(head.x - s * 0.05f, head.y - s * 0.035f)
        lineTo(head.x - s * 0.04f, head.y + s * 0.035f)
        close()
    }
    drawPath(tail, Brush.horizontalGradient(listOf(Color.Transparent, Cyan.copy(alpha = 0.5f), HaloGold), startX = tailEnd.x, endX = head.x))
    // Искры вдоль хвоста бегут к голове
    for (i in 0 until 3) {
        val t = (phase + i / 3f) % 1f
        val p = Offset(tailEnd.x + (head.x - tailEnd.x) * t, tailEnd.y + (head.y - tailEnd.y) * t)
        drawCircle(Color.White.copy(alpha = t), radius = s * 0.012f, center = p)
    }
    rotate(phase * 360f, head) {
        drawPath(star5(head, s * 0.12f), HaloGold)
        drawPath(star5(head, s * 0.12f), Color.White, style = Stroke(width = s * 0.012f, join = StrokeJoin.Round))
    }
}

private fun DrawScope.sharpCrown(bob: Float) {
    val s = size.width
    val w = s * 0.58f
    val left = s / 2 - w / 2
    val base = s * 0.1f
    val top = base - s * 0.2f
    val zig = Path().apply {
        moveTo(left, top + s * 0.02f)
        lineTo(left + w * 0.18f, base)
        lineTo(left + w * 0.34f, top + s * 0.08f)
        lineTo(left + w * 0.5f, base - s * 0.03f)
        lineTo(left + w * 0.66f, top + s * 0.08f)
        lineTo(left + w * 0.82f, base)
        lineTo(left + w, top + s * 0.02f)
    }
    val spike = Path().apply {
        moveTo(left + w * 0.4f, base - s * 0.04f)
        lineTo(left + w * 0.5f, top - s * 0.03f)
        lineTo(left + w * 0.6f, base - s * 0.04f)
    }
    val neon = Brush.horizontalGradient(listOf(Magenta, Violet, Magenta), startX = left, endX = left + w)
    for (p in listOf(zig, spike)) {
        drawPath(p, Magenta.copy(alpha = 0.22f), style = Stroke(width = s * 0.07f, join = StrokeJoin.Miter))
        drawPath(p, neon, style = Stroke(width = s * 0.026f, join = StrokeJoin.Miter))
    }
    // Три ромба над короной
    val lift = bob * s * 0.02f
    for ((x, y) in listOf(left + w * 0.5f to top - s * 0.12f - lift, left - s * 0.02f to top - s * 0.06f - lift, left + w + s * 0.02f to top - s * 0.06f - lift)) {
        val d = s * 0.035f
        drawPath(Path().apply { moveTo(x, y - d * 1.6f); lineTo(x + d, y); lineTo(x, y + d * 1.6f); lineTo(x - d, y); close() }, Color.White)
    }
}

private fun DrawScope.starCrown(phase: Float) {
    val s = size.width
    val c = Offset(s / 2, s * 0.04f)
    val rx = s * 0.38f
    val ry = s * 0.09f
    val n = 12
    // Сначала дальние звёзды (сверху эллипса), потом ближние: так венец выглядит объёмным
    val items = (0 until n).map { i ->
        val a = (i.toFloat() / n + phase * 0.25f) * 2 * PI
        Triple(sin(a).toFloat(), cos(a).toFloat(), i)
    }.sortedBy { it.first }
    for ((depth, cx, _) in items) {
        val front = (depth + 1) / 2
        val p = Offset(c.x + rx * cx, c.y + ry * depth)
        val r = s * (0.035f + 0.045f * front)
        drawPath(star4(p, r, 0.3f), Color.White.copy(alpha = 0.35f + 0.65f * front))
        if (front > 0.6f) drawCircle(Cyan.copy(alpha = 0.18f), radius = r, center = p)
    }
}

private fun DrawScope.thornHalo(phase: Float, bob: Float) {
    val s = size.width
    val c = Offset(s / 2, s / 2)
    val r = s * 0.53f
    val crimson = Color(0xFFB3122E)
    rotate(phase * 30f - 15f, c) {
        drawArc(crimson.copy(alpha = 0.25f), 150f, 240f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(width = s * 0.08f))
        drawArc(crimson, 150f, 240f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(width = s * 0.03f, cap = StrokeCap.Round))
        // Шипы наружу разной длины; набор одинаковый у всех, «дыхание» меняет длину
        val lens = listOf(0.18f, 0.08f, 0.26f, 0.1f, 0.2f, 0.06f, 0.3f, 0.09f, 0.22f, 0.07f, 0.17f, 0.12f, 0.27f)
        lens.forEachIndexed { i, l ->
            val ang = Math.toRadians((150.0 + 240.0 * i / (lens.size - 1)))
            val dir = Offset(cos(ang).toFloat(), sin(ang).toFloat())
            val from = c + dir * r
            val to = c + dir * (r + s * l * 0.6f * (0.85f + 0.3f * bob))
            val side = Offset(-dir.y, dir.x) * (s * 0.018f)
            drawPath(Path().apply { moveTo(from.x + side.x, from.y + side.y); lineTo(to.x, to.y); lineTo(from.x - side.x, from.y - side.y); close() },
                Brush.linearGradient(listOf(crimson, Color(0xFFFF8FA3)), start = from, end = to))
        }
    }
}

private fun DrawScope.thornVine() {
    val s = size.width
    val a = Offset(-s * 0.04f, s * 0.86f)
    val ctrl = Offset(s * 0.38f, s * 0.42f)
    val b = Offset(s * 1.04f, s * 0.18f)
    val vine = Path().apply { moveTo(a.x, a.y); quadraticBezierTo(ctrl.x, ctrl.y, b.x, b.y) }
    drawPath(vine, Violet.copy(alpha = 0.25f), style = Stroke(width = s * 0.07f, cap = StrokeCap.Round))
    drawPath(vine, Brush.linearGradient(listOf(Color(0xFF2A1450), Violet, Magenta), start = a, end = b),
        style = Stroke(width = s * 0.03f, cap = StrokeCap.Round))
    // Шипы по стеблю, поочерёдно в разные стороны
    for (i in 1..8) {
        val t = i / 9f
        val p = bez(a, ctrl, b, t)
        val d = bezD(a, ctrl, b, t)
        val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y).coerceAtLeast(0.001f)
        val tan = Offset(d.x / len, d.y / len)
        val nrm = Offset(-tan.y, tan.x) * (if (i % 2 == 0) 1f else -1f)
        val tip = p + nrm * (s * 0.11f) + tan * (s * 0.05f)
        val w = tan * (s * 0.025f)
        drawPath(Path().apply { moveTo(p.x - w.x, p.y - w.y); lineTo(tip.x, tip.y); lineTo(p.x + w.x, p.y + w.y); close() }, Magenta)
    }
}

private fun bez(a: Offset, c: Offset, b: Offset, t: Float): Offset {
    val u = 1 - t
    return Offset(u * u * a.x + 2 * u * t * c.x + t * t * b.x, u * u * a.y + 2 * u * t * c.y + t * t * b.y)
}

private fun bezD(a: Offset, c: Offset, b: Offset, t: Float): Offset =
    Offset(2 * (1 - t) * (c.x - a.x) + 2 * t * (b.x - c.x), 2 * (1 - t) * (c.y - a.y) + 2 * t * (b.y - c.y))

private fun DrawScope.eclipse(bob: Float) {
    val s = size.width
    val c = Offset(s / 2, s / 2)
    // Корона затмения за кольцом и вспышка сбоку
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Cyan.copy(alpha = 0.18f), Color.Transparent), center = c, radius = s * 0.72f),
        radius = s * 0.72f, center = c)
    drawCircle(Color(0xFF020108), radius = s * 0.56f, center = c)
    drawCircle(Color.White.copy(alpha = 0.7f), radius = s * 0.56f, center = c, style = Stroke(width = s * 0.012f))
    // Росчерк по диагонали
    val from = Offset(-s * 0.1f, s * 1.05f)
    val to = Offset(s * 1.1f, -s * 0.05f)
    drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White, Color.Transparent), start = from, end = to), from, to, strokeWidth = s * 0.02f, cap = StrokeCap.Round)
    val flare = Offset(s * 0.84f, s * 0.2f)
    drawCircle(Brush.radialGradient(listOf(Color.White, Cyan.copy(alpha = 0.4f), Color.Transparent), center = flare, radius = s * (0.16f + 0.05f * bob)),
        radius = s * (0.16f + 0.05f * bob), center = flare)
    // Брызги
    listOf(0.95f to 0.06f, 1.02f to 0.28f, 0.7f to 0.02f, 0.06f to 0.9f, 0.18f to 1.0f).forEachIndexed { i, (x, y) ->
        drawCircle(Color.White.copy(alpha = 0.6f), radius = s * (0.012f + 0.006f * (i % 3)), center = Offset(s * x, s * y))
    }
}

private fun DrawScope.radiation(phase: Float) {
    val s = size.width
    val c = Offset(s * 0.83f, s * 0.83f)
    val r = s * 0.17f
    val yellow = Color(0xFFFFE14D)
    drawCircle(yellow.copy(alpha = 0.25f), radius = r * 1.2f, center = c)
    drawCircle(Color(0xFF14110A), radius = r, center = c)
    drawCircle(yellow, radius = r, center = c, style = Stroke(width = s * 0.014f))
    rotate(phase * 120f, c) {
        for (k in 0 until 3) {
            drawArc(yellow, -90f - 30f + k * 120f, 60f, true, Offset(c.x - r * 0.82f, c.y - r * 0.82f), Size(r * 1.64f, r * 1.64f))
        }
        drawCircle(Color(0xFF14110A), radius = r * 0.32f, center = c)
        drawCircle(yellow, radius = r * 0.2f, center = c)
    }
}

private fun DrawScope.dna(phase: Float) {
    val s = size.width
    val cx = s * 0.97f
    val amp = s * 0.07f
    val top = s * 0.04f
    val bottom = s * 0.96f
    val steps = 40
    val a = Path()
    val b = Path()
    for (i in 0..steps) {
        val y = top + (bottom - top) * i / steps
        val ang = (i / steps.toFloat() * 3 * PI + phase * 2 * PI)
        val xa = cx + amp * sin(ang).toFloat()
        val xb = cx - amp * sin(ang).toFloat()
        if (i == 0) { a.moveTo(xa, y); b.moveTo(xb, y) } else { a.lineTo(xa, y); b.lineTo(xb, y) }
        if (i % 4 == 2) {
            val depth = ((cos(ang) + 1) / 2).toFloat()
            drawLine(Color.White.copy(alpha = 0.25f + 0.5f * depth), Offset(xa, y), Offset(xb, y), strokeWidth = s * 0.014f, cap = StrokeCap.Round)
        }
    }
    drawPath(a, Mint, style = Stroke(width = s * 0.024f, cap = StrokeCap.Round))
    drawPath(b, Violet, style = Stroke(width = s * 0.024f, cap = StrokeCap.Round))
}

private fun DrawScope.claws(bob: Float) {
    val s = size.width
    // Четыре рваных следа когтей наискосок, сверху справа вниз влево
    val dir = Offset(-0.62f, 0.78f)
    val nrm = Offset(0.78f, 0.62f)
    for (i in 0 until 4) {
        val shift = (i - 1.5f) * s * 0.14f
        val start = Offset(s * 0.78f, s * 0.12f) + nrm * shift
        val len = s * (0.62f - 0.06f * kotlin.math.abs(i - 1.5f))
        val end = start + dir * len
        val mid = start + dir * (len * 0.45f)
        val w = nrm * (s * 0.032f)
        val slash = Path().apply {
            moveTo(start.x, start.y)
            lineTo(mid.x + w.x, mid.y + w.y)
            lineTo(end.x, end.y)
            lineTo(mid.x - w.x * 0.5f, mid.y - w.y * 0.5f)
            close()
        }
        drawPath(slash, Red.copy(alpha = 0.25f + 0.15f * bob), style = Stroke(width = s * 0.05f, join = StrokeJoin.Round))
        drawPath(slash, Brush.linearGradient(listOf(Color(0xFFFFB3C0), Red, Color(0xFF6E0016)), start = start, end = end))
    }
}

/** Плашка с текстом под аватаром: тёмная «таблетка» с неоновой рамкой. */
private fun DrawScope.bottomPlate(w: Float, h: Float, line: Color): Offset {
    val s = size.width
    val tl = Offset(s / 2 - w / 2, s * 0.84f)
    drawRoundRect(Bg, tl, Size(w, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
    drawRoundRect(line, tl, Size(w, h), androidx.compose.ui.geometry.CornerRadius(h / 2), style = Stroke(width = s * 0.012f))
    return tl
}

private fun DrawScope.glitch404(text: TextMeasurer, phase: Float) {
    val s = size.width
    val h = s * 0.22f
    val tl = bottomPlate(s * 0.62f, h, Cyan)
    val style = TextStyle(fontSize = (h * 0.72f / density / fontScale).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
    val layout = text.measure("404", style, constraints = Constraints(maxWidth = s.toInt() * 2))
    val x = s / 2 - layout.size.width / 2f
    val y = tl.y + h / 2 - layout.size.height / 2f
    // Сбой: раз в пару секунд копии разъезжаются
    val jolt = if (((phase * 12).toInt() % 7) == 0) s * 0.03f else s * 0.008f
    translate(x - jolt, y) { drawText(layout, color = Magenta.copy(alpha = 0.8f)) }
    translate(x + jolt, y) { drawText(layout, color = Cyan.copy(alpha = 0.8f)) }
    translate(x, y) { drawText(layout, color = Color.White) }
}

private fun DrawScope.loser(text: TextMeasurer, bob: Float) {
    val s = size.width
    val h = s * 0.2f
    val tl = bottomPlate(s * 0.86f, h, Color.White.copy(alpha = 0.6f))
    val style = TextStyle(fontSize = (h * 0.62f / density / fontScale).sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    val layout = text.measure("LOSER", style, constraints = Constraints(maxWidth = s.toInt() * 2))
    val x = s / 2 - layout.size.width / 2f
    val y = tl.y + h / 2 - layout.size.height / 2f
    translate(x, y) {
        drawText(layout, color = Color(0xFFEAF2FF))
        // Красная «V» поверх «S»: LOSER читается как LOVER
        val box = layout.getBoundingBox(2)
        val v = Path().apply {
            moveTo(box.left - box.width * 0.1f, box.top - box.height * 0.1f)
            lineTo(box.center.x, box.bottom + box.height * 0.15f)
            lineTo(box.right + box.width * 0.1f, box.top - box.height * 0.2f)
        }
        drawPath(v, Red.copy(alpha = 0.35f + 0.3f * bob), style = Stroke(width = s * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(v, Red, style = Stroke(width = s * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
