package app.scriptor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.scriptor.R
import app.scriptor.domain.Strokes
import app.scriptor.ui.art.parchment
import app.scriptor.ui.stringResource
import app.scriptor.ui.theme.U

/**
 * A square of vellum to draw on with a finger: arrows, boxes, a little figure. The strokes come
 * back encoded after each one is lifted. Touches here never scroll the page around it.
 */
@Composable
fun DrawPad(initial: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val strokes = remember { mutableStateListOf<List<Pair<Float, Float>>>().apply { addAll(Strokes.decode(initial)) } }
    var live by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    val ink = U.c.ink
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(U.RSm)
                .parchment(U.c.vellum, edge = 0.05f)
                .border(1.dp, U.c.line, U.RSm)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        fun norm(o: Offset) = (o.x / w).coerceIn(0f, 1f) to (o.y / h).coerceIn(0f, 1f)
                        var points = listOf(norm(down.position))
                        live = points
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            c.consume()
                            points = points + norm(c.position)
                            live = points
                        }
                        strokes += points
                        live = emptyList()
                        onChange(Strokes.encode(strokes))
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                strokes.forEach { drawStroke(it, ink) }
                drawStroke(live, ink)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton(stringResource(R.string.draw_undo), {
                if (strokes.isNotEmpty()) {
                    strokes.removeAt(strokes.lastIndex)
                    onChange(Strokes.encode(strokes))
                }
            }, Modifier.weight(1f), color = U.c.muted, soft = U.c.surface2, icon = Icons.AutoMirrored.Rounded.Undo)
            SoftButton(stringResource(R.string.draw_clear), {
                strokes.clear()
                onChange("")
            }, Modifier.weight(1f), color = U.c.muted, soft = U.c.surface2, icon = Icons.Rounded.Clear)
        }
    }
}

/** A miniature drawn back from its strokes, in [ink]. */
@Composable
fun Miniature(strokes: String, modifier: Modifier = Modifier, ink: Color = U.c.ink) {
    val list = remember(strokes) { Strokes.decode(strokes) }
    Box(modifier.aspectRatio(1f).clip(U.RSm).parchment(U.c.vellum, edge = 0.05f).border(1.dp, U.c.line, U.RSm)) {
        Canvas(Modifier.fillMaxSize()) { list.forEach { drawStroke(it, ink) } }
    }
}

private fun DrawScope.drawStroke(points: List<Pair<Float, Float>>, color: Color) {
    if (points.isEmpty()) return
    val w = size.width
    val h = size.height
    val width = (w / 110f).coerceAtLeast(2.5f)
    if (points.size == 1) {
        drawCircle(color, width / 2, Offset(points[0].first * w, points[0].second * h))
        return
    }
    val path = Path()
    path.moveTo(points[0].first * w, points[0].second * h)
    // Smoothed through midpoints, so a quick finger still leaves a pen's curve.
    for (i in 1 until points.size) {
        val (x0, y0) = points[i - 1]
        val (x1, y1) = points[i]
        path.quadraticTo(x0 * w, y0 * h, (x0 + x1) / 2 * w, (y0 + y1) / 2 * h)
    }
    path.lineTo(points.last().first * w, points.last().second * h)
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
