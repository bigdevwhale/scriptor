package app.scriptor.domain

/**
 * Plain word-work for comparing a restoration with the page: no dictionary, just a rough stem
 * (the first letters) that is forgiving of Russian and English endings alike.
 */
object Words {
    private val word = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N}'’-]*")

    private val stop = setOf(
        // English
        "the", "and", "that", "this", "with", "from", "they", "them", "then", "than", "there", "their", "what", "when", "which",
        "will", "would", "could", "should", "have", "has", "had", "been", "being", "were", "was", "are", "you", "your", "for",
        "not", "but", "its", "it's", "can", "into", "onto", "about", "just", "only", "also", "very", "more", "most", "some",
        "such", "each", "every", "other", "over", "under", "does", "did", "done", "how", "why", "who", "whom", "where",
        "because", "while", "after", "before", "until", "again", "once", "here", "these", "those", "one", "all", "any",
        "you're", "you’re", "it’s", "that's", "that’s", "there's", "there’s", "don't", "don’t", "doesn't", "doesn’t", "isn't", "isn’t",
        "aren't", "aren’t", "won't", "won’t", "can't", "can’t", "they're", "they’re", "we're", "we’re", "i'm", "i’m", "what's", "what’s",
        // Russian
        "это", "этот", "эта", "эти", "того", "тому", "тот", "чтобы", "что", "как", "так", "когда", "если", "или", "только",
        "уже", "ещё", "еще", "тоже", "также", "очень", "потому", "поэтому", "чем", "где", "кто", "его", "её", "ее", "них",
        "они", "она", "оно", "мне", "меня", "мой", "моя", "свой", "своё", "свои", "себя", "себе", "был", "была", "было",
        "были", "будет", "есть", "нет", "для", "при", "про", "над", "под", "без", "после", "перед", "через", "между",
        "все", "всё", "всех", "весь", "вся", "каждый", "который", "которая", "которое", "которые", "можно", "нужно", "надо",
        "даже", "лишь", "вот", "там", "тут", "здесь", "сам", "сама", "само", "сами", "один", "одна", "одно",
    )

    fun tokens(text: String): List<String> = word.findAll(text).map { it.value }.toList()

    fun isContent(w: String): Boolean {
        val l = w.lowercase()
        return l.length > 3 && l !in stop && !l.all { it.isDigit() }
    }

    /** A rough stem: long enough to tell words apart, short enough to forgive endings. */
    fun stem(w: String): String {
        val l = w.lowercase().replace('ё', 'е').trimEnd('\'', '’', '-')
        return if (l.length <= 5) l else l.take(maxOf(5, l.length - 3).coerceAtMost(7))
    }

    fun stems(text: String): Set<String> = tokens(text).filter(::isContent).map(::stem).toSet()

    /** What the restoration caught of the page. */
    class Match(val caught: Set<String>, val missed: Set<String>) {
        val coverage: Float get() = if (caught.size + missed.size == 0) 1f else caught.size / (caught.size + missed.size).toFloat()
    }

    fun compare(page: String, attempt: String): Match {
        val p = stems(page)
        val a = stems(attempt)
        // An attempt word stands for a page word when one stem starts the other ("memor" and "memoriz").
        val caught = p.filter { s -> s in a || a.any { it.length >= 5 && (s.startsWith(it) || it.startsWith(s)) } }.toSet()
        return Match(caught, p - caught)
    }

    /** The grade the comparison suggests; the scribe has the last word. */
    fun suggest(m: Match): Grade = when {
        m.coverage < 0.3f -> Grade.AGAIN
        m.coverage < 0.55f -> Grade.HARD
        m.coverage < 0.85f -> Grade.GOOD
        else -> Grade.EASY
    }

    /**
     * Words the mice ate: about one content word in [every], the longest of each run, never two in a row.
     * Returns the character ranges to hide. Words of the golden line go first.
     */
    fun lacunae(text: String, gold: String = "", every: Int = 4, seed: Long = 0): List<IntRange> {
        val matches = word.findAll(text).filter { isContent(it.value) }.toList()
        if (matches.isEmpty()) return emptyList()
        val goldStems = stems(gold)
        val rnd = java.util.Random(seed)
        val out = mutableListOf<IntRange>()
        matches.chunked(every).forEach { run ->
            val pick = run.maxByOrNull { (if (stem(it.value) in goldStems) 100 else 0) + it.value.length + rnd.nextInt(3) } ?: return@forEach
            out += pick.range
        }
        return out
    }
}
