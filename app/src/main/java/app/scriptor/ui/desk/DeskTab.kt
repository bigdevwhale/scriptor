package app.scriptor.ui.desk

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.scriptor.R
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.Source
import app.scriptor.ui.T
import app.scriptor.ui.art.Candle
import app.scriptor.ui.art.Fleuron
import app.scriptor.ui.art.Quill
import app.scriptor.ui.art.parchment
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PageRow
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.SoftButton
import app.scriptor.ui.ctx
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/** The desk: what is fading tonight, what yesterday asks back, and the codices open on it. */
@Composable
fun DeskTab(desk: Desk, go: Go) {
    val c = ctx()
    Column(Modifier.fillMaxSize().parchment(U.c.bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(T.hour(c, desk.now).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(2.dp))
                Text(T.longDay(c, desk.now), style = UType.Title)
            }
            IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings_title), tint = U.c.muted) }
        }

        if (desk.pages.isEmpty()) {
            Empty(go)
            return@Column
        }

        Spacer(Modifier.height(14.dp))
        Candles(desk, go)

        if (desk.yesterday.isNotEmpty() && !desk.matinsKept) {
            Spacer(Modifier.height(12.dp))
            Leaf(onClick = { go.restore(Source.MATINS) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WbTwilight, null, tint = U.c.gold, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.matins), style = UType.Name)
                        Text(pluralStringResource(R.plurals.matins_text, desk.yesterday.size, desk.yesterday.size), style = UType.Muted)
                    }
                }
            }
        }

        if (desk.errata.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Leaf(onClick = go::errata) {
                Text(stringResource(R.string.errata_title), style = UType.Name.copy(color = U.c.cinnabar))
                Text(pluralStringResource(R.plurals.errata_text, desk.errata.size, desk.errata.size), style = UType.Muted)
            }
        }

        Rubric(stringResource(R.string.desk_open))
        desk.open.take(4).forEach { cx ->
            val pages = desk.pagesOf(cx.id)
            Leaf(Modifier.padding(bottom = 10.dp), onClick = { go.codex(cx.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(T.kindIcon(cx.kindOf), null, tint = U.c.muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cx.title, style = UType.Name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOf(cx.author, pluralStringResource(R.plurals.pages, pages.size, pages.size)).filter { it.isNotBlank() }.joinToString(" · "),
                            style = UType.Muted, maxLines = 1,
                        )
                    }
                    SoftButton(stringResource(R.string.write_page_short), { go.write(cx.id) }, color = U.c.cinnabar, soft = U.c.cinnabarSoft, icon = Icons.Rounded.Add)
                }
            }
        }
        if (desk.open.isEmpty()) Text(stringResource(R.string.desk_none_open), style = UType.Italic)
        Spacer(Modifier.height(6.dp))
        SoftButton(stringResource(R.string.new_codex), go::newCodex, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)

        val recent = desk.pages.sortedByDescending { it.createdAt }.take(3)
        if (recent.isNotEmpty()) {
            Rubric(stringResource(R.string.desk_recent))
            recent.forEach { PageRow(it, desk, { go.page(it.id) }, showCodex = true) }
        }
        Spacer(Modifier.height(28.dp))
    }
}

/** One candle per fading page: tonight's work. Snuffed ones are what has been restored today. */
@Composable
private fun Candles(desk: Desk, go: Go) {
    val n = desk.fading.size
    Leaf(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            val lit = n.coerceAtMost(7)
            val snuffed = desk.restoredToday.coerceAtMost(7 - lit).coerceAtLeast(if (n == 0) 1 else 0)
            repeat(snuffed) { Candle(false, Modifier.padding(horizontal = 4.dp), 50.dp) }
            repeat(lit) { Candle(true, Modifier.padding(horizontal = 4.dp), 56.dp) }
        }
        Spacer(Modifier.height(10.dp))
        Fleuron(Modifier.align(Alignment.CenterHorizontally).width(140.dp).height(12.dp))
        Spacer(Modifier.height(10.dp))
        if (n == 0) {
            Text(stringResource(R.string.fading_none), style = UType.Name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(
                desk.pages.filter { !it.gilded }.minOfOrNull { it.due(desk.retention) }?.let { next ->
                    stringResource(R.string.fading_next, T.span(ctx(), (next - desk.now) / 86_400_000.0))
                } ?: stringResource(R.string.fading_all_gold),
                style = UType.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(pluralStringResource(R.plurals.fading, n, n), style = UType.Name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.fading_hint), style = UType.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            PrimaryButton(stringResource(R.string.restore_all), { go.restore(Source.FADING) }, Modifier.fillMaxWidth())
            if (n > desk.settings.complineSize) {
                Spacer(Modifier.height(8.dp))
                SoftButton(
                    stringResource(R.string.restore_compline, desk.settings.complineSize), { go.restore(Source.COMPLINE) }, Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Bedtime,
                )
            }
        }
    }
}

@Composable
private fun Empty(go: Go) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) { Quill(Modifier.size(110.dp)) }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.empty_title), style = UType.Title, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_text), style = UType.Sub, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.new_codex), go::newCodex, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
    }
}
