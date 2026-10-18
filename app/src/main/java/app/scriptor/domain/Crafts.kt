package app.scriptor.domain

/** What the scriptorium has done so far; crafts open as it grows. */
data class Progress(val pages: Int, val reviews: Int, val codices: Int)

/**
 * The crafts of the scriptorium, opened one by one so that a new scribe starts with ink alone and
 * each new technique arrives as a reward that explains itself.
 */
enum class Craft(val opens: (Progress) -> Boolean) {
    /** Writing pages in one's own words, and restoring them from memory. */
    INK({ true }),
    /** Margin questions, the golden line, the colophon; restoring by the margin. */
    CINNABAR({ it.pages >= 1 }),
    /** Explaining to the novice. */
    NOVICE({ it.pages >= 2 }),
    /** Restoring by filling the lacunae. */
    LACUNAE({ it.reviews >= 1 }),
    /** Examples, parables, disputes with the author, the seal of practice. */
    MARGINS({ it.pages >= 3 }),
    /** A drawing on the page, drawn again from memory. */
    MINIATURE({ it.pages >= 5 }),
    /** The scholastic disputation: thesis, objections, replies. */
    DISPUTATIO({ it.reviews >= 5 }),
    /** Red threads between pages of different codices. */
    THREADS({ it.codices >= 2 && it.pages >= 4 }),
    /** Mixed questions from every codex at once. */
    EXAM({ it.codices >= 2 && it.reviews >= 10 }),
    /** Pages set down in the places of a monastery and found again on a walk. */
    PALACE({ it.pages >= 8 });

    companion object {
        fun open(p: Progress): Set<Craft> = entries.filter { it.opens(p) }.toSet()
    }
}
