package app.scriptor.ui.codex

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.domain.Words
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.ctx
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/**
 * Closing a codex is a compression: the whole of it in one sentence, then in three words for the
 * spine, and a look back at the preface to see what turned out otherwise.
 */
@Composable
fun CloseScreen(id: Long, desk: Desk, go: Go) {
    val cx = desk.codex(id) ?: return
    val c = ctx()
    var sentence by remember { mutableStateOf(cx.sentence) }
    var words by remember { mutableStateOf(cx.words) }
    var surprise by remember { mutableStateOf(cx.surprise) }
    val three = Words.tokens(words).size

    Screen(
        stringResource(R.string.close_title), go::back,
        bottom = {
            PrimaryButton(
                stringResource(R.string.close_do),
                {
                    c.container.write {
                        db.dao().update(cx.copy(sentence = sentence.trim(), words = words.trim(), surprise = surprise.trim(), closedAt = System.currentTimeMillis()))
                    }
                    go.back()
                },
                Modifier.fillMaxWidth(), enabled = sentence.isNotBlank(), color = U.c.wax,
            )
        },
    ) {
        Text(stringResource(R.string.close_intro), style = UType.Italic)

        Rubric(stringResource(R.string.close_sentence))
        Leaf { ScriptField(sentence, { sentence = it.take(400) }, stringResource(R.string.close_sentence_hint), minLines = 2) }

        Rubric(stringResource(R.string.close_words))
        Leaf { ScriptField(words, { words = it.take(60) }, stringResource(R.string.close_words_hint), single = true) }
        if (three > 3) Text(stringResource(R.string.close_words_more), style = UType.Muted.copy(color = U.c.cinnabar))

        Rubric(stringResource(R.string.close_surprise))
        if (cx.preface.isNotBlank()) {
            Text(stringResource(R.string.close_you_wrote), style = UType.Muted)
            Spacer(Modifier.height(6.dp))
            Leaf { Text(cx.preface, style = UType.Italic.copy(color = U.c.ink)) }
            Spacer(Modifier.height(10.dp))
        }
        Leaf { ScriptField(surprise, { surprise = it }, stringResource(R.string.close_surprise_hint), minLines = 2) }
        if (cx.questionList.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val open = cx.questionList.indices.count { !cx.isAnswered(it) }
            if (open > 0) Text(stringResource(R.string.close_open_questions, open), style = UType.Muted)
        }
    }
}
