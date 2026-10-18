package app.scriptor.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import app.scriptor.ui.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.domain.Craft
import app.scriptor.ui.components.IconDisc
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.pluralStringResource
import app.scriptor.data.Codex
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.art.parchment
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.SoftButton
import app.scriptor.ui.theme.Garamond
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/** The leathers a codex can be bound in. */
val Leathers = listOf(
    Color(0xFF6E2A22), Color(0xFF6B3A22), Color(0xFF2F4A37), Color(0xFF263A5C),
    Color(0xFF8A5A2E), Color(0xFF2E2622), Color(0xFF4D2A3E),
)

fun leather(i: Int): Color = Leathers[Math.floorMod(i, Leathers.size)]

@Composable
fun LibraryTab(desk: Desk, go: Go) {
    Column(Modifier.fillMaxSize().parchment(U.c.bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.library_title), style = UType.Title, modifier = Modifier.padding(top = 18.dp))
        Text(stringResource(R.string.library_sub), style = UType.Italic)
        if (desk.codices.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Shelf(emptyList(), desk, go)
            Spacer(Modifier.height(18.dp))
            PrimaryButton(stringResource(R.string.new_codex), go::newCodex, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            return@Column
        }
        if (desk.has(Craft.PALACE)) {
            Spacer(Modifier.height(14.dp))
            Leaf(onClick = go::palace) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(Icons.Rounded.Church, 44.dp, U.c.vellum, U.c.leather)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.palace_title), style = UType.Name)
                        Text(pluralStringResource(R.plurals.palace_count, desk.loci.size, desk.loci.size), style = UType.Muted)
                    }
                }
            }
        }
        Rubric(stringResource(R.string.library_open))
        Shelf(desk.open, desk, go)
        if (desk.closed.isNotEmpty()) {
            Rubric(stringResource(R.string.library_closed))
            Shelf(desk.closed, desk, go)
        }
        if (desk.has(Craft.THREADS) && desk.threads.isNotEmpty()) {
            Rubric(stringResource(R.string.threads_map))
            Text(stringResource(R.string.threads_map_hint), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            ThreadMap(desk, go)
        }
        Spacer(Modifier.height(16.dp))
        SoftButton(stringResource(R.string.new_codex), go::newCodex, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        Spacer(Modifier.height(28.dp))
    }
}

/** Spines standing on wooden planks, packed into rows as they fit. */
@Composable
private fun Shelf(codices: List<Codex>, desk: Desk, go: Go) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widths = codices.map { spineWidth(desk.pagesOf(it.id).size) }
        val rows = mutableListOf<List<Int>>()
        var row = mutableListOf<Int>()
        var used = 0.dp
        widths.forEachIndexed { i, w ->
            if (row.isNotEmpty() && used + w + 4.dp > maxWidth - 12.dp) {
                rows += row
                row = mutableListOf()
                used = 0.dp
            }
            row += i
            used += w + 4.dp
        }
        if (row.isNotEmpty() || rows.isEmpty()) rows += row
        Column {
            rows.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    r.forEach { i -> Spine(codices[i], desk, widths[i]) { go.codex(codices[i].id) } }
                    if (r.isEmpty()) Spacer(Modifier.height(150.dp))
                }
                Plank()
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

private fun spineWidth(pages: Int): Dp = (38 + pages.coerceAtMost(24) * 2).dp

@Composable
private fun Plank() {
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF8A5B33), Color(0xFF5E3B1F)), 0f, size.height))
        drawRect(Color(0xFFB0804F), size = Size(size.width, 2.dp.toPx()))
        drawRect(Color.Black.copy(alpha = 0.18f), Offset(0f, size.height), Size(size.width, 4.dp.toPx()))
    }
}

/**
 * A codex on the shelf: leather, raised bands, the title tooled in gold. The leather pales with the
 * ink inside it; a dot of gold for each gilded page, and a wax tab once closed.
 */
