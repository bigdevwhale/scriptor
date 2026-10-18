package app.scriptor.ui

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Class
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
        }
    )

    fun gloss(c: Context, k: GlossKind): String = c.getString(
        when (k) {
            GlossKind.NOVICE -> R.string.gloss_novice
            GlossKind.EXEMPLUM -> R.string.gloss_exemplum
            GlossKind.PARABLE -> R.string.gloss_parable
            GlossKind.DISSENT -> R.string.gloss_dissent
            GlossKind.SEAL -> R.string.gloss_seal
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

    fun percent(f: Double): String = "${(f * 100).roundToInt()}%"

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)
}

@Composable
fun ctx(): Context = LocalContext.current
