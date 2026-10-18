package app.scriptor.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Gloss
import app.scriptor.data.GlossKind
import app.scriptor.data.Mode
import app.scriptor.data.Page
import app.scriptor.domain.Illumination
import app.scriptor.domain.Memory
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.Source
import app.scriptor.ui.T
import app.scriptor.ui.art.Fleuron
import app.scriptor.ui.art.Initial
import app.scriptor.ui.art.Seal
import app.scriptor.ui.art.Vines
import app.scriptor.ui.art.inkFor
import app.scriptor.ui.art.parchment
import app.scriptor.ui.components.ConfirmDialog
import app.scriptor.ui.components.InkGauge
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.components.SoftButton
import app.scriptor.ui.components.VoiceButton
import app.scriptor.ui.components.appendSpoken
import app.scriptor.ui.ctx
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageScreen(id: Long, desk: Desk, go: Go) {
    val p = desk.page(id) ?: return
    val c = ctx()
    val codex = desk.codex(p.codexId)
    val ill = desk.illumination(p)
    val glosses = desk.glossesOf(id)
    val recall = p.recall(desk.now)
    var deleting by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<GlossKind?>(null) }

    Screen(
        "", go::back,
        actions = {
            IconButton(onClick = { go.write(p.codexId, id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.muted) }
            IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = U.c.muted) }
        },
    ) {
        Text(
            listOfNotNull(codex?.title, p.locator.ifBlank { null }).joinToString(" · ").uppercase(),
            style = UType.Eyebrow, modifier = Modifier.clickable { codex?.let { go.codex(it.id) } },
        )
        Spacer(Modifier.height(10.dp))
        Spread(p, ill, recall)

        Spacer(Modifier.height(14.dp))
        InkGauge(recall, gilded = p.gilded)
        Spacer(Modifier.height(6.dp))
        Text(
            if (p.gilded) stringResource(R.string.ink_gold)
            else stringResource(R.string.ink_status, T.percent(recall), T.span(c, (p.due(desk.retention) - desk.now) / Memory.DAY.toDouble()))
                .let { if (p.due(desk.retention) <= desk.now) stringResource(R.string.ink_fading, T.percent(recall)) else it },
            style = UType.Muted,
        )

        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.restore_page), { go.restore(Source.page(id)) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Replay)
        Spacer(Modifier.height(8.dp))
        SoftButton(stringResource(R.string.explain_novice), { go.novice(id) }, Modifier.fillMaxWidth(), color = U.c.verdigris, soft = U.c.verdigrisSoft)

        Rubric(stringResource(R.string.margins))
        Text(stringResource(R.string.margins_hint), style = UType.Muted)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(GlossKind.EXEMPLUM, GlossKind.PARABLE, GlossKind.DISSENT, GlossKind.SEAL).forEach { k ->
                Box(
                    Modifier.clip(U.RSm).background(U.c.surface).border(1.dp, U.c.line, U.RSm).clickable { adding = k }.padding(horizontal = 12.dp, vertical = 8.dp),
                ) { Text("+ " + T.gloss(c, k), style = UType.Body.copy(fontSize = 14.sp, color = glossColor(k))) }
            }
        }
        GlossKind.entries.forEach { k ->
            val list = glosses.filter { it.kindOf == k }
            if (list.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(T.gloss(c, k), style = UType.Rubric.copy(color = glossColor(k)))
                list.forEach { GlossItem(it) }
            }
        }

        val history = desk.reviewsOf(id)
        if (history.isNotEmpty()) {
            Rubric(stringResource(R.string.palimpsest))
            Text(stringResource(R.string.palimpsest_hint), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            history.take(6).forEach { r ->
                Leaf(Modifier.padding(bottom = 8.dp), padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(T.date(c, r.at), style = UType.Small, modifier = Modifier.weight(1f))
                        Text(
                            listOfNotNull(
                                T.mode(c, runCatching { Mode.valueOf(r.mode) }.getOrDefault(Mode.RECALL)),
                                if (r.confidence > 0) "🕯".repeat(r.confidence) else null,
                                gradeName(r.grade),
                                if (r.coverage >= 0) T.percent(r.coverage.toDouble()) else null,
                            ).joinToString(" · "),
                            style = UType.Small,
                        )
                    }
                    if (r.attempt.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(r.attempt, style = UType.Script.copy(fontSize = 15.sp, lineHeight = 20.sp, color = U.c.faint), maxLines = 4)
                    }
                }
            }
        }
    }

    adding?.let { k ->
        GlossDialog(k, onDismiss = { adding = null }) { text ->
            c.container.write { db.dao().insert(Gloss(pageId = id, kind = k.name, text = text, createdAt = System.currentTimeMillis())) }
        }
    }
    if (deleting) {
        ConfirmDialog(
            stringResource(R.string.page_delete_title), stringResource(R.string.page_delete_text), stringResource(R.string.delete),
            onConfirm = {
                c.container.write { db.dao().deletePage(p) }
                go.back()
            },
            onDismiss = { deleting = false }, danger = true,
        )
    }
}

/**
 * The page laid out as a scribe would: the painted initial and title, questions in cinnabar on the
 * left margin (the Cornell cue column), the body in ink as faded as the memory of it, the golden
 * line, the colophon. Vines climb the edge once earned; the seal sits in the corner.
 */
