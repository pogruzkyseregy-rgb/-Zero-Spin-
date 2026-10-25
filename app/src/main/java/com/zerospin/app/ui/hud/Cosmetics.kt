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
)

private val HaloGold = Color(0xFFFFF0A8)
private val DemonRed = Color(0xFFFF2E4D)

/**
 * Украшение поверх круглого аватара размером size. Рисуется в тех же границах,
 * но может немного выходить вверх (нимб, корона, шляпа) — как ушки у Авроры.
 */
@Composable
fun CosmeticOverlay(kind: String, size: Int, modifier: Modifier = Modifier) {
    if (kind.isBlank() || AvatarCosmetics.none { it.first == kind }) return
    val t = rememberInfiniteTransition(label = "cosmetic")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "phase")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bob")
    Canvas(modifier.size(size.dp)) {
        when (kind) {
            "halo" -> halo(bob, HaloGold, Color(0xFFFFFFFF))
            "demon" -> demon(bob)
            "orbit" -> orbit(phase)
            "hat" -> hat()
            "crown" -> crown(bob)
            "stars" -> stars(phase)
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
