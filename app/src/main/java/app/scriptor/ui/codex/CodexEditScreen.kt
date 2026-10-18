package app.scriptor.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.scriptor.ui.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Codex
import app.scriptor.data.Kind
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.T
import app.scriptor.ui.components.Chip
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.ctx
import app.scriptor.ui.library.Leathers
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType
import kotlinx.coroutines.launch

/**
 * A new codex, or its details changed. Before the first page: a preface of what is already known and
 * expected, and questions to put to the source. Both are asked back when the codex is closed.
 */
@Composable
fun CodexEditScreen(id: Long, desk: Desk, go: Go) {
    val c = ctx()
    val existing = remember { desk.codex(id) }
    var kind by remember { mutableStateOf(existing?.kindOf ?: Kind.BOOK) }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var author by remember { mutableStateOf(existing?.author.orEmpty()) }
    var preface by remember { mutableStateOf(existing?.preface.orEmpty()) }
    var questions by remember { mutableStateOf(existing?.questions.orEmpty()) }
    val scope = rememberCoroutineScope()
    var binding by remember { mutableIntStateOf(existing?.binding ?: (desk.codices.size % Leathers.size)) }

    Screen(
        stringResource(if (existing == null) R.string.codex_new_title else R.string.codex_edit_title), go::back,
        bottom = {
            PrimaryButton(
                stringResource(if (existing == null) R.string.codex_create else R.string.save),
                {
                    val container = c.container
                    scope.launch {
                        val cx = (existing ?: Codex(title = "", createdAt = System.currentTimeMillis())).copy(
                            kind = kind.name, title = title.trim(), author = author.trim(), preface = preface.trim(),
                            questions = questions.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n"), binding = binding,
                        )
                        if (existing == null) {
                            go.replaceWithCodex(container.db.dao().insert(cx))
                        } else {
                            container.db.dao().update(cx)
                            go.back()
                        }
                    }
                },
                Modifier.fillMaxWidth(), enabled = title.isNotBlank(),
            )
        },
    ) {
        Rubric(stringResource(R.string.codex_kind))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Kind.entries.forEach { k -> Chip(T.kind(c, k), kind == k, { kind = k }, color = U.c.cinnabar) }
        }
        Rubric(stringResource(R.string.codex_title_label))
        Leaf {
            ScriptField(title, { title = it.take(120) }, stringResource(R.string.codex_title_hint), style = UType.Name.copy(fontSize = 22.sp), single = true)
            Spacer(Modifier.height(10.dp))
            ScriptField(author, { author = it.take(80) }, stringResource(R.string.codex_author_hint), style = UType.Italic, single = true)
        }
        Rubric(stringResource(R.string.codex_binding))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Leathers.forEachIndexed { i, col ->
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(col)
                        .border(if (i == binding) 3.dp else 0.dp, if (i == binding) U.c.gold else col, CircleShape)
                        .clickable { binding = i },
                )
            }
        }
        if (existing == null || existing.preface.isNotBlank() || preface.isNotBlank()) {
            Rubric(stringResource(R.string.preface))
            Text(stringResource(R.string.preface_hint), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            Leaf { ScriptField(preface, { preface = it }, stringResource(R.string.preface_placeholder), minLines = 3) }
        }
        Rubric(stringResource(R.string.questions))
        Text(stringResource(R.string.questions_hint), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        Leaf { ScriptField(questions, { questions = it }, stringResource(R.string.questions_placeholder), minLines = 3) }
    }
}
