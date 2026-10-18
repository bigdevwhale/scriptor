package app.scriptor.ui.restore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Mode
import app.scriptor.data.Page
import app.scriptor.domain.Grade
import app.scriptor.domain.Memory
import app.scriptor.domain.Rules
import app.scriptor.domain.Words
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.Source
import app.scriptor.ui.T
import app.scriptor.ui.art.Candle
import app.scriptor.ui.art.Fleuron
import app.scriptor.ui.art.Initial
import app.scriptor.ui.components.Bar
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.components.VoiceButton
import app.scriptor.ui.components.appendSpoken
import app.scriptor.ui.ctx
import app.scriptor.ui.page.gradeName
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType
import kotlinx.coroutines.launch

private enum class Step { ASK, STAKE, REVEAL }

private class Result(val page: Page, val grade: Grade, val erratum: Boolean)

/**
 * Restoring faded pages. Each page is asked in its turn's way (rewrite it, answer the margin, fill
 * the lacunae), then candles are staked on the answer, then the page is shown with what was missed
 * in cinnabar, and the scribe grades it. The grade bends the forgetting curve.
 */
@Composable
fun RestoreScreen(source: String, desk: Desk, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val queue = remember {
        when {
            source == Source.FADING -> desk.fading
            source == Source.COMPLINE -> desk.fading.take(desk.settings.complineSize)
            source == Source.MATINS -> desk.yesterday
            source == Source.ERRATA -> desk.errata
            source.startsWith("page-") -> listOfNotNull(desk.page(source.removePrefix("page-").toLongOrNull() ?: 0))
            source.startsWith("codex-") -> desk.pagesOf(source.removePrefix("codex-").toLongOrNull() ?: 0).sortedBy { it.recall(desk.now) }
            else -> emptyList()
        }.map { it.id }
    }
    var index by remember { mutableIntStateOf(0) }
    var step by remember { mutableStateOf(Step.ASK) }
    var attempt by remember { mutableStateOf("") }
    var confidence by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<Result>() }
    var coverage by remember { mutableFloatStateOf(-1f) }

    val title = stringResource(
        when (source) {
            Source.MATINS -> R.string.matins
            Source.COMPLINE -> R.string.compline
            Source.ERRATA -> R.string.errata_title
            else -> R.string.restore_title
        }
    )

    if (queue.isEmpty() || index >= queue.size) {
        Finished(title, results, queue.isEmpty(), go)
        return
    }
    val page = desk.page(queue[index])
    if (page == null) {
        // Deleted since the session began: skip it.
        androidx.compose.runtime.LaunchedEffect(index) { index++ }
        return
    }
    val mode = remember(index) { Rules.mode(page, desk.reviews) }
    val lacunae = remember(index) { Words.lacunae(page.body, page.gold, every = 4, seed = page.id + page.reps) }

    Screen(title, go::back) {
        // Where we are: a candle per page, snuffed as they are restored.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            queue.indices.take(9).forEach { i -> Candle(i >= index, Modifier.padding(horizontal = 3.dp), if (i == index) 40.dp else 32.dp) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.restore_progress, index + 1, queue.size) + " · " + T.mode(c, mode),
            style = UType.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))

        Leaf {
            desk.codex(page.codexId)?.let { Text(it.title.uppercase(), style = UType.Eyebrow) }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Initial(page.title.firstOrNull { it.isLetterOrDigit() } ?: '·', 46.dp, painted = page.reps > 0, gilded = page.gilded, seed = page.id.toInt())
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(page.title, style = UType.Name.copy(fontSize = 21.sp))
                    if (page.locator.isNotBlank()) Text(page.locator, style = UType.Italic.copy(fontSize = 14.sp))
                }
            }
            if (mode == Mode.CUES || (step == Step.REVEAL && page.cueList.isNotEmpty())) {
                Spacer(Modifier.height(12.dp))
                page.cueList.forEach { Text("¶ $it", style = UType.Rubric.copy(fontSize = 17.sp, lineHeight = 22.sp), modifier = Modifier.padding(bottom = 6.dp)) }
            }
            if (mode == Mode.LACUNAE && step != Step.REVEAL) {
                Spacer(Modifier.height(12.dp))
                Text(withLacunae(page.body, lacunae, U.c.cinnabar.copy(alpha = 0.55f)), style = UType.Script)
            }
            if (step == Step.REVEAL) {
                Spacer(Modifier.height(12.dp))
                Fleuron(Modifier.align(Alignment.CenterHorizontally).width(120.dp).height(12.dp))
                Spacer(Modifier.height(12.dp))
                val missed = if (attempt.isNotBlank()) Words.compare(page.whole, attempt).missed else emptySet()
                val mark = SpanStyle(color = U.c.cinnabar, background = U.c.cinnabarSoft)
                val body = when {
                    mode == Mode.LACUNAE -> revealLacunae(page.body, lacunae, mark)
                    missed.isNotEmpty() -> highlight(page.body, missed, mark)
                    else -> AnnotatedString(page.body)
                }
                Text(body, style = UType.Script)
                if (page.gold.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (missed.isNotEmpty()) highlight(page.gold, missed, mark) else AnnotatedString(page.gold),
                        style = UType.Script.copy(color = U.c.goldDeep),
                        modifier = Modifier.fillMaxWidth().clip(U.RSm).background(U.c.goldSoft.copy(alpha = 0.55f)).padding(10.dp),
                    )
                }
                if (page.colophon.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(if (missed.isNotEmpty()) highlight(page.colophon, missed, mark) else AnnotatedString(page.colophon), style = UType.Italic.copy(color = U.c.ink))
                }
            }
        }

        when (step) {
            Step.ASK -> {
                Rubric(stringResource(R.string.restore_task))
                Text(
                    stringResource(
                        when (mode) {
                            Mode.RECALL -> R.string.ask_recall
                            Mode.CUES -> R.string.ask_cues
                            Mode.LACUNAE -> R.string.ask_lacunae
                        }
                    ),
                    style = UType.Italic,
                )
                if (mode != Mode.LACUNAE) {
                    Spacer(Modifier.height(10.dp))
                    Leaf {
                        ScriptField(attempt, { attempt = it }, stringResource(if (mode == Mode.RECALL) R.string.recall_hint else R.string.cues_hint), minLines = if (mode == Mode.RECALL) 6 else 3)
                        Spacer(Modifier.height(6.dp))
                        VoiceButton({ attempt = appendSpoken(attempt, it) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    stringResource(if (attempt.isNotBlank()) R.string.restore_compare else R.string.restore_in_head),
                    { step = Step.STAKE }, Modifier.fillMaxWidth(),
                )
            }
            Step.STAKE -> {
                Rubric(stringResource(R.string.stake_title))
                Text(stringResource(R.string.stake_hint), style = UType.Italic)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to R.string.stake_1, 2 to R.string.stake_2, 3 to R.string.stake_3).forEach { (n, label) ->
                        Column(
                            Modifier.weight(1f).clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd)
                                .clickable {
                                    confidence = n
                                    coverage = if (attempt.isNotBlank()) Words.compare(page.whole, attempt).coverage else -1f
                                    step = Step.REVEAL
                                }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(Modifier.height(42.dp), verticalAlignment = Alignment.Bottom) { repeat(n) { Candle(true, Modifier.padding(horizontal = 1.dp), 38.dp) } }
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(label), style = UType.Small.copy(color = U.c.ink), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Step.REVEAL -> {
                if (coverage >= 0) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.coverage, T.percent(coverage.toDouble())), style = UType.Strong)
                    Spacer(Modifier.height(6.dp))
                    Bar(coverage, color = U.c.verdigris, track = U.c.line)
                }
                Rubric(stringResource(R.string.grade_title))
                val suggested = if (coverage >= 0) Words.suggest(Words.compare(page.whole, attempt)) else null
                val now = System.currentTimeMillis()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(listOf(Grade.AGAIN, Grade.HARD), listOf(Grade.GOOD, Grade.EASY)).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { g ->
                                val next = Memory.review(page.trace, g, now)
                                GradeButton(
                                    gradeName(g.n), T.span(c, Memory.interval(next.stability, desk.retention)), gradeColor(g), g == suggested,
                                    Modifier.weight(1f),
                                ) {
                                    if (busy) return@GradeButton
                                    busy = true
                                    scope.launch {
                                        c.container.scribe.restore(page.id, mode, g, confidence, coverage, attempt.trim())
                                        results += Result(page, g, g == Grade.AGAIN && confidence == 3)
                                        attempt = ""
                                        confidence = 0
                                        coverage = -1f
                                        step = Step.ASK
                                        busy = false
                                        index++
                                    }
                                }
                            }
                        }
                    }
                }
                if (confidence == 3) {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.stake_3_note), style = UType.Muted)
                }
            }
        }
    }
}

