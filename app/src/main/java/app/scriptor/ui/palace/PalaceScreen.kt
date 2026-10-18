package app.scriptor.ui.palace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Locus
import app.scriptor.data.Page
import app.scriptor.domain.Place
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.T
import app.scriptor.ui.components.ConfirmDialog
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PageRow
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.ScriptField
import app.scriptor.ui.components.SoftButton
import app.scriptor.ui.ctx
import app.scriptor.ui.stringResource
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/** How many pages one place holds before the walk gets crowded. */
private const val PER_PLACE = 3

/**
 * The monastery of memory, the method of loci: set the key ideas down in places you can walk through
 * in your mind (the gate, the refectory, the cell, the bell tower…), each with a vivid, odd picture,
 * and find them again on a walk.
 */
@Composable
fun PalaceScreen(desk: Desk, go: Go) {
    val c = ctx()
    var selected by rememberSaveable { mutableStateOf(Place.GATE.name) }
    val place = Place.of(selected)
    var picking by remember { mutableStateOf(false) }
    var placing by remember { mutableStateOf<Page?>(null) }
    val here = desk.lociOf(place)

    Screen(
        stringResource(R.string.palace_title), go::back,
        bottom = if (desk.loci.isEmpty()) null else {
            { PrimaryButton(stringResource(R.string.palace_walk), go::walk, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.DirectionsWalk) }
        },
    ) {
        Text(stringResource(R.string.palace_intro), style = UType.Italic)
        Spacer(Modifier.height(14.dp))
        Plan(desk, place) { selected = it.name }

        Rubric(T.place(c, place))
        Text(stringResource(R.string.palace_place_hint), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        if (here.isNotEmpty()) Leaf(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            here.forEach { l -> desk.page(l.pageId)?.let { LocusRow(l, it, desk, onOpen = { go.page(it.id) }) } }
        }
        Spacer(Modifier.height(10.dp))
        if (here.size < PER_PLACE) {
            SoftButton(stringResource(R.string.palace_place_page), { picking = true }, Modifier.fillMaxWidth(), color = U.c.cinnabar, soft = U.c.cinnabarSoft)
        } else Text(stringResource(R.string.palace_full), style = UType.Small)
    }

    if (picking) {
        val placed = desk.loci.map { it.pageId }.toSet()
        AlertDialog(
            onDismissRequest = { picking = false },
            containerColor = U.c.surface,
            title = { Text(stringResource(R.string.palace_pick), style = UType.Name) },
            text = {
                Column(Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
                    val free = desk.pages.filter { it.id !in placed }.sortedByDescending { it.createdAt }
                    if (free.isEmpty()) Text(stringResource(R.string.palace_pick_none), style = UType.Sub)
                    free.forEach { p -> PageRow(p, desk, { placing = p; picking = false }, showCodex = true) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
        )
    }
    placing?.let { p ->
        var image by remember(p.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { placing = null },
            containerColor = U.c.surface,
            title = { Text(stringResource(R.string.palace_image_title, T.place(c, place)), style = UType.Name.copy(color = U.c.cinnabar)) },
            text = {
                Column {
                    Text(p.title, style = UType.Italic)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.palace_image_hint), style = UType.Muted)
                    Spacer(Modifier.height(10.dp))
                    ScriptField(image, { image = it }, stringResource(R.string.palace_image_placeholder), style = UType.Script.copy(fontSize = 17.sp, lineHeight = 24.sp), minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    c.container.write { db.dao().insert(Locus(place = place.name, pageId = p.id, image = image.trim(), createdAt = System.currentTimeMillis())) }
                    placing = null
                }, enabled = image.isNotBlank()) { Text(stringResource(R.string.palace_set_down), color = U.c.cinnabar, style = UType.Button) }
            },
            dismissButton = { TextButton(onClick = { placing = null }) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
        )
    }
}

/**
 * The plan of the monastery from above: the wall, the cloister walk round the garden and its well,
 * the buildings in their places. Dots show how many pages each holds; the dashed line is the walk.
 */
@Composable
private fun Plan(desk: Desk, selected: Place, onSelect: (Place) -> Unit) {
    val c = U.c
    val ctx = ctx()
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(U.RMd)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val cell = w / 3
            // Grass beyond the walls, then the stone of the precinct.
            drawRect(Color(0xFF9DAA78))
            drawRoundRect(Color(0xFFCDBB97), Offset(w * 0.03f, w * 0.03f), Size(w * 0.94f, w * 0.94f), CornerRadius(w * 0.02f))
            drawRoundRect(Color(0xFF8C7656), Offset(w * 0.03f, w * 0.03f), Size(w * 0.94f, w * 0.94f), CornerRadius(w * 0.02f), style = Stroke(w * 0.012f))
            // The garden in the middle, with the well.
            drawRect(Brush.radialGradient(listOf(Color(0xFF8FA765), Color(0xFF6E8A4A)), Offset(w / 2, w / 2), cell * 0.8f), Offset(cell * 1.08f, cell * 1.08f), Size(cell * 0.84f, cell * 0.84f))
            drawLine(Color(0xFFD9CBA7), Offset(w / 2, cell * 1.08f), Offset(w / 2, cell * 1.92f), w * 0.012f)
            drawLine(Color(0xFFD9CBA7), Offset(cell * 1.08f, w / 2), Offset(cell * 1.92f, w / 2), w * 0.012f)
            // The cloister walk round it.
            drawRect(Color(0xFF7F6A4E), Offset(cell * 1.02f, cell * 1.02f), Size(cell * 0.96f, cell * 0.96f), style = Stroke(w * 0.014f))
            // Buildings: a roof on every other cell.
            Place.entries.filter { it != Place.GARDEN }.forEach { p ->
                val x = p.col * cell
                val y = p.row * cell
                val roof = when (p) {
                    Place.CHAPEL, Place.CRYPT, Place.BELLTOWER -> Color(0xFF7D4B3A)
                    Place.SCRIPTORIUM, Place.LIBRARY -> Color(0xFF6E5A80)
                    else -> Color(0xFF8D5E3C)
                }
                drawRoundRect(roof.copy(alpha = 0.9f), Offset(x + cell * 0.12f, y + cell * 0.12f), Size(cell * 0.76f, cell * 0.76f), CornerRadius(cell * 0.06f))
                drawRoundRect(Color.Black.copy(alpha = 0.15f), Offset(x + cell * 0.12f, y + cell * 0.12f), Size(cell * 0.76f, cell * 0.76f), CornerRadius(cell * 0.06f), style = Stroke(w * 0.006f))
                if (p == selected) drawRoundRect(c.gold, Offset(x + cell * 0.09f, y + cell * 0.09f), Size(cell * 0.82f, cell * 0.82f), CornerRadius(cell * 0.08f), style = Stroke(w * 0.012f))
            }
            if (selected == Place.GARDEN) drawRect(c.gold, Offset(cell * 1.06f, cell * 1.06f), Size(cell * 0.88f, cell * 0.88f), style = Stroke(w * 0.012f))
            // The walk, from the gate round the cloister into the garden.
            val pts = Place.entries.map { Offset((it.col + 0.5f) * cell, (it.row + 0.5f) * cell) }
            for (i in 1 until pts.size) {
                drawLine(c.cinnabar.copy(alpha = 0.55f), pts[i - 1], pts[i], w * 0.006f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.015f, w * 0.012f)))
            }
        }
        Column(Modifier.fillMaxSize()) {
            (0..2).forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    (0..2).forEach { col ->
                        val p = Place.at(col, row)!!
                        val n = desk.lociOf(p).size
                        Column(
                            Modifier.weight(1f).fillMaxSize().clickable { onSelect(p) }.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFF7EFDC).copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                                Icon(T.placeIcon(p), null, tint = c.ink, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                T.place(ctx, p), style = UType.Small.copy(color = Color(0xFFFBF6E9), fontSize = 11.5.sp, textAlign = TextAlign.Center),
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            if (n > 0) {
                                Spacer(Modifier.height(3.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(n) { Box(Modifier.size(7.dp).clip(CircleShape).background(c.gold).border(1.dp, Color(0xFF6B4A22), CircleShape)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocusRow(l: Locus, p: Page, desk: Desk, onOpen: () -> Unit) {
    val c = ctx()
    var confirm by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            desk.codex(p.codexId)?.let { Text(it.title, style = UType.Small) }
            Text(p.title, style = UType.Name.copy(fontSize = 17.sp))
            Text(l.image, style = UType.Italic.copy(fontSize = 15.sp, lineHeight = 20.sp, color = U.c.lapis))
        }
        IconButton(onClick = { confirm = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = U.c.faint, modifier = Modifier.size(16.dp)) }
    }
    if (confirm) {
        ConfirmDialog(
            stringResource(R.string.palace_remove), p.title, stringResource(R.string.delete),
            onConfirm = { c.container.write { db.dao().delete(l) } }, onDismiss = { confirm = false }, danger = true,
        )
    }
}
