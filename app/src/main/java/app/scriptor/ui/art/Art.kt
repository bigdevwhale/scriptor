package app.scriptor.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.ui.theme.Garamond
import app.scriptor.ui.theme.Palette
import app.scriptor.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Iron-gall ink browns and pales as it fades: full black at sure recall, a ghost near forgetting. */
fun inkFor(p: Palette, recall: Double): Color {
    val f = ((recall - 0.55) / 0.45).coerceIn(0.0, 1.0).toFloat()
    return lerp(Color(0xFFC4B08C), p.ink, f * f * (3 - 2 * f))
}

/** Parchment: the page colour with fibres, flecks and a darker edge, drawn once per size. */
fun Modifier.parchment(base: Color, seed: Int = 7, edge: Float = 0.10f): Modifier = drawWithCache {
    val bmp = ImageBitmap(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
    val canvas = androidx.compose.ui.graphics.Canvas(bmp)
    val scope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
    scope.draw(this, layoutDirection, canvas, size) {
        drawRect(base)
        val rnd = java.util.Random(seed.toLong())
        val n = (size.width * size.height / 900f).toInt().coerceIn(40, 2400)
        repeat(n) {
            val x = rnd.nextFloat() * size.width
            val y = rnd.nextFloat() * size.height
            val dark = rnd.nextFloat() < 0.6f
            val a = 0.025f + rnd.nextFloat() * 0.05f
            if (rnd.nextFloat() < 0.18f) {
                // A fibre.
                val len = 6f + rnd.nextFloat() * 22f
                val ang = rnd.nextFloat() * PI.toFloat()
                drawLine(Color(0xFF7A5A30).copy(alpha = a), Offset(x, y), Offset(x + cos(ang) * len, y + sin(ang) * len), strokeWidth = 0.8f)
            } else {
                drawCircle(if (dark) Color(0xFF6B4A22).copy(alpha = a) else Color.White.copy(alpha = a * 1.4f), radius = 0.6f + rnd.nextFloat() * 1.8f, center = Offset(x, y))
            }
        }
        // Edges darken where hands have held it.
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color(0xFF6B4A22).copy(alpha = edge)),
                center = Offset(size.width / 2, size.height / 2),
                radius = maxOf(size.width, size.height) * 0.75f,
            )
        )
    }
    onDrawBehind { drawImage(bmp) }
}

/**
 * An illuminated initial: the letter on a field of lapis or cinnabar, framed in gold with white
 * filigree. Before the page is restored the letter is merely inked.
 */
@Composable
fun Initial(letter: Char, size: Dp, painted: Boolean, gilded: Boolean = false, seed: Int = 0) {
    val c = U.c
    val field = if (seed % 2 == 0) c.lapis else c.cinnabar
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (painted) Canvas(Modifier.fillMaxSize()) { drawInitialField(c, field, gilded) }
        Text(
            letter.uppercase(),
            style = TextStyle(
                fontFamily = Garamond, fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.66f).sp, lineHeight = (size.value * 0.7f).sp,
                color = when {
                    painted && gilded -> c.goldSoft
                    painted -> c.vellum
                    else -> c.cinnabar
                },
            ),
        )
    }
}

private fun DrawScope.drawInitialField(c: Palette, field: Color, gilded: Boolean) {
    val w = size.width
    val r = CornerRadius(w * 0.06f)
    drawRoundRect(Brush.linearGradient(listOf(c.goldSoft, c.gold, c.goldDeep, c.gold), Offset.Zero, Offset(w, w)), cornerRadius = r)
    val inset = w * 0.08f
    drawRoundRect(field, Offset(inset, inset), Size(w - inset * 2, w - inset * 2), cornerRadius = CornerRadius(w * 0.03f))
    // White filigree: tendrils curling in from the corners.
    val fil = Color.White.copy(alpha = 0.55f)
    val s = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
    for (k in 0 until 4) {
        rotate(90f * k, Offset(w / 2, w / 2)) {
            val p = Path().apply {
                moveTo(inset * 1.6f, inset * 1.6f)
                cubicTo(w * 0.32f, w * 0.14f, w * 0.18f, w * 0.34f, w * 0.30f, w * 0.30f)
                moveTo(inset * 1.6f, inset * 1.6f)
                cubicTo(w * 0.14f, w * 0.40f, w * 0.10f, w * 0.20f, w * 0.20f, w * 0.48f)
            }
            drawPath(p, fil, style = s)
            drawCircle(fil, w * 0.016f, Offset(w * 0.30f, w * 0.30f))
            drawCircle(c.gold.copy(alpha = 0.9f), w * 0.02f, Offset(inset * 1.6f, inset * 1.6f))
        }
    }
    if (gilded) {
        // Burnished leaf catches the light.
        drawRoundRect(
            Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent), Offset(0f, 0f), Offset(w, w)),
            cornerRadius = r,
        )
    }
}

