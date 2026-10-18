package app.scriptor.ui.crafts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import app.scriptor.R
import app.scriptor.container
import app.scriptor.domain.Craft
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.T
import app.scriptor.ui.components.IconDisc
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.Screen
import app.scriptor.ui.ctx
import app.scriptor.ui.stringResource
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/** The crafts of the scriptorium: what is open, what each is for, and what opens the rest. */
@Composable
fun CraftsScreen(desk: Desk, go: Go) {
    val c = ctx()
    // Seen here, so none of the open ones needs announcing on the desk any more.
    LaunchedEffect(desk.crafts) {
        val names = desk.crafts.map { it.name }.toSet()
        if (!desk.settings.crafts.containsAll(names)) c.container.write { settings.update { it.copy(crafts = it.crafts + names) } }
    }
    Screen(stringResource(R.string.crafts_title), go::back) {
        Text(stringResource(R.string.crafts_intro), style = UType.Italic)
        Spacer(Modifier.height(14.dp))
        Craft.entries.forEach { k ->
            val open = desk.has(k)
            val (name, text, need) = T.craftIds(k)
            Leaf(Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.alpha(if (open) 1f else 0.45f)) { IconDisc(T.craftIcon(k), 42.dp, U.c.cinnabar, U.c.cinnabarSoft) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(name), style = UType.Name.copy(color = if (open) U.c.ink else U.c.muted))
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(text), style = UType.Muted)
                        if (!open) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, null, tint = U.c.faint, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(need), style = UType.Small)
                            }
                        }
                    }
                }
            }
        }
    }
}
