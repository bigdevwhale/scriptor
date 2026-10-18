package app.scriptor.ui

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PestControlRodent
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import app.scriptor.R
import app.scriptor.data.GlossKind
import app.scriptor.data.Kind
import app.scriptor.data.Mode
import app.scriptor.domain.Craft
import app.scriptor.domain.Place
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Words and dates, in the app's language. Plain functions over a [Context] so receivers can use them too. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]

    fun local(ms: Long, zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(ms).atZone(zone)

    /** "17 Oct 2026". */
    fun date(c: Context, ms: Long): String = local(ms).format(DateTimeFormatter.ofPattern("d MMM yyyy", locale(c)))

    /** "17 Oct". */
    fun day(c: Context, ms: Long): String = local(ms).format(DateTimeFormatter.ofPattern("d MMM", locale(c)))

    /** "Saturday, 17 October". */
    fun longDay(c: Context, ms: Long): String =
        local(ms).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    fun kind(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.BOOK -> R.string.kind_book
            Kind.VIDEO -> R.string.kind_video
            Kind.ARTICLE -> R.string.kind_article
            Kind.PODCAST -> R.string.kind_podcast
            Kind.LECTURE -> R.string.kind_lecture
            Kind.COURSE -> R.string.kind_course
        }
    )

    fun kindIcon(k: Kind): ImageVector = when (k) {
        Kind.BOOK -> Icons.AutoMirrored.Rounded.MenuBook
        Kind.VIDEO -> Icons.Rounded.OndemandVideo
        Kind.ARTICLE -> Icons.AutoMirrored.Rounded.Article
        Kind.PODCAST -> Icons.Rounded.Podcasts
        Kind.LECTURE -> Icons.Rounded.School
        Kind.COURSE -> Icons.Rounded.Class
    }

    /** Placeholder for where in the source a page is from. */
    fun locatorHint(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.BOOK -> R.string.locator_book
            Kind.VIDEO, Kind.PODCAST, Kind.LECTURE -> R.string.locator_time
            Kind.ARTICLE -> R.string.locator_article
            Kind.COURSE -> R.string.locator_course
        }
    )

    fun mode(c: Context, m: Mode): String = c.getString(
        when (m) {
            Mode.RECALL -> R.string.mode_recall
            Mode.LACUNAE -> R.string.mode_lacunae
            Mode.CUES -> R.string.mode_cues
            Mode.MINIATURE -> R.string.mode_miniature
            Mode.PALACE -> R.string.mode_palace
            Mode.EXAM -> R.string.mode_exam
        }
    )

    fun gloss(c: Context, k: GlossKind): String = c.getString(
        when (k) {
            GlossKind.NOVICE -> R.string.gloss_novice
            GlossKind.EXEMPLUM -> R.string.gloss_exemplum
            GlossKind.PARABLE -> R.string.gloss_parable
            GlossKind.DISSENT -> R.string.gloss_dissent
            GlossKind.SEAL -> R.string.gloss_seal
            GlossKind.DISPUTE -> R.string.gloss_dispute
        }
    )

    /** "in 3 days", "in 2 months", "tomorrow": a span of [days] ahead, rounded the way people say it. */
    fun span(c: Context, days: Double): String {
        val r = c.resources
        return when {
            days < 0.75 -> c.getString(R.string.span_today)
            days < 1.5 -> c.getString(R.string.span_tomorrow)
            days < 30 -> days.roundToInt().let { r.getQuantityString(R.plurals.span_days, it, it) }
            days < 365 -> (days / 30.4).roundToInt().coerceAtLeast(1).let { r.getQuantityString(R.plurals.span_months, it, it) }
            else -> (days / 365).roundToInt().coerceAtLeast(1).let { r.getQuantityString(R.plurals.span_years, it, it) }
        }
    }

    /** The canonical hour it is now, from Matins before dawn to Compline at night. */
    fun hour(c: Context, ms: Long): String {
        val h = local(ms).hour
        return c.getString(
            when (h) {
                in 3..5 -> R.string.hour_matins
                in 6..8 -> R.string.hour_prime
                in 9..11 -> R.string.hour_terce
                in 12..14 -> R.string.hour_sext
                in 15..16 -> R.string.hour_none
                in 17..19 -> R.string.hour_vespers
                else -> R.string.hour_compline
            }
        )
    }

    fun place(c: Context, p: Place): String = c.getString(placeId(p))

    fun placeId(p: Place): Int = when (p) {
        Place.GATE -> R.string.place_gate
        Place.REFECTORY -> R.string.place_refectory
        Place.CELL -> R.string.place_cell
        Place.BELLTOWER -> R.string.place_belltower
        Place.CHAPEL -> R.string.place_chapel
        Place.CRYPT -> R.string.place_crypt
        Place.SCRIPTORIUM -> R.string.place_scriptorium
        Place.LIBRARY -> R.string.place_library
        Place.GARDEN -> R.string.place_garden
    }

    fun placeIcon(p: Place): ImageVector = when (p) {
        Place.GATE -> Icons.Rounded.DoorFront
        Place.REFECTORY -> Icons.Rounded.Restaurant
        Place.CELL -> Icons.Rounded.Bed
        Place.BELLTOWER -> Icons.Rounded.NotificationsActive
        Place.CHAPEL -> Icons.Rounded.Church
        Place.CRYPT -> Icons.Rounded.Stairs
        Place.SCRIPTORIUM -> Icons.Rounded.HistoryEdu
        Place.LIBRARY -> Icons.Rounded.LocalLibrary
        Place.GARDEN -> Icons.Rounded.Park
    }

    /** A craft's name, what it is for, and what opens it. */
    fun craftIds(k: Craft): Triple<Int, Int, Int> = when (k) {
        Craft.INK -> Triple(R.string.craft_ink, R.string.craft_ink_text, R.string.craft_ink_need)
        Craft.CINNABAR -> Triple(R.string.craft_cinnabar, R.string.craft_cinnabar_text, R.string.craft_cinnabar_need)
        Craft.NOVICE -> Triple(R.string.craft_novice, R.string.craft_novice_text, R.string.craft_novice_need)
        Craft.LACUNAE -> Triple(R.string.craft_lacunae, R.string.craft_lacunae_text, R.string.craft_lacunae_need)
        Craft.MARGINS -> Triple(R.string.craft_margins, R.string.craft_margins_text, R.string.craft_margins_need)
        Craft.MINIATURE -> Triple(R.string.craft_miniature, R.string.craft_miniature_text, R.string.craft_miniature_need)
        Craft.DISPUTATIO -> Triple(R.string.craft_disputatio, R.string.craft_disputatio_text, R.string.craft_disputatio_need)
        Craft.THREADS -> Triple(R.string.craft_threads, R.string.craft_threads_text, R.string.craft_threads_need)
        Craft.EXAM -> Triple(R.string.craft_exam, R.string.craft_exam_text, R.string.craft_exam_need)
        Craft.PALACE -> Triple(R.string.craft_palace, R.string.craft_palace_text, R.string.craft_palace_need)
    }

    fun craftIcon(k: Craft): ImageVector = when (k) {
        Craft.INK -> Icons.Rounded.Edit
        Craft.CINNABAR -> Icons.Rounded.FormatColorText
        Craft.NOVICE -> Icons.Rounded.ChildCare
        Craft.LACUNAE -> Icons.Rounded.PestControlRodent
        Craft.MARGINS -> Icons.AutoMirrored.Rounded.Notes
        Craft.MINIATURE -> Icons.Rounded.Brush
        Craft.DISPUTATIO -> Icons.Rounded.Gavel
        Craft.THREADS -> Icons.Rounded.Hub
        Craft.EXAM -> Icons.Rounded.Quiz
        Craft.PALACE -> Icons.Rounded.Church
    }

    fun percent(f: Double): String = "${(f * 100).roundToInt()}%"

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)
}

@Composable
fun ctx(): Context = LocalContext.current