@Composable
private fun gradeColor(g: Grade): Color = when (g) {
    Grade.AGAIN -> U.c.cinnabar
    Grade.HARD -> U.c.goldDeep
    Grade.GOOD -> U.c.verdigris
    Grade.EASY -> U.c.lapis
}

@Composable
private fun GradeButton(label: String, next: String, color: Color, suggested: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(U.RMd)
            .background(if (suggested) color.copy(alpha = 0.16f) else U.c.surface)
            .border(if (suggested) 2.dp else 1.dp, if (suggested) color else U.c.line, U.RMd)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = UType.Button.copy(color = color))
        Text(next, style = UType.Small)
    }
}

/** The body with the eaten words drawn as gaps of the same length. */
private fun withLacunae(text: String, gaps: List<IntRange>, color: Color): AnnotatedString = buildAnnotatedString {
    var at = 0
    gaps.forEach { r ->
        append(text.substring(at, r.first))
        withStyle(SpanStyle(color = color, letterSpacing = 0.5.sp)) { append("_".repeat((r.last - r.first + 1).coerceIn(4, 12))) }
        at = r.last + 1
    }
    append(text.substring(at))
}

private fun revealLacunae(text: String, gaps: List<IntRange>, style: SpanStyle): AnnotatedString = buildAnnotatedString {
    var at = 0
    gaps.forEach { r ->
        append(text.substring(at, r.first))
        withStyle(style) { append(text.substring(r.first, r.last + 1)) }
        at = r.last + 1
    }
    append(text.substring(at))
}

