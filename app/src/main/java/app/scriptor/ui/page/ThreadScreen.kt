package app.scriptor.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Page
import app.scriptor.data.Thread
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PageRow
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.ctx
import app.scriptor.ui.stringResource
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/**
 * Tying a red thread from this page to another: the strongest ideas are the ones other books lead
 * back to. Pages of other codices come first, then the rest of this one.
 */
@Composable
fun ThreadScreen(pageId: Long, desk: Desk, go: Go) {
    val p = desk.page(pageId) ?: return
    val c = ctx()
    var query by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<Page?>(null) }
    val tied = desk.threadsOf(pageId).map { it.other(pageId) }.toSet()
    val candidates = desk.pages.filter { it.id != pageId && it.id !in tied }
        .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true) }

    Screen(stringResource(R.string.thread_title), go::back) {
        Text(stringResource(R.string.thread_intro, p.title), style = UType.Italic)
        Spacer(Modifier.height(12.dp))
        Leaf { ScriptField(query, { query = it }, stringResource(R.string.thread_search), style = UType.Script.copy(fontSize = 17.sp), single = true) }
        val (others, same) = candidates.partition { it.codexId != p.codexId }
        listOf(R.string.thread_other_codices to others, R.string.thread_same_codex to same).forEach { (label, list) ->
            if (list.isNotEmpty()) {
                Rubric(stringResource(label))
                Leaf(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    list.forEach { PageRow(it, desk, { target = it }, showCodex = true) }
                }
            }
        }
        if (candidates.isEmpty()) Text(stringResource(R.string.thread_none), style = UType.Sub)
    }

    target?.let { t ->
        var note by remember(t.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { target = null },
            containerColor = U.c.surface,
            title = { Text(stringResource(R.string.thread_note_title), style = UType.Name.copy(color = U.c.cinnabar)) },
            text = {
                Column {
                    Text("${p.title}  ⟷  ${t.title}", style = UType.Italic)
                    Spacer(Modifier.height(10.dp))
                    ScriptField(note, { note = it }, stringResource(R.string.thread_note_hint), style = UType.Script.copy(fontSize = 17.sp, lineHeight = 24.sp), minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    c.container.write { db.dao().insert(Thread(fromPage = pageId, toPage = t.id, note = note.trim(), createdAt = System.currentTimeMillis())) }
                    target = null
                    go.back()
                }) { Text(stringResource(R.string.thread_tie), color = U.c.cinnabar, style = UType.Button) }
            },
            dismissButton = { TextButton(onClick = { target = null }) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
        )
    }
}
