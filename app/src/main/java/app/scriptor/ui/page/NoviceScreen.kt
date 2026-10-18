package app.scriptor.ui.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Gloss
import app.scriptor.data.GlossKind
import app.scriptor.domain.Rules
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.art.Novice
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.components.SoftButton
import app.scriptor.ui.components.VoiceButton
import app.scriptor.ui.components.appendSpoken
import app.scriptor.ui.ctx
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/**
 * The Feynman way: a novice who knows nothing asks about the page, and the scribe explains in plain
 * words, without looking. Where the explanation stumbles is where understanding is thin.
 */
@Composable
fun NoviceScreen(pageId: Long, desk: Desk, go: Go) {
    val p = desk.page(pageId) ?: return
    val c = ctx()
    val all = stringArrayResource(R.array.novice_questions).toList()
    var seed by remember { mutableLongStateOf(pageId * 31 + desk.glossesOf(pageId).size) }
    val question = remember(seed, desk.glossesOf(pageId).size) { Rules.noviceQuestion(all, desk.glossesOf(pageId), seed) }
    var answer by remember(question) { mutableStateOf("") }
    var peek by remember { mutableStateOf(false) }
    var understood by remember { mutableStateOf(false) }

    Screen(stringResource(R.string.novice_title), go::back) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Novice(Modifier.size(width = 108.dp, height = 150.dp), asking = !understood)
            Spacer(Modifier.width(8.dp))
            Bubble(
                when {
                    understood -> stringResource(R.string.novice_thanks)
                    question == null -> stringResource(R.string.novice_done)
                    else -> question
                },
                Modifier.weight(1f).padding(bottom = 60.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.novice_about, p.title), style = UType.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        if (understood) {
            PrimaryButton(stringResource(R.string.novice_more), { understood = false; seed++ }, Modifier.fillMaxWidth(), color = U.c.verdigris)
            Spacer(Modifier.height(8.dp))
            SoftButton(stringResource(R.string.done), go::back, Modifier.fillMaxWidth())
            return@Screen
        }
        if (question == null) {
            SoftButton(stringResource(R.string.done), go::back, Modifier.fillMaxWidth())
            return@Screen
        }

        Text(stringResource(R.string.novice_rule), style = UType.Muted)
        Spacer(Modifier.height(10.dp))
        Leaf {
            ScriptField(answer, { answer = it }, stringResource(R.string.novice_answer_hint), minLines = 5)
            Spacer(Modifier.height(8.dp))
            VoiceButton({ answer = appendSpoken(answer, it) })
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            stringResource(R.string.novice_answer), {
                val text = answer.trim()
                c.container.write { db.dao().insert(Gloss(pageId = pageId, kind = GlossKind.NOVICE.name, prompt = question, text = text, createdAt = System.currentTimeMillis())) }
                understood = true
            },
            Modifier.fillMaxWidth(), enabled = answer.isNotBlank(), color = U.c.verdigris,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton(stringResource(R.string.novice_other), { seed++ }, Modifier.weight(1f), icon = Icons.Rounded.Refresh)
            SoftButton(
                stringResource(if (peek) R.string.novice_hide else R.string.novice_peek), { peek = !peek }, Modifier.weight(1f),
                color = U.c.muted, soft = U.c.surface2, icon = if (peek) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            )
        }
        if (peek) {
            Spacer(Modifier.height(12.dp))
            Spread(p, desk.illumination(p), p.recall(desk.now))
        }
    }
}

/** A speech bubble with a tail toward the novice. */
@Composable
private fun Bubble(text: String, modifier: Modifier) {
    val c = U.c
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val r = 14.dp.toPx()
            val tail = 12.dp.toPx()
            drawRoundRect(c.vellum, Offset(tail, 0f), Size(size.width - tail, size.height), CornerRadius(r))
            drawRoundRect(c.line, Offset(tail, 0f), Size(size.width - tail, size.height), CornerRadius(r), style = Stroke(1.dp.toPx()))
            val t = Path().apply {
                moveTo(tail + 1, size.height - 26.dp.toPx())
                lineTo(0f, size.height - 8.dp.toPx())
                lineTo(tail + 1, size.height - 14.dp.toPx())
                close()
            }
            drawPath(t, c.vellum)
        }
        Text(text, style = UType.Script.copy(fontSize = 18.sp, lineHeight = 23.sp), modifier = Modifier.padding(start = 26.dp, end = 14.dp, top = 12.dp, bottom = 12.dp))
    }
}