/** The text with the words whose stems were missed marked out, so forgetting shows in cinnabar. */
private fun highlight(text: String, missed: Set<String>, style: SpanStyle): AnnotatedString = buildAnnotatedString {
    var at = 0
    Regex("[\\p{L}\\p{N}][\\p{L}\\p{N}'’-]*").findAll(text).forEach { m ->
        append(text.substring(at, m.range.first))
        if (Words.isContent(m.value) && Words.stem(m.value) in missed) withStyle(style) { append(m.value) } else append(m.value)
        at = m.range.last + 1
    }
    append(text.substring(at))
}

@Composable
private fun Finished(title: String, results: List<Result>, nothing: Boolean, go: Go) {
    Screen(title, go::back, bottom = { PrimaryButton(stringResource(R.string.done), go::back, Modifier.fillMaxWidth()) }) {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            repeat(results.size.coerceIn(1, 7)) { Candle(false, Modifier.padding(horizontal = 4.dp), 54.dp) }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (nothing) stringResource(R.string.restore_nothing) else pluralStringResource(R.plurals.restored, results.size, results.size),
            style = UType.Title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (nothing) R.string.restore_nothing_text else R.string.restore_done_text),
            style = UType.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        if (results.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Leaf {
                results.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.page.title, style = UType.Name.copy(fontSize = 17.sp), modifier = Modifier.weight(1f))
                        Text(gradeName(r.grade.n), style = UType.Button.copy(color = gradeColor(r.grade)))
                    }
                }
            }
            if (results.any { it.erratum }) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.cinnabarSoft).padding(14.dp)) {
                    Text(stringResource(R.string.restore_errata_note), style = UType.Body.copy(color = U.c.cinnabar))
                }
            }
        }
    }
}