@Composable
private fun Spine(cx: Codex, desk: Desk, width: Dp, onClick: () -> Unit) {
    val c = U.c
    val pages = desk.pagesOf(cx.id)
    val ink = if (pages.isEmpty()) 1.0 else pages.map { it.recall(desk.now) }.average()
    val gold = pages.count { it.gilded }
    val h = (150 + (cx.id * 37 % 5).toInt() * 9).dp
    val base = lerp(Color(0xFFB9A285), leather(cx.binding), (0.35 + 0.65 * ((ink - 0.55) / 0.45).coerceIn(0.0, 1.0)).toFloat())
    Box(Modifier.width(width).height(h).clickable(onClick = onClick)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            drawRoundRect(
                Brush.horizontalGradient(listOf(lerp(base, Color.Black, 0.35f), lerp(base, Color.White, 0.12f), base, lerp(base, Color.Black, 0.3f)), 0f, w),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            // Raised bands.
            listOf(0.08f, 0.16f, 0.84f, 0.92f).forEach { f ->
                val y = size.height * f
                drawRect(lerp(base, Color.Black, 0.35f), Offset(0f, y), Size(w, 3.dp.toPx()))
                drawRect(lerp(base, Color.White, 0.2f), Offset(0f, y - 1.dp.toPx()), Size(w, 1.dp.toPx()))
            }
            // Gold fillets framing the title.
            drawRect(c.gold.copy(alpha = 0.8f), Offset(w * 0.18f, size.height * 0.22f), Size(w * 0.64f, 1.dp.toPx()))
            drawRect(c.gold.copy(alpha = 0.8f), Offset(w * 0.18f, size.height * 0.78f), Size(w * 0.64f, 1.dp.toPx()))
            // A gold dot for each gilded page.
            val dots = gold.coerceAtMost(5)
            repeat(dots) { i ->
                drawCircle(c.gold, 2.2.dp.toPx(), Offset(w / 2 + (i - (dots - 1) / 2f) * 6.dp.toPx(), size.height * 0.81f))
            }
            if (cx.closed) drawRect(c.wax, Offset(w * 0.5f - 3.dp.toPx(), size.height * 0.92f + 3.dp.toPx()), Size(6.dp.toPx(), 10.dp.toPx()))
        }
        Text(
            cx.title,
            style = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.goldSoft),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).vertical(h * 0.52f).rotate(-90f),
        )
    }
}

/** Lays a child out turned on its side, at most [length] long, so a rotated title fits a spine. */
private fun Modifier.vertical(length: Dp) = layout { m, c ->
    val p = m.measure(Constraints(maxWidth = length.roundToPx(), maxHeight = c.maxWidth))
    layout(p.height, p.width) { p.place(-(p.width - p.height) / 2, (p.width - p.height) / 2) }
}

/**
 * Codices round a circle, and the red threads between their pages drawn across it: thicker where
 * two books are tied by more threads. Tap a codex to open it.
 */
@Composable
private fun ThreadMap(desk: Desk, go: Go) {
    val c = U.c
    val pageCodex = desk.pages.associate { it.id to it.codexId }
    val ties = desk.threads.mapNotNull { t ->
        val a = pageCodex[t.fromPage] ?: return@mapNotNull null
        val b = pageCodex[t.toPage] ?: return@mapNotNull null
        if (a == b) null else if (a < b) a to b else b to a
    }.groupingBy { it }.eachCount()
    val nodes = desk.codices.filter { cx -> ties.keys.any { it.first == cx.id || it.second == cx.id } }
    if (nodes.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth().height(300.dp).clip(U.RMd).parchment(c.vellum, edge = 0.06f)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val r = minOf(w, h) * 0.34f
        val centre = Offset(w / 2, h / 2)
        val at = nodes.mapIndexed { i, cx ->
            val a = -Math.PI / 2 + i * 2 * Math.PI / nodes.size
            cx.id to Offset(centre.x + (r * kotlin.math.cos(a)).toFloat(), centre.y + (r * kotlin.math.sin(a)).toFloat())
        }.toMap()
        Canvas(Modifier.fillMaxSize()) {
            ties.forEach { (pair, n) ->
                val p0 = at[pair.first] ?: return@forEach
                val p1 = at[pair.second] ?: return@forEach
                val mid = Offset((p0.x + p1.x) / 2, (p0.y + p1.y) / 2)
                // Bow each thread a little toward the centre, as a thread hangs.
                val ctrl = Offset(mid.x + (centre.x - mid.x) * 0.5f, mid.y + (centre.y - mid.y) * 0.5f)
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(p0.x, p0.y)
                    quadraticTo(ctrl.x, ctrl.y, p1.x, p1.y)
                }
                drawPath(path, c.cinnabar.copy(alpha = 0.75f), style = androidx.compose.ui.graphics.drawscope.Stroke((1.5f + n * 1.2f).dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        nodes.forEach { cx ->
            val p = at.getValue(cx.id)
            val boxW = 96.dp
            with(density) {
                Column(
                    Modifier
                        .offset(x = p.x.toDp() - boxW / 2, y = p.y.toDp() - 15.dp)
                        .width(boxW)
                        .clickable { go.codex(cx.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(width = 22.dp, height = 30.dp).clip(U.RSm).background(leather(cx.binding)).border(1.dp, c.gold, U.RSm))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        cx.title, style = UType.Small.copy(color = c.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
