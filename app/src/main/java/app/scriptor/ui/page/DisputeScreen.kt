package app.scriptor.ui.page

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Gloss
import app.scriptor.data.GlossKind
import app.scriptor.domain.Dispute
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.ctx
import app.scriptor.ui.stringResource
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/**
 * The disputation, as the schoolmen held it: state the thesis, give the strongest objections to it
 * (steelman them), then answer each. An idea that has survived its objections is understood.
 */
@Composable
fun DisputeScreen(pageId: Long, desk: Desk, go: Go) {
    val p = desk.page(pageId) ?: return
    val c = ctx()
    var thesis by remember { mutableStateOf(p.gold.ifBlank { p.title }) }
    val objections = remember { mutableStateListOf("", "", "") }
    val replies = remember { mutableStateListOf("", "", "") }
    val filled = (0 until 3).count { objections[it].isNotBlank() && replies[it].isNotBlank() }

    Screen(
        stringResource(R.string.dispute_title), go::back,
        bottom = {
            PrimaryButton(
                stringResource(R.string.dispute_save), {
                    val d = Dispute(thesis.trim(), (0 until 3).filter { objections[it].isNotBlank() && replies[it].isNotBlank() }.map { objections[it].trim() to replies[it].trim() })
                    c.container.write { db.dao().insert(Gloss(pageId = pageId, kind = GlossKind.DISPUTE.name, prompt = d.thesis, text = d.encode(), createdAt = System.currentTimeMillis())) }
                    go.back()
                },
                Modifier.fillMaxWidth(), enabled = thesis.isNotBlank() && filled >= 1, color = U.c.ink,
            )
        },
    ) {
        Text(stringResource(R.string.dispute_intro), style = UType.Italic)
        Rubric(stringResource(R.string.dispute_thesis))
        Leaf { ScriptField(thesis, { thesis = it.take(300) }, stringResource(R.string.dispute_thesis_hint), minLines = 2) }
        (0 until 3).forEach { i ->
            Rubric(stringResource(R.string.dispute_objection, i + 1))
            Leaf {
                ScriptField(objections[i], { objections[i] = it }, stringResource(R.string.dispute_objection_hint), style = UType.Italic.copy(fontSize = 18.sp, lineHeight = 26.sp), minLines = 2)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.dispute_reply), style = UType.Rubric)
                ScriptField(replies[i], { replies[i] = it }, stringResource(R.string.dispute_reply_hint), minLines = 2)
            }
        }
    }
}
