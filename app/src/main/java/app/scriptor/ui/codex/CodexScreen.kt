package app.scriptor.ui.codex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.scriptor.ui.pluralStringResource
import app.scriptor.ui.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Codex
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.Source
import app.scriptor.ui.T
import app.scriptor.ui.art.Fleuron
import app.scriptor.ui.components.ConfirmDialog
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PageRow
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.SoftButton
import app.scriptor.ui.ctx
import app.scriptor.ui.library.leather
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

@Composable
fun CodexScreen(id: Long, desk: Desk, go: Go) {
    val cx = desk.codex(id) ?: return
    val c = ctx()
    val pages = desk.pagesOf(id)
    var deleting by remember { mutableStateOf(false) }

    Screen(
        "", go::back,
        actions = {
            IconButton(onClick = { go.editCodex(id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.muted) }
            IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = U.c.muted) }
        },
        bottom = if (cx.closed) null else {
            { PrimaryButton(stringResource(R.string.write_page), { go.write(id) }, Modifier.fillMaxWidth(), color = U.c.cinnabar, icon = Icons.Rounded.Add) }
        },
    ) {
        Cover(cx, desk)

        if (cx.closed) {
            Rubric(stringResource(R.string.colophon_of_codex))
            Leaf {
                Text(cx.sentence, style = UType.Script)
                if (cx.words.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Fleuron(Modifier.align(Alignment.CenterHorizontally).width(120.dp).height(12.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(cx.words.uppercase(), style = UType.Eyebrow.copy(fontSize = 15.sp, color = U.c.goldDeep), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                if (cx.surprise.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.surprise_label), style = UType.Rubric)
                    Text(cx.surprise, style = UType.Script.copy(fontSize = 17.sp))
                }
            }
        }

        if (cx.preface.isNotBlank()) {
            Rubric(stringResource(R.string.preface))
            Leaf { Text(cx.preface, style = UType.Italic.copy(color = U.c.ink)) }
        }

        if (cx.questionList.isNotEmpty()) {
            Rubric(stringResource(R.string.questions))
            Leaf(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                cx.questionList.forEachIndexed { i, q ->
                    val done = cx.isAnswered(i)
                    Row(
                        Modifier.fillMaxWidth().clip(U.RSm).clickable {
                            c.container.write { db.dao().update(cx.copy(answered = cx.answered xor (1 shl i))) }
                        }.padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(if (done) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null, tint = if (done) U.c.verdigris else U.c.faint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(q, style = UType.Script.copy(fontSize = 17.sp, lineHeight = 22.sp, color = if (done) U.c.muted else U.c.ink), modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Rubric(pluralStringResource(R.plurals.pages, pages.size, pages.size))
        if (pages.isEmpty()) {
            Text(stringResource(R.string.codex_no_pages), style = UType.Italic)
        } else {
            Leaf(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                pages.forEach { PageRow(it, desk, { go.page(it.id) }) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton(stringResource(R.string.restore_codex), { go.restore(Source.codex(id)) }, Modifier.weight(1f), icon = Icons.Rounded.Replay)
                if (!cx.closed) SoftButton(
                    stringResource(R.string.close_codex), { go.closeCodex(id) }, Modifier.weight(1f),
                    color = U.c.wax, soft = U.c.cinnabarSoft, icon = Icons.Rounded.Lock,
                )
            }
        }
    }

    if (deleting) {
        ConfirmDialog(
            stringResource(R.string.codex_delete_title), pluralStringResource(R.plurals.codex_delete_text, pages.size, pages.size), stringResource(R.string.delete),
            onConfirm = {
                c.container.write { db.dao().deleteCodex(cx) }
                go.back()
            },
            onDismiss = { deleting = false }, danger = true,
        )
    }
}

/** The front board: leather, a gold frame, the title and author tooled in. */
@Composable
private fun Cover(cx: Codex, desk: Desk) {
    val c = ctx()
    val col = leather(cx.binding)
    val pages = desk.pagesOf(cx.id)
    Box(Modifier.fillMaxWidth().wrapContentHeight().clip(U.RMd)) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(listOf(lerp(col, Color.White, 0.12f), col, lerp(col, Color.Black, 0.3f)), Offset.Zero, Offset(size.width, size.height)))
            val i = 10.dp.toPx()
            drawRect(U_GOLD.copy(alpha = 0.85f), Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
            val j = 15.dp.toPx()
            drawRect(U_GOLD.copy(alpha = 0.5f), Offset(j, j), Size(size.width - 2 * j, size.height - 2 * j), style = androidx.compose.ui.graphics.drawscope.Stroke(0.8.dp.toPx()))
            listOf(Offset(i, i), Offset(size.width - i, i), Offset(i, size.height - i), Offset(size.width - i, size.height - i)).forEach {
                drawCircle(U_GOLD, 3.dp.toPx(), it)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(T.kindIcon(cx.kindOf), null, tint = U_GOLD_SOFT.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(T.kind(c, cx.kindOf).uppercase(), style = UType.Eyebrow.copy(color = U_GOLD_SOFT.copy(alpha = 0.8f)))
            }
            Spacer(Modifier.height(10.dp))
            Text(cx.title, style = UType.Display.copy(color = U_GOLD_SOFT, textAlign = TextAlign.Center, fontSize = 29.sp, lineHeight = 33.sp))
            if (cx.author.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(cx.author, style = UType.Italic.copy(color = U_GOLD_SOFT.copy(alpha = 0.85f), textAlign = TextAlign.Center))
            }
            Spacer(Modifier.height(14.dp))
            val ink = if (pages.isEmpty()) null else pages.map { it.recall(desk.now) }.average()
            Text(
                listOfNotNull(
                    stringResource(R.string.begun, T.date(c, cx.createdAt)),
                    ink?.let { stringResource(R.string.ink_left, T.percent(it)) },
                ).joinToString(" · "),
                style = UType.Small.copy(color = U_GOLD_SOFT.copy(alpha = 0.7f)),
            )
        }
    }
}

private val U_GOLD = Color(0xFFCDA33E)
private val U_GOLD_SOFT = Color(0xFFF3E3AE)
