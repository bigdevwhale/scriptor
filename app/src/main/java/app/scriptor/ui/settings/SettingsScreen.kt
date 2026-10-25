package app.scriptor.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.scriptor.ui.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.scriptor.BuildConfig
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Backup
import app.scriptor.data.Settings
import app.scriptor.domain.Gender
import app.scriptor.notify.Hours
import app.scriptor.ui.Desk
import app.scriptor.ui.Go
import app.scriptor.ui.Language
import app.scriptor.ui.components.Chip
import app.scriptor.ui.components.CommunityLinks
import app.scriptor.ui.components.ConfirmDialog
import app.scriptor.ui.components.InfoRow
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.Rubric
import app.scriptor.ui.components.Screen
import app.scriptor.ui.components.SwitchRow
import app.scriptor.ui.components.TimeDialog
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(desk: Desk, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val s = desk.settings
    var status by remember { mutableStateOf<String?>(null) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var timeFor by remember { mutableIntStateOf(0) }
    fun set(change: (Settings) -> Settings) = c.write { settings.update(change) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        c.scope.launch {
            val dao = c.db.dao()
            val ok = runCatching {
                withContext(Dispatchers.IO) { Backup.write(context, uri, Backup.encode(dao.codices(), dao.pages(), dao.glosses(), dao.reviews(), dao.threads(), dao.loci())) }
            }.isSuccess
            status = context.getString(if (ok) R.string.backup_done else R.string.backup_failed)
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    Screen(stringResource(R.string.settings_title), go::back) {
        Rubric(stringResource(R.string.set_hours))
        Leaf {
            Text(stringResource(R.string.set_hours_hint), style = UType.Muted)
            SwitchRow(stringResource(R.string.matins), stringResource(R.string.set_matins_hint), s.matins, { on -> set { it.copy(matins = on) } })
            if (s.matins) InfoRow(Icons.Rounded.WbTwilight, stringResource(R.string.set_at), hm(s.matinsAt), tint = U.c.goldDeep, soft = U.c.goldSoft, onClick = { timeFor = 1 })
            SwitchRow(stringResource(R.string.compline), stringResource(R.string.set_compline_hint), s.compline, { on -> set { it.copy(compline = on) } })
            if (s.compline) {
                InfoRow(Icons.Rounded.Bedtime, stringResource(R.string.set_at), hm(s.complineAt), onClick = { timeFor = 2 })
                Text(stringResource(R.string.set_compline_size), style = UType.Body)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 5, 8).forEach { n -> Chip(n.toString(), s.complineSize == n, { set { it.copy(complineSize = n) } }) }
                }
            }
        }

        Rubric(stringResource(R.string.set_retention))
        Leaf {
            Text(stringResource(R.string.set_retention_hint), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.85f to R.string.retention_light, 0.9f to R.string.retention_usual, 0.95f to R.string.retention_strict).forEach { (r, label) ->
                    Chip(stringResource(label), kotlin.math.abs(s.retention - r) < 0.001f, { set { it.copy(retention = r) } })
                }
            }
        }

        Rubric(stringResource(R.string.set_language))
        Leaf {
            val cur = Language.current()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.lang_system), cur == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                Chip("English", cur == Language.EN, { Language.set(Language.EN) })
                Chip("Русский", cur == Language.RU, { Language.set(Language.RU) })
            }
            // Russian speaks to the scribe as "уверен" or "уверена"; English has no need to ask.
            if (LocalConfiguration.current.locales[0].language == "ru") {
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.set_gender), style = UType.Body)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.gender_male), s.gender == Gender.MALE, { set { it.copy(gender = Gender.MALE) } })
                    Chip(stringResource(R.string.gender_female), s.gender == Gender.FEMALE, { set { it.copy(gender = Gender.FEMALE) } })
                }
            }
        }

        Rubric(stringResource(R.string.set_backup))
        Leaf {
            Text(stringResource(R.string.set_backup_hint), style = UType.Muted)
            InfoRow(Icons.Rounded.Upload, stringResource(R.string.backup_export), stringResource(R.string.backup_export_hint), onClick = { export.launch("Scriptor.json") })
            InfoRow(Icons.Rounded.Download, stringResource(R.string.backup_import), stringResource(R.string.backup_import_hint), onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
            status?.let { Text(it, style = UType.Muted.copy(color = U.c.lapis)) }
        }

        CommunityLinks()

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.about, BuildConfig.VERSION_NAME), style = UType.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
    }

    if (timeFor != 0) {
        TimeDialog(if (timeFor == 1) s.matinsAt else s.complineAt, onDismiss = { timeFor = 0 }) { m ->
            if (timeFor == 1) set { it.copy(matinsAt = m) } else set { it.copy(complineAt = m) }
        }
    }

    restoreUri?.let { uri ->
        ConfirmDialog(
            stringResource(R.string.backup_import), stringResource(R.string.backup_import_confirm), stringResource(R.string.backup_import_do),
            onConfirm = {
                c.scope.launch {
                    val ok = runCatching {
                        withContext(Dispatchers.IO) {
                            val b = Backup.read(context, uri)
                            c.db.dao().replaceAll(b.codices, b.pages, b.glosses, b.reviews, b.threads, b.loci)
                        }
                    }.isSuccess
                    if (ok) c.settings.update { it.copy(onboarded = true) }
                    Hours.reschedule(context)
                    status = context.getString(if (ok) R.string.backup_restored else R.string.backup_bad)
                }
            },
            onDismiss = { restoreUri = null }, danger = true,
        )
    }
}

private fun hm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
