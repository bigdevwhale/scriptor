package app.scriptor.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.scriptor.ui.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.data.Page
import app.scriptor.domain.Illumination
import app.scriptor.ui.Desk
import app.scriptor.ui.T
import app.scriptor.ui.art.Initial
import app.scriptor.ui.art.inkFor
import app.scriptor.ui.art.parchment
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/** A full screen on the desk: parchment, a top bar, and a scrolling column. */
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().parchment(U.c.bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(title, onBack, actions)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            content()
            Spacer(Modifier.height(32.dp))
        }
        if (bottom != null) Column(Modifier.fillMaxWidth().background(U.c.bg).padding(horizontal = 16.dp, vertical = 10.dp), content = bottom)
    }
}

/** A leaf of vellum on the desk. */
@Composable
fun Leaf(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(18.dp), onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(U.RSm)
            .parchment(U.c.vellum, edge = 0.06f)
            .border(1.dp, U.c.line, U.RSm)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** A heading in rubric: small capitals in cinnabar, as a scribe marks a new part. */
@Composable
fun Rubric(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = UType.Eyebrow, modifier = modifier.padding(start = 2.dp, top = 22.dp, bottom = 8.dp))
}

/**
 * A field that looks like writing on the page rather than a form: a ruled line under it, the book
 * hand, the placeholder in faded ink.
 */
@Composable
fun ScriptField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: TextStyle = UType.Script,
    minLines: Int = 1,
    single: Boolean = false,
) {
    val c = U.c
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = style,
        singleLine = single,
        minLines = minLines,
        cursorBrush = SolidColor(c.cinnabar),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawRules(c.line, style)
                    .padding(vertical = 4.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = c.faint))
                inner()
            }
        },
    )
}

/** Faint ruling, one line per line of text, as pricked and ruled on a leaf before writing. */
private fun Modifier.drawRules(color: Color, style: TextStyle) = drawBehind {
    val step = style.lineHeight.toPx().takeIf { it > 0 } ?: 26.dp.toPx()
    var y = 4.dp.toPx() + step - 3.dp.toPx()
    while (y <= size.height) {
        drawLine(color.copy(alpha = 0.7f), Offset(0f, y), Offset(size.width, y), 0.8.dp.toPx())
        y += step
    }
}

/** The phone's own speech recogniser: speak, and the words are handed back. Hidden when there is none. */
@Composable
fun VoiceButton(onText: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val intent = remember(locale) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
    }
    val available = remember { intent.resolveActivity(context.packageManager) != null }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onText)
        }
    }
    if (!available) return
    Row(
        modifier.clip(CircleShape).background(U.c.cinnabarSoft).clickable {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_prompt))
            runCatching { launcher.launch(intent) }
        }.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Mic, null, Modifier.size(17.dp), tint = U.c.cinnabar)
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.voice), style = UType.Small.copy(color = U.c.cinnabar))
    }
}

/** Appends spoken words to what is written, with a space between. */
fun appendSpoken(text: String, spoken: String): String =
    if (text.isBlank()) spoken.replaceFirstChar { it.uppercase() } else text.trimEnd() + " " + spoken

/** A row in a list of pages: its initial, title, the first line in its present ink, and its marks. */
@Composable
fun PageRow(p: Page, desk: Desk, onClick: () -> Unit, showCodex: Boolean = false) {
    val ill = desk.illumination(p)
    val recall = p.recall(desk.now)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Initial(p.title.firstOrNull { it.isLetterOrDigit() } ?: '·', 44.dp, painted = ill.initial, gilded = ill.gold, seed = p.id.toInt())
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (showCodex) desk.codex(p.codexId)?.let { Text(it.title, style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Text(p.title, style = UType.Name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                p.body.lineSequence().firstOrNull().orEmpty(),
                style = UType.Script.copy(fontSize = 15.sp, lineHeight = 19.sp, color = inkFor(U.c, recall)),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Marks(ill)
            Spacer(Modifier.height(4.dp))
            Text(if (p.gilded) "∞" else T.percent(recall), style = UType.Small.copy(color = if (p.gilded) U.c.goldDeep else inkFor(U.c, recall)))
        }
    }
}

/** The marks a page has earned, as small dots of their colours: rubric, vines, gold, seal. */
@Composable
fun Marks(ill: Illumination, dot: Dp = 7.dp) {
    val c = U.c
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(ill.rubric to c.cinnabar, ill.vines to c.verdigris, ill.gold to c.gold, ill.seal to c.wax).forEach { (on, col) ->
            Box(Modifier.size(dot).clip(CircleShape).background(if (on) col else c.line))
        }
    }
}

/** A thin ink gauge: how much of the page's ink is left. */
@Composable
fun InkGauge(recall: Double, modifier: Modifier = Modifier, gilded: Boolean = false) {
    val c = U.c
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val r = size.height / 2
        drawRoundRect(c.line, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
        val f = if (gilded) 1f else recall.toFloat().coerceIn(0f, 1f)
        drawRoundRect(
            if (gilded) c.gold else inkFor(c, recall),
            size = androidx.compose.ui.geometry.Size(size.width * f, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
        )
    }
}