/** Vines climbing the margin: a stem of verdigris with leaves, cinnabar berries and gold dots. */
@Composable
fun Vines(modifier: Modifier, seed: Int = 0) {
    val c = U.c
    Canvas(modifier) {
        val h = size.height
        val x0 = size.width / 2
        val amp = size.width * 0.28f
        val step = 46.dp.toPx()
        val stem = Path()
        stem.moveTo(x0, 0f)
        var y = 0f
        while (y < h) {
            val y1 = (y + step).coerceAtMost(h)
            val side = if (((y / step).toInt() + seed) % 2 == 0) 1 else -1
            stem.cubicTo(x0 + amp * side, y + step * 0.25f, x0 + amp * side, y + step * 0.75f, x0, y1)
            y = y1
        }
        drawPath(stem, c.verdigris, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
        var i = 0
        y = step * 0.5f
        while (y < h) {
            val side = if ((i + seed) % 2 == 0) 1 else -1
            val bx = x0 + amp * 0.75f * side
            // A leaf.
            translate(bx, y) {
                rotate(if (side > 0) -35f else 215f, Offset.Zero) {
                    val leaf = Path().apply {
                        moveTo(0f, 0f)
                        quadraticTo(size.width * 0.18f, -size.width * 0.16f, size.width * 0.36f, 0f)
                        quadraticTo(size.width * 0.18f, size.width * 0.16f, 0f, 0f)
                    }
                    drawPath(leaf, c.verdigris)
                }
            }
            if (i % 2 == 0) {
                drawCircle(c.cinnabar, 2.6.dp.toPx(), Offset(x0 - amp * 0.4f * side, y + step * 0.22f))
                drawCircle(c.cinnabar, 2.0.dp.toPx(), Offset(x0 - amp * 0.62f * side, y + step * 0.34f))
            } else {
                drawCircle(c.gold, 2.2.dp.toPx(), Offset(x0 - amp * 0.5f * side, y + step * 0.2f))
            }
            i++
            y += step
        }
    }
}

/** A red wax seal, pressed a little crooked, with a cross of the scriptorium on it. */
@Composable
fun Seal(size: Dp, modifier: Modifier = Modifier) {
    val c = U.c
    Canvas(modifier.size(size)) { drawSeal(c.wax, this.size.width) }
}

fun DrawScope.drawSeal(wax: Color, w: Float) {
    val cx = w / 2
    val blob = Path()
    val n = 36
    for (i in 0..n) {
        val a = i * 2 * PI / n
        val r = w * (0.45f + 0.035f * sin(a * 5 + 1).toFloat() + 0.02f * sin(a * 9).toFloat())
        val x = cx + (r * cos(a)).toFloat()
        val y = cx + (r * sin(a)).toFloat()
        if (i == 0) blob.moveTo(x, y) else blob.lineTo(x, y)
    }
    blob.close()
    drawPath(blob, Brush.radialGradient(listOf(lerp(wax, Color.White, 0.18f), wax, lerp(wax, Color.Black, 0.25f)), Offset(cx * 0.8f, cx * 0.75f), w * 0.6f))
    drawCircle(lerp(wax, Color.Black, 0.22f), w * 0.30f, Offset(cx, cx), style = Stroke(w * 0.03f))
    val arm = w * 0.17f
    val st = Stroke(w * 0.06f, cap = StrokeCap.Round)
    drawLine(lerp(wax, Color.Black, 0.3f), Offset(cx, cx - arm), Offset(cx, cx + arm), st.width, StrokeCap.Round)
    drawLine(lerp(wax, Color.Black, 0.3f), Offset(cx - arm, cx), Offset(cx + arm, cx), st.width, StrokeCap.Round)
    drawCircle(Color.White.copy(alpha = 0.22f), w * 0.07f, Offset(cx * 0.72f, cx * 0.66f))
}

/** A candle on the desk; lit ones flicker. */
@Composable
fun Candle(lit: Boolean, modifier: Modifier = Modifier, height: Dp = 46.dp) {
    val c = U.c
    val t = if (lit) {
        val tr = rememberInfiniteTransition(label = "flame")
        val v by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "flicker")
        v
    } else 0f
    Canvas(modifier.size(height * 0.5f, height)) {
        val w = size.width
        val h = size.height
        val top = h * 0.38f
        val bw = w * 0.46f
        val bx = (w - bw) / 2
        if (lit) {
            drawCircle(
                Brush.radialGradient(listOf(c.candle.copy(alpha = 0.35f), Color.Transparent), Offset(w / 2, top - h * 0.12f), w * 0.95f),
                w * 0.95f, Offset(w / 2, top - h * 0.12f),
            )
        }
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color(0xFFE9DCC0), Color(0xFFFFF8E8), Color(0xFFD9C8A4)), bx, bx + bw),
            Offset(bx, top), Size(bw, h - top), CornerRadius(w * 0.06f),
        )
        // A drip.
        drawRoundRect(Color(0xFFF6EDD9), Offset(bx + bw * 0.62f, top), Size(bw * 0.2f, h * 0.16f), CornerRadius(bw * 0.1f))
        drawLine(Color(0xFF3A2A1E), Offset(w / 2, top), Offset(w / 2, top - h * 0.06f), w * 0.04f)
        if (lit) {
            val sway = sin(t * 2 * PI).toFloat() * w * 0.04f
            val tall = 1f + 0.08f * sin(t * 4 * PI + 1).toFloat()
            val fh = h * 0.26f * tall
            val base = top - h * 0.04f
            val flame = Path().apply {
                moveTo(w / 2, base)
                cubicTo(w / 2 - w * 0.2f, base - fh * 0.3f, w / 2 - w * 0.08f + sway, base - fh * 0.75f, w / 2 + sway, base - fh)
                cubicTo(w / 2 + w * 0.08f + sway, base - fh * 0.75f, w / 2 + w * 0.2f, base - fh * 0.3f, w / 2, base)
            }
            drawPath(flame, Brush.verticalGradient(listOf(Color(0xFFFFF4C2), c.candle, Color(0xFFE0672A)), base - fh, base))
        } else {
            // A thread of smoke from a snuffed wick.
            drawLine(Color(0xFF8C7B68).copy(alpha = 0.35f), Offset(w / 2, top - h * 0.07f), Offset(w / 2 + w * 0.06f, top - h * 0.18f), w * 0.025f, StrokeCap.Round)
        }
    }
}

