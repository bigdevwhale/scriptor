package app.scriptor.ui.page

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Page
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.T
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.components.VoiceButton
import app.scriptor.ui.components.appendSpoken
import app.scriptor.ui.ctx
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType
import kotlinx.coroutines.launch

/** Longer than this arriving in one go is a paste, not typing. */
private const val PASTE = 40

/**
 * Writing a page: the body in one's own words from memory, the Cornell margin of questions, the
 * golden line and the colophon. Pasted text is the author's ink, not the scribe's: it is set aside
 * as a quotation, and the body stays the scribe's own.
 */
@Composable
fun PageEditScreen(codexId: Long, pageId: Long, desk: Desk, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val existing = remember { desk.page(pageId) }
    val codex = desk.codex(existing?.codexId ?: codexId) ?: return
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var locator by remember { mutableStateOf(existing?.locator.orEmpty()) }
    var body by remember { mutableStateOf(existing?.body.orEmpty()) }
    var quote by remember { mutableStateOf(existing?.quote.orEmpty()) }
    var cues by remember { mutableStateOf(existing?.cues.orEmpty()) }
    var gold by remember { mutableStateOf(existing?.gold.orEmpty()) }
    var colophon by remember { mutableStateOf(existing?.colophon.orEmpty()) }
    var borrowed by remember { mutableStateOf(false) }

    fun onBody(new: String) {
        val old = body
        if (new.length - old.length >= PASTE) {
            val pre = old.commonPrefixWith(new).length
            val suf = old.substring(pre).commonSuffixWith(new.substring(pre)).length
            val inserted = new.substring(pre, new.length - suf).trim()
            if (inserted.length >= PASTE) {
                quote = if (quote.isBlank()) inserted else quote.trimEnd() + "\n\n" + inserted
                borrowed = true
                return
            }
        }
        body = new
    }

    Screen(
        stringResource(if (existing == null) R.string.page_new_title else R.string.page_edit_title), go::back,
        bottom = {
            PrimaryButton(
                stringResource(if (existing == null) R.string.page_bind else R.string.save),
                {
                    val container = c.container
                    val clean = { s: String -> s.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n") }
                    scope.launch {
                        if (existing == null) {
                            val id = container.scribe.addPage(
                                Page(
                                    codexId = codex.id, title = title.trim(), locator = locator.trim(), body = body.trim(), quote = quote.trim(),
                                    cues = clean(cues), gold = gold.trim(), colophon = colophon.trim(),
                                    stability = 0.0, difficulty = 0.0, reviewedAt = 0L, createdAt = 0L,
                                )
                            )
                            go.replaceWithPage(id)
                        } else {
                            container.db.dao().update(
                                existing.copy(
                                    title = title.trim(), locator = locator.trim(), body = body.trim(), quote = quote.trim(),
                                    cues = clean(cues), gold = gold.trim(), colophon = colophon.trim(), updatedAt = System.currentTimeMillis(),
                                )
                            )
                            go.back()
                        }
                    }
                },
                Modifier.fillMaxWidth(), enabled = title.isNotBlank() && body.isNotBlank(), color = U.c.cinnabar,
            )
        },
    ) {
        Text(codex.title.uppercase(), style = UType.Eyebrow)
        if (existing == null) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.page_rule), style = UType.Italic)
        }
        Spacer(Modifier.height(14.dp))
        Leaf {
            ScriptField(title, { title = it.take(100) }, stringResource(R.string.page_title_hint), style = UType.Name.copy(fontSize = 22.sp), single = true)
            Spacer(Modifier.height(6.dp))
            ScriptField(locator, { locator = it.take(40) }, T.locatorHint(c, codex.kindOf), style = UType.Italic.copy(fontSize = 15.sp), single = true)
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Rubric(stringResource(R.string.page_body), Modifier.weight(1f))
            VoiceButton({ body = appendSpoken(body, it) })
        }
        Leaf { ScriptField(body, ::onBody, stringResource(R.string.page_body_hint), minLines = 7) }
        if (borrowed) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.page_borrowed), style = UType.Muted.copy(color = U.c.cinnabar))
        }

        if (quote.isNotBlank() || borrowed) {
            Rubric(stringResource(R.string.page_quote))
            Leaf { ScriptField(quote, { quote = it }, stringResource(R.string.page_quote_hint), style = UType.Italic.copy(color = U.c.faint), minLines = 2) }
        }

        Rubric(stringResource(R.string.page_cues))
        Text(stringResource(R.string.page_cues_hint), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        Leaf { ScriptField(cues, { cues = it }, stringResource(R.string.page_cues_placeholder), style = UType.Rubric.copy(fontSize = 18.sp, lineHeight = 26.sp), minLines = 2) }

        Rubric(stringResource(R.string.page_gold))
        Text(stringResource(R.string.page_gold_hint), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        Leaf { ScriptField(gold, { gold = it.take(200) }, stringResource(R.string.page_gold_placeholder), style = UType.Script.copy(color = U.c.goldDeep)) }

        Rubric(stringResource(R.string.page_colophon))
        Text(stringResource(R.string.page_colophon_hint), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        Leaf { ScriptField(colophon, { colophon = it.take(400) }, stringResource(R.string.page_colophon_placeholder), minLines = 2) }
    }
}

