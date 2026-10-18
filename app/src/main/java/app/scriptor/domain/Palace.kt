package app.scriptor.domain

/**
 * The places of the monastery of memory, in the order a walk visits them: in at the gate, round the
 * cloister and into the garden with the well. [col] and [row] place each on a 3 x 3 plan.
 */
enum class Place(val col: Int, val row: Int) {
    GATE(1, 2), REFECTORY(0, 2), CELL(0, 1), BELLTOWER(0, 0), CHAPEL(1, 0), CRYPT(2, 0), SCRIPTORIUM(2, 1), LIBRARY(2, 2), GARDEN(1, 1);

    companion object {
        fun of(name: String): Place = entries.firstOrNull { it.name == name } ?: GATE
        fun at(col: Int, row: Int): Place? = entries.firstOrNull { it.col == col && it.row == row }
    }
}