/** The novice: a small monk with a tonsure, his book under his arm, wondering. */
@Composable
fun Novice(modifier: Modifier, asking: Boolean = true) {
    val c = U.c
    val tr = rememberInfiniteTransition(label = "novice")
    val bob by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "bob")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val dy = sin(bob * 2 * PI).toFloat() * h * 0.012f
        val robe = Color(0xFF7A5434)
        val robeDark = Color(0xFF5E3F25)
        val skin = Color(0xFFF0C9A0)
        translate(0f, dy) {
            // Robe.
            val body = Path().apply {
                moveTo(w * 0.5f, h * 0.36f)
                cubicTo(w * 0.30f, h * 0.40f, w * 0.20f, h * 0.70f, w * 0.18f, h * 0.96f)
                lineTo(w * 0.82f, h * 0.96f)
                cubicTo(w * 0.80f, h * 0.70f, w * 0.70f, h * 0.40f, w * 0.5f, h * 0.36f)
                close()
            }
            drawPath(body, Brush.verticalGradient(listOf(robe, robeDark), h * 0.36f, h))
            // Hood around the neck.
            drawOval(robeDark, Offset(w * 0.30f, h * 0.34f), Size(w * 0.40f, h * 0.13f))
            // Rope belt.
            drawLine(Color(0xFFD9C08A), Offset(w * 0.26f, h * 0.66f), Offset(w * 0.74f, h * 0.66f), w * 0.025f, StrokeCap.Round)
            drawLine(Color(0xFFD9C08A), Offset(w * 0.60f, h * 0.66f), Offset(w * 0.62f, h * 0.82f), w * 0.02f, StrokeCap.Round)
            // The book, held to the chest.
            rotate(-12f, Offset(w * 0.40f, h * 0.56f)) {
                drawRoundRect(c.cinnabar, Offset(w * 0.28f, h * 0.48f), Size(w * 0.24f, h * 0.17f), CornerRadius(w * 0.02f))
                drawRect(c.gold, Offset(w * 0.30f, h * 0.48f), Size(w * 0.02f, h * 0.17f))
            }
            drawCircle(skin, w * 0.045f, Offset(w * 0.45f, h * 0.57f))
            // Head.
            val hc = Offset(w * 0.5f, h * 0.24f)
            val hr = w * 0.16f
            drawCircle(skin, hr, hc)
            // The tonsure: a ring of hair round a bare crown.
            drawArc(Color(0xFF5A3B22), 150f, 240f, false, Offset(hc.x - hr, hc.y - hr), Size(hr * 2, hr * 2), style = Stroke(hr * 0.42f))
            drawCircle(skin, hr * 0.62f, Offset(hc.x, hc.y - hr * 0.3f))
            drawCircle(Color.White.copy(alpha = 0.35f), hr * 0.2f, Offset(hc.x - hr * 0.2f, hc.y - hr * 0.55f))
            // Face.
            drawCircle(Color(0xFF2A1F17), hr * 0.09f, Offset(hc.x - hr * 0.33f, hc.y + hr * 0.18f))
            drawCircle(Color(0xFF2A1F17), hr * 0.09f, Offset(hc.x + hr * 0.33f, hc.y + hr * 0.18f))
            drawCircle(Color(0xFFE8A08A).copy(alpha = 0.6f), hr * 0.14f, Offset(hc.x - hr * 0.55f, hc.y + hr * 0.45f))
            drawCircle(Color(0xFFE8A08A).copy(alpha = 0.6f), hr * 0.14f, Offset(hc.x + hr * 0.55f, hc.y + hr * 0.45f))
            if (asking) {
                drawArc(Color(0xFF2A1F17), 20f, 140f, false, Offset(hc.x - hr * 0.16f, hc.y + hr * 0.42f), Size(hr * 0.32f, hr * 0.26f), style = Stroke(hr * 0.06f, cap = StrokeCap.Round))
            } else {
                drawArc(Color(0xFF2A1F17), 20f, 140f, false, Offset(hc.x - hr * 0.3f, hc.y + hr * 0.3f), Size(hr * 0.6f, hr * 0.4f), style = Stroke(hr * 0.07f, cap = StrokeCap.Round))
            }
        }
    }
}

