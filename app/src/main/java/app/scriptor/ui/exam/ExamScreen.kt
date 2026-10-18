package app.scriptor.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Mode
import app.scriptor.domain.Grade
import app.scriptor.domain.Rules
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.art.Candle
import app.scriptor.ui.art.Seal
import app.scriptor.ui.components.Bar
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.SoftButton
import app.scriptor.ui.ctx
import app.scriptor.ui.stringResource
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType
import kotlinx.coroutines.launch

private enum class Step { GUESS, ANSWER, MARK }

/**
 * The abbot's exam: margin questions from every codex at once, mixed. First say which codex the
 * question comes from (telling them apart is half of knowing), then answer it in your head, then see
 * the page and say honestly whether you knew. Each answer counts as a restoration of that page.
 */
@Composable
fun ExamScreen(desk: Desk, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val questions = remember { Rules.exam(desk.pages) }
    var index by remember { mutableIntStateOf(0) }
    var step by remember { mutableStateOf(Step.GUESS) }
    var guess by remember { mutableStateOf<Long?>(null) }
    var placed by remember { mutableIntStateOf(0) }
    var known by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    if (questions.isEmpty() || index >= questions.size) {
        Screen(stringResource(R.string.exam_title), go::back, bottom = { PrimaryButton(stringResource(R.string.done), go::back, Modifier.fillMaxWidth()) }) {
            Spacer(Modifier.height(24.dp))
            if (questions.isEmpty()) {
                Text(stringResource(R.string.exam_empty), style = UType.Sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                return@Screen
            }
            Seal(84.dp, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.exam_result, known, questions.size), style = UType.Title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.exam_placed, placed, questions.size), style = UType.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            val share = known / questions.size.toFloat()
            Leaf {
                Text(
                    stringResource(
                        when {
                            share >= 0.8f -> R.string.exam_verdict_high
                            share >= 0.5f -> R.string.exam_verdict_mid
                            else -> R.string.exam_verdict_low
                        }
                    ),
                    style = UType.Script.copy(fontSize = 18.sp),
                )
            }
        }
        return
    }

    val q = questions[index]
    val right = q.page.codexId
    Screen(stringResource(R.string.exam_title), go::back) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            questions.indices.forEach { i -> Candle(i >= index, Modifier.padding(horizontal = 1.dp), if (i == index) 34.dp else 26.dp) }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.exam_progress, index + 1, questions.size), style = UType.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Bar((index) / questions.size.toFloat(), color = U.c.gold, track = U.c.line)
        Spacer(Modifier.height(16.dp))
        Leaf {
            Text(stringResource(R.string.exam_asks).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(8.dp))
            Text("¶ " + q.cue, style = UType.Script.copy(fontSize = 22.sp, lineHeight = 29.sp, color = U.c.cinnabar))
        }

        Rubric(stringResource(R.string.exam_which))
        q.options.forEach { id ->
            val cx = desk.codex(id) ?: return@forEach
            val chosen = guess == id
            val reveal = step != Step.GUESS
            val color = when {
                reveal && id == right -> U.c.verdigris
                reveal && chosen -> U.c.cinnabar
                else -> U.c.line
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(U.RMd)
                    .background(if (reveal && (id == right || chosen)) color.copy(alpha = 0.12f) else U.c.surface)
                    .border(if (reveal && (id == right || chosen)) 2.dp else 1.dp, color, U.RMd)
                    .clickable(enabled = step == Step.GUESS) {
                        guess = id
                        if (id == right) placed++
                        step = Step.ANSWER
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(cx.title, style = UType.Name.copy(fontSize = 17.sp), modifier = Modifier.weight(1f))
                if (cx.author.isNotBlank()) Text(cx.author, style = UType.Small, maxLines = 1, modifier = Modifier.width(110.dp))
            }
        }

        when (step) {
            Step.GUESS -> Unit
            Step.ANSWER -> {
                Rubric(stringResource(R.string.exam_answer))
                Text(stringResource(R.string.exam_answer_hint), style = UType.Italic)
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.exam_show), { step = Step.MARK }, Modifier.fillMaxWidth())
            }
            Step.MARK -> {
                Rubric(q.page.title)
                Leaf {
                    if (q.page.gold.isNotBlank()) {
                        Text(q.page.gold, style = UType.Script.copy(color = U.c.goldDeep))
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(q.page.body, style = UType.Script.copy(fontSize = 17.sp, lineHeight = 24.sp))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false, true).forEach { knew ->
                        val label = stringResource(if (knew) R.string.exam_knew else R.string.exam_didnt)
                        val next = {
                            if (!busy) {
                                busy = true
                                scope.launch {
                                    c.container.scribe.restore(q.page.id, Mode.EXAM, if (knew) Grade.GOOD else Grade.AGAIN, 0, -1f, "")
                                    if (knew) known++
                                    if (index + 1 >= questions.size) {
                                        val score = known
                                        c.container.settings.update { it.copy(examAt = System.currentTimeMillis(), examScore = score, examOf = questions.size) }
                                    }
                                    guess = null
                                    step = Step.GUESS
                                    busy = false
                                    index++
                                }
                            }
                        }
                        if (knew) PrimaryButton(label, next, Modifier.weight(1f), color = U.c.verdigris)
                        else SoftButton(label, next, Modifier.weight(1f), color = U.c.cinnabar, soft = U.c.cinnabarSoft)
                    }
                }
            }
        }
    }
}
