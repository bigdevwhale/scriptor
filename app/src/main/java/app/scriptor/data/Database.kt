package app.scriptor.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import app.scriptor.domain.Memory
import app.scriptor.domain.Trace
import kotlinx.coroutines.flow.Flow

/** What was studied: a book, a video, a lecture. Its pages are bound into one codex. */
enum class Kind { BOOK, VIDEO, ARTICLE, PODCAST, LECTURE, COURSE }

/** Notes in the margin of a page, each from a different craft. */
enum class GlossKind {
    /** An answer to the novice's question. */
    NOVICE,
    /** An example of one's own, from life rather than the book. */
    EXEMPLUM,
    /** "It's like…": the idea told through something else. */
    PARABLE,
    /** Where the scribe disagrees with the author. */
    DISSENT,
    /** The idea was put to use: what happened. */
    SEAL,
    /** A scholastic disputation: the thesis in [Gloss.prompt], objections and replies in [Gloss.text] (see [app.scriptor.domain.Dispute]). */
    DISPUTE,
}

@Entity(tableName = "codices")
data class Codex(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [Kind] name. */
    val kind: String = Kind.BOOK.name,
    val title: String,
    val author: String = "",
    /** Written before starting: what I already know and what I expect. */
    val preface: String = "",
    /** Questions to the book, one per line, asked before reading. */
    val questions: String = "",
    /** Bit i is set when question i found its answer. */
    val answered: Int = 0,
    /** The whole codex in one sentence, written on closing it. */
    val sentence: String = "",
    /** …and then in three words, tooled on the spine. */
    val words: String = "",
    /** What turned out unlike the preface. */
    val surprise: String = "",
    /** Colour of the binding, an index into the leathers. */
    val binding: Int = 0,
    val createdAt: Long,
    val closedAt: Long? = null,
) {
    val kindOf: Kind get() = runCatching { Kind.valueOf(kind) }.getOrDefault(Kind.BOOK)
    val questionList: List<String> get() = questions.lines().map { it.trim() }.filter { it.isNotEmpty() }
    fun isAnswered(i: Int) = answered and (1 shl i) != 0
    val closed: Boolean get() = closedAt != null
}

/**
 * One page of a codex, laid out the Cornell way: the body in the scribe's own words, questions on the
 * margin, the colophon at the foot. Its memory trace says how far the ink has faded.
 */
@Entity(tableName = "pages", indices = [Index("codexId")])
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codexId: Long,
    val title: String,
    /** Page, chapter or timecode in the source. */
    val locator: String = "",
    /** In the scribe's own words. */
    val body: String,
    /** Questions on the margin, one per line; the body answers them. */
    val cues: String = "",
    /** The essence, laid in gold: one line at most. */
    val gold: String = "",
    /** One or two sentences at the foot. */
    val colophon: String = "",
    /** Borrowed ink: the author's words, kept apart from the scribe's. */
    val quote: String = "",
    /** A miniature drawn by hand: strokes in [app.scriptor.domain.Strokes] form, or empty. */
    @ColumnInfo(defaultValue = "") val miniature: String = "",
    val stability: Double,
    val difficulty: Double,
    val reviewedAt: Long,
    /** Restorations that held, and those that didn't. */
    val reps: Int = 0,
    val lapses: Int = 0,
    /** Forgotten while sure of it: goes in the book of errors until restored well. */
    val erratum: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
) {
    val trace: Trace get() = Trace(stability, difficulty, reviewedAt)
    val cueList: List<String> get() = cues.lines().map { it.trim() }.filter { it.isNotEmpty() }
    fun recall(now: Long): Double = if (gilded) 1.0 else Memory.recall(trace, now)
    fun due(retention: Double): Long = if (gilded) Long.MAX_VALUE else Memory.due(trace, retention)
    val gilded: Boolean get() = stability >= Memory.GOLD_DAYS
    /** Everything on the page that a restoration is compared with. */
    val whole: String get() = listOf(body, gold, colophon).filter { it.isNotBlank() }.joinToString("\n")
}

@Entity(tableName = "glosses", indices = [Index("pageId")])
data class Gloss(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Long,
    /** [GlossKind] name. */
    val kind: String,
    /** The question it answers, for the novice's glosses. */
    val prompt: String = "",
    val text: String,
    val createdAt: Long,
) {
    val kindOf: GlossKind get() = runCatching { GlossKind.valueOf(kind) }.getOrDefault(GlossKind.EXEMPLUM)
}

/** How a restoration went. */
enum class Mode {
    /** Rewritten from memory, then compared. */
    RECALL,
    /** Words eaten by mice, filled in. */
    LACUNAE,
    /** Only the margin questions showing. */
    CUES,
    /** The miniature drawn again from memory. */
    MINIATURE,
    /** A walk through the monastery of memory. */
    PALACE,
    /** A question at the abbot's exam. */
    EXAM,
}

/** A red thread between two pages, usually of different codices, and what ties them. */
@Entity(tableName = "threads", indices = [Index("fromPage"), Index("toPage")])
data class Thread(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromPage: Long,
    val toPage: Long,
    val note: String = "",
    val createdAt: Long,
) {
    fun other(pageId: Long) = if (fromPage == pageId) toPage else fromPage
}

/** A page set down in a place of the monastery of memory, with the image that keeps it there. */
@Entity(tableName = "loci", indices = [Index("pageId")])
data class Locus(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [app.scriptor.domain.Place] name. */
    val place: String,
    val pageId: Long,
    /** What is seen there: a vivid, odd picture that stands for the idea. */
    val image: String,
    val createdAt: Long,
)