@Composable
fun Spread(p: Page, ill: Illumination, recall: Double, inkOverride: androidx.compose.ui.graphics.Color? = null) {
    val c = U.c
    val ink = inkOverride ?: inkFor(c, recall)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(U.RSm)
            .parchment(c.vellum, seed = p.id.toInt(), edge = 0.08f)
            .border(if (ill.gold) 2.dp else 1.dp, if (ill.gold) c.gold else c.line, U.RSm),
    ) {
        if (ill.vines) Box(Modifier.matchParentSize().padding(vertical = 10.dp)) { Vines(Modifier.width(26.dp).fillMaxHeight(), seed = p.id.toInt()) }
        Column(Modifier.fillMaxWidth().padding(start = if (ill.vines) 30.dp else 18.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Initial(p.title.firstOrNull { it.isLetterOrDigit() } ?: '·', 52.dp, painted = ill.initial, gilded = ill.gold, seed = p.id.toInt())
                    Spacer(Modifier.width(12.dp))
                    Text(p.title, style = UType.Title.copy(fontSize = 23.sp, lineHeight = 27.sp, color = ink))
                }
                Spacer(Modifier.height(14.dp))
                val cues = p.cueList
                if (cues.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(0.36f).padding(end = 10.dp)) {
                            cues.forEach { q ->
                                Text("¶ $q", style = UType.Rubric.copy(fontSize = 14.5.sp, lineHeight = 18.sp))
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                        val rule = c.cinnabar.copy(alpha = 0.35f)
                        Text(
                            p.body, style = UType.Script.copy(fontSize = 17.5.sp, lineHeight = 25.sp, color = ink),
                            modifier = Modifier.weight(0.64f).drawBehind { drawLine(rule, Offset.Zero, Offset(0f, size.height), 1.dp.toPx()) }.padding(start = 12.dp),
                        )
                    }
                } else {
                    Text(p.body, style = UType.Script.copy(color = ink))
                }
                if (p.gold.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        p.gold,
                        style = UType.Script.copy(color = c.goldDeep, fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth().clip(U.RSm).background(c.goldSoft.copy(alpha = 0.55f)).border(1.dp, c.gold.copy(alpha = 0.6f), U.RSm).padding(12.dp),
                    )
                }
                if (p.colophon.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Fleuron(Modifier.align(Alignment.CenterHorizontally).width(120.dp).height(12.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(p.colophon, style = UType.Italic.copy(color = ink, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                }
                if (p.quote.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.page_quote).uppercase(), style = UType.Eyebrow.copy(color = c.faint, fontSize = 11.sp))
                    Text("“" + p.quote + "”", style = UType.Italic.copy(color = c.faint, fontSize = 15.sp, lineHeight = 20.sp))
                }
                if (ill.seal) Spacer(Modifier.height(46.dp))
        }
        if (ill.seal) Seal(54.dp, Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 8.dp))
    }
}

@Composable
private fun GlossItem(g: Gloss) {
    val c = ctx()
    var confirm by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            if (g.prompt.isNotBlank()) Text("— " + g.prompt, style = UType.Italic.copy(fontSize = 15.sp))
            Text(g.text, style = UType.Script.copy(fontSize = 17.sp, lineHeight = 23.sp))
        }
        IconButton(onClick = { confirm = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = U.c.faint, modifier = Modifier.size(16.dp)) }
    }
    if (confirm) {
        ConfirmDialog(
            stringResource(R.string.gloss_delete), g.text.take(120), stringResource(R.string.delete),
            onConfirm = { c.container.write { db.dao().delete(g) } }, onDismiss = { confirm = false }, danger = true,
        )
    }
}

@Composable
fun glossColor(k: GlossKind) = when (k) {
    GlossKind.NOVICE, GlossKind.EXEMPLUM -> U.c.verdigris
    GlossKind.PARABLE -> U.c.lapis
    GlossKind.DISSENT -> U.c.ink
    GlossKind.SEAL -> U.c.wax
}

@Composable
private fun GlossDialog(kind: GlossKind, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val c = ctx()
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        title = { Text(T.gloss(c, kind), style = UType.Name.copy(color = glossColor(kind))) },
        text = {
            Column {
                Text(
                    stringResource(
                        when (kind) {
                            GlossKind.EXEMPLUM -> R.string.exemplum_hint
                            GlossKind.PARABLE -> R.string.parable_hint
                            GlossKind.DISSENT -> R.string.dissent_hint
                            GlossKind.SEAL -> R.string.seal_hint
                            GlossKind.NOVICE -> R.string.novice_answer_hint
                        }
                    ),
                    style = UType.Muted,
                )
                Spacer(Modifier.height(10.dp))
                ScriptField(text, { text = it }, stringResource(R.string.write_here), style = UType.Script.copy(fontSize = 17.sp, lineHeight = 24.sp), minLines = 3)
                Spacer(Modifier.height(6.dp))
                VoiceButton({ text = appendSpoken(text, it) })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()); onDismiss() }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.save), color = U.c.cinnabar, style = UType.Button)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}

@Composable
fun gradeName(n: Int): String = stringResource(
    when (n) {
        1 -> R.string.grade_again
        2 -> R.string.grade_hard
        3 -> R.string.grade_good
        else -> R.string.grade_easy
    }
)
