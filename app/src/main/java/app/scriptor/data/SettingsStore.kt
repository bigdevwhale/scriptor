package app.scriptor.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** A page wants restoring when the chance of recalling it drops below this. */
    val retention: Float = 0.9f,
    /** Compline: a few fading pages before sleep, when memory sets best. */
    val compline: Boolean = true,
    val complineAt: Int = 21 * 60 + 30,
    /** Matins: yesterday's pages recalled in the morning. */
    val matins: Boolean = true,
    val matinsAt: Int = 8 * 60 + 30,
    /** Pages in an evening's restoration. */
    val complineSize: Int = 3,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        retention = this[RETENTION] ?: 0.9f,
        compline = this[COMPLINE] ?: true,
        complineAt = this[COMPLINE_AT] ?: (21 * 60 + 30),
        matins = this[MATINS] ?: true,
        matinsAt = this[MATINS_AT] ?: (8 * 60 + 30),
        complineSize = this[COMPLINE_SIZE] ?: 3,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[RETENTION] = s.retention
            it[COMPLINE] = s.compline
            it[COMPLINE_AT] = s.complineAt
            it[MATINS] = s.matins
            it[MATINS_AT] = s.matinsAt
            it[COMPLINE_SIZE] = s.complineSize
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val RETENTION = floatPreferencesKey("retention")
        val COMPLINE = booleanPreferencesKey("compline")
        val COMPLINE_AT = intPreferencesKey("compline_at")
        val MATINS = booleanPreferencesKey("matins")
        val MATINS_AT = intPreferencesKey("matins_at")
        val COMPLINE_SIZE = intPreferencesKey("compline_size")
    }
}
