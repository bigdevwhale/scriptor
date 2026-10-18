package app.scriptor.ui.palace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Mode
import app.scriptor.domain.Grade
import app.scriptor.domain.Place
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.T
import app.scriptor.ui.components.Bar
import app.scriptor.ui.components.IconDisc
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.SoftButton
import app.scriptor.ui.ctx
import app.scriptor.ui.pluralStringResource
import app.scriptor.ui.stringResource
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType
import kotlinx.coroutines.launch

/**
 * A walk through the monastery: place after place in the walk's order, recall the picture you left
 * there and the idea it stands for, then look. Each stop counts as a restoration of its page.
 */
@Composable
fun WalkScreen(desk: Desk, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val stops = remember { desk.loci.sortedWith(compareBy({ Place.of(it.place).ordinal }, { it.createdAt })) }
    var index by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf(false) }
    var found by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    if (index >= stops.size) {
        Screen(stringResource(R.string.walk_title), go::back, bottom = { PrimaryButton(stringResource(R.string.done), go::back, Modifier.fillMaxWidth()) }) {
            Spacer(Modifier.height(30.dp))
            Text(stringResource(R.string.walk_done, found, stops.size), style = UType.Title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.walk_done_text), style = UType.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        return
    }
    val l = stops[index]
    val page = desk.page(l.pageId)
    if (page == null) {
        LaunchedEffect(index) { index++ }
        return
    }
    val place = Place.of(l.place)

    Screen(stringResource(R.string.walk_title), go::back) {
        Bar(index / stops.size.toFloat(), color = U.c.gold, track = U.c.line)
        Spacer(Modifier.height(6.dp))
        Text(pluralStringResource(R.plurals.walk_left, stops.size - index, stops.size - index), style = UType.Small)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { IconDisc(T.placeIcon(place), 84.dp, U.c.ink, U.c.surface2) }
        Spacer(Modifier.height(12.dp))
        Text(T.place(c, place), style = UType.Display, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.walk_ask), style = UType.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        if (!shown) {
            PrimaryButton(stringResource(R.string.walk_look), { shown = true }, Modifier.fillMaxWidth())
        } else {
            Leaf {
                Text(l.image, style = UType.Script.copy(fontSize = 19.sp, color = U.c.lapis))
            }
            Rubric(page.title)
            Leaf {
                desk.codex(page.codexId)?.let { Text(it.title.uppercase(), style = UType.Eyebrow) }
                Spacer(Modifier.height(6.dp))
                Text(page.gold.ifBlank { page.body }, style = UType.Script.copy(fontSize = 17.sp, lineHeight = 24.sp, color = if (page.gold.isBlank()) U.c.ink else U.c.goldDeep))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(false, true).forEach { knew ->
                    val label = stringResource(if (knew) R.string.walk_found else R.string.walk_lost)
                    val next = {
                        if (!busy) {
                            busy = true
                            scope.launch {
                                c.container.scribe.restore(page.id, Mode.PALACE, if (knew) Grade.GOOD else Grade.AGAIN, 0, -1f, "")
                                if (knew) found++
                                shown = false
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