/** A quill, for the empty states and the welcome. */
@Composable
fun Quill(modifier: Modifier, color: Color = U.c.ink) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        rotate(-35f) {
            val vane = Path().apply {
                moveTo(w * 0.5f, h * 0.02f)
                cubicTo(w * 0.86f, h * 0.18f, w * 0.72f, h * 0.55f, w * 0.52f, h * 0.78f)
                lineTo(w * 0.5f, h * 0.78f)
                cubicTo(w * 0.30f, h * 0.55f, w * 0.20f, h * 0.20f, w * 0.5f, h * 0.02f)
                close()
            }
            drawPath(vane, Brush.verticalGradient(listOf(Color(0xFFFDF8EE), Color(0xFFE6D9BF)), 0f, h * 0.8f))
            // Barbs.
            for (i in 1..9) {
                val y = h * (0.08f + i * 0.07f)
                drawLine(Color(0xFFC9B48F), Offset(w * 0.5f, y), Offset(w * (0.5f + 0.18f * sin(i * 1.0).toFloat().let { if (it < 0) -it else it } + 0.05f), y - h * 0.05f), w * 0.012f)
                drawLine(Color(0xFFC9B48F), Offset(w * 0.5f, y), Offset(w * 0.34f, y - h * 0.04f), w * 0.012f)
            }
            drawLine(Color(0xFFB79E74), Offset(w * 0.5f, h * 0.04f), Offset(w * 0.5f, h * 0.92f), w * 0.025f, StrokeCap.Round)
            drawLine(color, Offset(w * 0.5f, h * 0.88f), Offset(w * 0.5f, h * 0.99f), w * 0.03f, StrokeCap.Round)
        }
    }
}

/** A small gold or ink diamond, the scriptorium's paragraph mark between parts. */
@Composable
fun Fleuron(modifier: Modifier = Modifier.size(width = 120.dp, height = 14.dp), color: Color = U.c.cinnabar) {
    Canvas(modifier) {
        val cy = size.height / 2
        val cx = size.width / 2
        drawLine(color.copy(alpha = 0.45f), Offset(0f, cy), Offset(cx - size.height, cy), 1.dp.toPx())
        drawLine(color.copy(alpha = 0.45f), Offset(cx + size.height, cy), Offset(size.width, cy), 1.dp.toPx())
        val d = size.height * 0.42f
        val p = Path().apply {
            moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close()
        }
        drawPath(p, color)
        drawCircle(color, d * 0.3f, Offset(cx - d * 2f, cy))
        drawCircle(color, d * 0.3f, Offset(cx + d * 2f, cy))
    }
}