@Entity(tableName = "reviews", indices = [Index("pageId")])
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Long,
    val at: Long,
    /** [Mode] name. */
    val mode: String,
    /** Candles staked before looking, 1..3; 0 when none was staked. */
    val confidence: Int = 0,
    /** [app.scriptor.domain.Grade.n]. */
    val grade: Int,
    /** Share of the page's words the restoration caught, or -1 when it was done in the head. */
    val coverage: Float = -1f,
    /** What was written from memory, a layer of the palimpsest. */
    val attempt: String = "",
)

@Dao
abstract class ScriptorDao {
    @Query("SELECT * FROM codices ORDER BY createdAt DESC") abstract fun observeCodices(): Flow<List<Codex>>
    @Query("SELECT * FROM pages ORDER BY createdAt ASC") abstract fun observePages(): Flow<List<Page>>
    @Query("SELECT * FROM glosses ORDER BY createdAt ASC") abstract fun observeGlosses(): Flow<List<Gloss>>
    @Query("SELECT * FROM reviews ORDER BY at ASC") abstract fun observeReviews(): Flow<List<Review>>
    @Query("SELECT * FROM threads ORDER BY createdAt ASC") abstract fun observeThreads(): Flow<List<Thread>>
    @Query("SELECT * FROM loci ORDER BY createdAt ASC") abstract fun observeLoci(): Flow<List<Locus>>

    @Query("SELECT * FROM codices") abstract suspend fun codices(): List<Codex>
    @Query("SELECT * FROM pages") abstract suspend fun pages(): List<Page>
    @Query("SELECT * FROM glosses") abstract suspend fun glosses(): List<Gloss>
    @Query("SELECT * FROM reviews") abstract suspend fun reviews(): List<Review>
    @Query("SELECT * FROM threads") abstract suspend fun threads(): List<Thread>
    @Query("SELECT * FROM loci") abstract suspend fun loci(): List<Locus>

    @Query("SELECT * FROM pages WHERE id = :id") abstract suspend fun page(id: Long): Page?
    @Query("SELECT * FROM codices WHERE id = :id") abstract suspend fun codex(id: Long): Codex?

    @Insert abstract suspend fun insert(c: Codex): Long
    @Update abstract suspend fun update(c: Codex)
    @Delete abstract suspend fun delete(c: Codex)

    @Insert abstract suspend fun insert(p: Page): Long
    @Update abstract suspend fun update(p: Page)
    @Delete abstract suspend fun delete(p: Page)

    @Insert abstract suspend fun insert(g: Gloss): Long
    @Update abstract suspend fun update(g: Gloss)
    @Delete abstract suspend fun delete(g: Gloss)

    @Insert abstract suspend fun insert(r: Review): Long

    @Insert abstract suspend fun insert(t: Thread): Long
    @Delete abstract suspend fun delete(t: Thread)

    @Insert abstract suspend fun insert(l: Locus): Long
    @Delete abstract suspend fun delete(l: Locus)

    @Query("DELETE FROM threads WHERE fromPage = :pageId OR toPage = :pageId") abstract suspend fun deleteThreadsOf(pageId: Long)
    @Query("DELETE FROM loci WHERE pageId = :pageId") abstract suspend fun deleteLociOf(pageId: Long)

    @Query("DELETE FROM glosses WHERE pageId = :pageId") abstract suspend fun deleteGlossesOf(pageId: Long)
    @Query("DELETE FROM reviews WHERE pageId = :pageId") abstract suspend fun deleteReviewsOf(pageId: Long)

    @Transaction
    open suspend fun deletePage(p: Page) {
        deleteGlossesOf(p.id)
        deleteReviewsOf(p.id)
        deleteThreadsOf(p.id)
        deleteLociOf(p.id)
        delete(p)
    }

    @Query("SELECT * FROM pages WHERE codexId = :codexId") abstract suspend fun pagesOf(codexId: Long): List<Page>

    @Transaction
    open suspend fun deleteCodex(c: Codex) {
        pagesOf(c.id).forEach { deletePage(it) }
        delete(c)
    }

    @Query("DELETE FROM codices") protected abstract suspend fun clearCodices()
    @Query("DELETE FROM pages") protected abstract suspend fun clearPages()
    @Query("DELETE FROM glosses") protected abstract suspend fun clearGlosses()
    @Query("DELETE FROM reviews") protected abstract suspend fun clearReviews()
    @Query("DELETE FROM threads") protected abstract suspend fun clearThreads()
    @Query("DELETE FROM loci") protected abstract suspend fun clearLoci()
    @Insert protected abstract suspend fun insertThreads(l: List<Thread>)
    @Insert protected abstract suspend fun insertLoci(l: List<Locus>)
    @Insert protected abstract suspend fun insertCodices(l: List<Codex>)
    @Insert protected abstract suspend fun insertPages(l: List<Page>)
    @Insert protected abstract suspend fun insertGlosses(l: List<Gloss>)
    @Insert protected abstract suspend fun insertReviews(l: List<Review>)

    @Transaction
    open suspend fun replaceAll(
        codices: List<Codex>, pages: List<Page>, glosses: List<Gloss>, reviews: List<Review>,
        threads: List<Thread> = emptyList(), loci: List<Locus> = emptyList(),
    ) {
        clearLoci()
        clearThreads()
        clearReviews()
        clearGlosses()
        clearPages()
        clearCodices()
        insertCodices(codices)
        insertPages(pages)
        insertGlosses(glosses)
        insertReviews(reviews)
        insertThreads(threads)
        insertLoci(loci)
    }
}

@Database(
    entities = [Codex::class, Page::class, Gloss::class, Review::class, Thread::class, Locus::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class ScriptorDatabase : RoomDatabase() {
    abstract fun dao(): ScriptorDao

    companion object {
        fun create(context: Context): ScriptorDatabase =
            Room.databaseBuilder(context.applicationContext, ScriptorDatabase::class.java, "scriptor.db").build()
    }
}
