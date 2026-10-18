package app.scriptor.data

import app.scriptor.domain.Grade
import app.scriptor.domain.Memory

/** The writes that carry rules with them: a new page starts its trace, a restoration bends it. */
class Scribe(private val db: ScriptorDatabase) {
    private val dao get() = db.dao()

    suspend fun addPage(p: Page, now: Long = System.currentTimeMillis()): Long {
        val t = Memory.written(now)
        return dao.insert(p.copy(stability = t.stability, difficulty = t.difficulty, reviewedAt = t.reviewedAt, createdAt = now, updatedAt = now))
    }

    suspend fun restore(pageId: Long, mode: Mode, grade: Grade, confidence: Int, coverage: Float, attempt: String, now: Long = System.currentTimeMillis()) {
        val p = dao.page(pageId) ?: return
        val t = Memory.review(p.trace, grade, now)
        // Sure of it and wrong: the error that, once corrected, sticks best. Off the list once restored well.
        val erratum = when {
            grade == Grade.AGAIN && confidence == 3 -> true
            grade.n >= Grade.GOOD.n -> false
            else -> p.erratum
        }
        dao.update(
            p.copy(
                stability = t.stability, difficulty = t.difficulty, reviewedAt = t.reviewedAt,
                reps = p.reps + if (grade == Grade.AGAIN) 0 else 1,
                lapses = p.lapses + if (grade == Grade.AGAIN) 1 else 0,
                erratum = erratum,
            )
        )
        dao.insert(Review(pageId = pageId, at = now, mode = mode.name, confidence = confidence, grade = grade.n, coverage = coverage, attempt = attempt))
    }
}
