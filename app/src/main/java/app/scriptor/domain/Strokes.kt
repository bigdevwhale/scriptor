package app.scriptor.domain

/**
 * A miniature as text: strokes separated by ";", points by " ", each "x,y" in 0..1 of the square
 * it was drawn in. Small enough to live in a column and a backup.
 */
object Strokes {
    fun encode(strokes: List<List<Pair<Float, Float>>>): String =
        strokes.filter { it.isNotEmpty() }.joinToString(";") { s -> s.joinToString(" ") { (x, y) -> "%.3f,%.3f".format(java.util.Locale.ROOT, x, y) } }

    fun decode(text: String): List<List<Pair<Float, Float>>> =
        if (text.isBlank()) emptyList()
        else text.split(';').map { s ->
            s.trim().split(' ').mapNotNull { p ->
                val xy = p.split(',')
                val x = xy.getOrNull(0)?.toFloatOrNull()
                val y = xy.getOrNull(1)?.toFloatOrNull()
                if (x == null || y == null) null else x to y
            }
        }.filter { it.isNotEmpty() }
}
