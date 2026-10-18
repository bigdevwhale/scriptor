package app.scriptor.ui.chronicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.scriptor.R
import app.scriptor.domain.Rules
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.T
import app.scriptor.ui.art.Candle
import app.scriptor.ui.art.inkFor
import app.scriptor.ui.art.parchment
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PageRow
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Stat
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/** The chronicle of the scriptorium: what is written, how the ink holds, and how true the scribe's eye is. */
@Composable
fun ChronicleTab(desk: Desk, go: Go) {
    Column(Modifier.fillMaxSize().parchment(U.c.bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.chronicle_title), style = UType.Title, modifier = Modifier.padding(top = 18.dp))
        Text(stringResource(R.string.chronicle_sub), style = UType.Italic)
        Spacer(Modifier.height(16.dp))
        Leaf {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(desk.codices.size.toString(), stringResource(R.string.stat_codices), Modifier.weight(1f))
                Stat(desk.pages.size.toString(), stringResource(R.string.stat_pages), Modifier.weight(1f))
                Stat(desk.reviews.size.toString(), stringResource(R.string.stat_restored), Modifier.weight(1f))
                Stat(desk.streak.toString(), stringResource(R.string.stat_days), Modifier.weight(1f), color = U.c.cinnabar)
            }
        }

        Rubric(stringResource(R.string.ink_state))
        Leaf { InkState(desk) }

        Rubric(stringResource(R.string.eye_title))
        Leaf { Eye(desk) }

        if (desk.gilded.isNotEmpty()) {
            Rubric(stringResource(R.string.golden_pages))
            Leaf(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                desk.gilded.forEach { PageRow(it, desk, { go.page(it.id) }, showCodex = true) }
            }
        }
        val sealed = desk.pages.filter { desk.illumination(it).seal }
        if (sealed.isNotEmpty()) {
            Rubric(stringResource(R.string.sealed_pages))
            Leaf(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                sealed.forEach { PageRow(it, desk, { go.page(it.id) }, showCodex = true) }
            }
        }
        Spacer(Modifier.height(28.dp))
        if (desk.pages.isEmpty()) Text(stringResource(R.string.chronicle_empty), style = UType.Sub)
    }
}

/** Every page by the state of its ink: fresh, fading, faint, gilded. One bar, segments labelled with their counts. */
@Composable
private fun InkState(desk: Desk) {
    val p = U.c
    val groups = listOf(
        Triple(R.string.ink_fresh, inkFor(p, 1.0), desk.pages.count { !it.gilded && it.recall(desk.now) >= 0.9 }),
        Triple(R.string.ink_fading_state, inkFor(p, 0.8), desk.pages.count { !it.gilded && it.recall(desk.now).let { r -> r >= 0.75 && r < 0.9 } }),
        Triple(R.string.ink_faint, inkFor(p, 0.62), desk.pages.count { !it.gilded && it.recall(desk.now) < 0.75 }),
        Triple(R.string.ink_gilded, p.gold, desk.pages.count { it.gilded }),
    )
    val total = groups.sumOf { it.third }.coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(18.dp)) {
        var x = 0f
        val gap = 2.dp.toPx()
        groups.filter { it.third > 0 }.forEach { (_, col, n) ->
            val w = size.width * n / total
            drawRoundRect(col, Offset(x, 0f), Size((w - gap).coerceAtLeast(1f), size.height), CornerRadius(4.dp.toPx()))
            x += w
        }
        if (desk.pages.isEmpty()) drawRoundRect(p.line, cornerRadius = CornerRadius(4.dp.toPx()))
    }
    Spacer(Modifier.height(12.dp))
    groups.forEach { (label, col, n) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(10.dp)) { drawCircle(col) }
            Spacer(Modifier.width(10.dp))
            Text(stringResource(label), style = UType.Body, modifier = Modifier.weight(1f))
            Text(n.toString(), style = UType.Strong)
        }
    }
}

/**
 * The scribe's eye: of the restorations staked on one, two or three candles, how many were right.
 * The dashed tick is where a true eye would land.
 */
@Composable
private fun Eye(desk: Desk) {
    val p = U.c
    val cal = Rules.calibration(desk.reviews)
    val staked = desk.reviews.count { it.confidence > 0 }
    Text(stringResource(R.string.eye_hint), style = UType.Muted)
    Spacer(Modifier.height(12.dp))
    if (staked < 3) {
        Text(stringResource(R.string.eye_empty), style = UType.Italic)
        return
    }
    cal.forEach { (n, rate) ->
        val target = Rules.CALIBRATION_TARGET.getValue(n)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.width(52.dp), verticalAlignment = Alignment.Bottom) { repeat(n) { Candle(true, Modifier.padding(end = 1.dp), 26.dp) } }
            Canvas(Modifier.weight(1f).height(16.dp)) {
                drawRoundRect(p.line.copy(alpha = 0.6f), cornerRadius = CornerRadius(4.dp.toPx()))
                if (rate != null) drawRoundRect(p.verdigris, size = Size(size.width * rate, size.height), cornerRadius = CornerRadius(4.dp.toPx()))
                val tx = size.width * target
                drawLine(p.ink, Offset(tx, -2.dp.toPx()), Offset(tx, size.height + 2.dp.toPx()), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))
            }
            Spacer(Modifier.width(10.dp))
            Text(rate?.let { T.percent(it.toDouble()) } ?: "—", style = UType.Strong, modifier = Modifier.width(44.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
    val verdict = eyeVerdict(cal)
    Text(stringResource(verdict), style = UType.Italic.copy(color = p.ink))
    Text(pluralStringResource(R.plurals.eye_count, staked, staked), style = UType.Small)
}

/** Over-sure, under-sure, or true, judged on the three-candle row where it matters most. */
private fun eyeVerdict(cal: List<Pair<Int, Float?>>): Int {
    val three = cal.first { it.first == 3 }.second
    val one = cal.first { it.first == 1 }.second
    return when {
        three != null && three < 0.8f -> R.string.eye_oversure
        one != null && one > 0.75f -> R.string.eye_undersure
        else -> R.string.eye_true
    }
}
