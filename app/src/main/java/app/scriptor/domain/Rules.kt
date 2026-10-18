package app.scriptor.domain

import app.scriptor.data.Gloss
import app.scriptor.data.GlossKind
import app.scriptor.data.Mode
import app.scriptor.data.Page
import app.scriptor.data.Review
import java.time.LocalDate
import java.time.ZoneId

/** The marks a page earns as it is understood, in the order a manuscript would gain them. */
data class Illumination(
    /** Restored from memory at least once: the first letter is painted. */
    val initial: Boolean,
    /** Two or more questions on the margin, in cinnabar. */
    val rubric: Boolean,
    /** The novice was answered, or two examples of one's own: vines climb the margin. */
    val vines: Boolean,
    /** Holds for half a year: gold leaf. */
    val gold: Boolean,
    /** Put to use in life: the wax seal. */
    val seal: Boolean,
) {
    val count: Int get() = listOf(initial, rubric, vines, gold, seal).count { it }
}

object Rules {
    fun illumination(p: Page, glosses: List<Gloss>): Illumination {
        val mine = glosses.filter { it.pageId == p.id }
        return Illumination(
            initial = p.reps > 0,
            rubric = p.cueList.size >= 2,
            vines = mine.any { it.kindOf == GlossKind.NOVICE } || mine.count { it.kindOf == GlossKind.EXEMPLUM } >= 2,
            gold = p.gilded,
            seal = mine.any { it.kindOf == GlossKind.SEAL },
        )
    }

    /** Pages whose ink has faded past the line, faintest first. */
    fun fading(pages: List<Page>, now: Long, retention: Double): List<Page> =
        pages.filter { it.due(retention) <= now }.sortedBy { it.recall(now) }

    /** Written yesterday: what Matins asks back. */
    fun yesterday(pages: List<Page>, today: LocalDate, zone: ZoneId): List<Page> =
        pages.filter { java.time.Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == today.minusDays(1) }

    /**
     * Each restoration asks in a different way, so the page is reached by more than one road:
     * rewrite it, fill the lacunae, answer the margin.
     */
    fun mode(p: Page, history: List<Review>): Mode {
        val n = history.count { it.pageId == p.id }
        val order = buildList {
            add(Mode.RECALL)
            if (p.cueList.isNotEmpty()) add(Mode.CUES)
            add(Mode.LACUNAE)
        }
        return order[n % order.size]
    }

    /** One of the novice's questions not yet answered on this page, or null when all are. */
    fun noviceQuestion(all: List<String>, answered: List<Gloss>, seed: Long): String? {
        val asked = answered.filter { it.kindOf == GlossKind.NOVICE }.map { it.prompt }.toSet()
        val left = all.filter { it !in asked }
        if (left.isEmpty()) return null
        return left[java.util.Random(seed).nextInt(left.size)]
    }

    /**
     * How often each number of candles was right: the scribe's eye. A well-calibrated eye
     * is right a third of the time on one candle, two thirds on two, nearly always on three.
     */
    fun calibration(reviews: List<Review>): List<Pair<Int, Float?>> = (1..3).map { c ->
        val staked = reviews.filter { it.confidence == c }
        c to if (staked.isEmpty()) null else staked.count { it.grade >= Grade.GOOD.n } / staked.size.toFloat()
    }

    val CALIBRATION_TARGET = mapOf(1 to 0.35f, 2 to 0.7f, 3 to 0.95f)

    /** Days in a row with at least one page written or restored, counting today only when it's done. */
    fun streak(days: Set<LocalDate>, today: LocalDate): Int {
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) {
            n++
            d = d.minusDays(1)
        }
        return n
    }
}
