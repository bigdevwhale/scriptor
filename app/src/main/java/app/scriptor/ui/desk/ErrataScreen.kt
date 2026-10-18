package app.scriptor.ui.desk

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.scriptor.R
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.Source
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PageRow
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.components.Screen
import app.scriptor.ui.theme.UType

/** Erratum: pages forgotten while three candles were staked on them. Corrected, they hold best of all. */
@Composable
fun ErrataScreen(desk: Desk, go: Go) {
    Screen(
        stringResource(R.string.errata_title), go::back,
        bottom = if (desk.errata.isEmpty()) null else {
            { PrimaryButton(stringResource(R.string.errata_restore), { go.restore(Source.ERRATA) }, Modifier.fillMaxWidth()) }
        },
    ) {
        Text(stringResource(R.string.errata_intro), style = UType.Italic)
        Spacer(Modifier.height(14.dp))
        if (desk.errata.isEmpty()) {
            Text(stringResource(R.string.errata_empty), style = UType.Sub)
        } else Leaf {
            desk.errata.forEach { PageRow(it, desk, { go.page(it.id) }, showCodex = true) }
        }
    }
}
