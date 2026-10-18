package app.scriptor

import app.scriptor.data.Backup
import app.scriptor.data.Codex
import app.scriptor.data.Gloss
import app.scriptor.data.GlossKind
import app.scriptor.data.Mode
import app.scriptor.data.Page
import app.scriptor.data.Locus
import app.scriptor.data.Review
import app.scriptor.data.Thread
import app.scriptor.domain.Craft
import app.scriptor.domain.Dispute
import app.scriptor.domain.Gender
import app.scriptor.domain.Place
import app.scriptor.domain.Progress
import app.scriptor.domain.Strokes
import app.scriptor.domain.gendered
import app.scriptor.domain.Grade
import app.scriptor.domain.Memory
import app.scriptor.domain.Rules
import app.scriptor.domain.Trace
import app.scriptor.domain.Words
import app.scriptor.notify.Hours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptorTest {
    private val day = Memory.DAY

    @Test
    fun recallIsNinetyPercentAfterOneStability() {
        val t = Memory.written(0)
        assertEquals(1.0, Memory.recall(t, 0), 1e-9)
        assertEquals(0.9, Memory.recall(t.copy(stability = 5.0), 5 * day), 1e-6)
        assertEquals(5.0, Memory.interval(5.0, 0.9), 1e-6)
    }

    @Test
    fun goodRestorationsStretchTheInterval() {
        var t = Memory.written(0)
        var at = 0L
        val intervals = mutableListOf<Double>()
        repeat(5) {
            at = Memory.due(t, 0.9)
            t = Memory.review(t, Grade.GOOD, at)
            intervals += t.stability
        }
        intervals.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b > a) }
        assertTrue(intervals.last() > 30)
    }

    @Test
    fun forgettingShrinksAndEasyGrowsMost() {
        val t = Trace(10.0, 5.0, 0)
        val at = 10 * day
        val again = Memory.review(t, Grade.AGAIN, at).stability
        val hard = Memory.review(t, Grade.HARD, at).stability
        val good = Memory.review(t, Grade.GOOD, at).stability
        val easy = Memory.review(t, Grade.EASY, at).stability
        assertTrue(again < t.stability)
        assertTrue(again < hard && hard < good && good < easy)
    }

    @Test
    fun comparisonForgivesEndings() {
        val page = "Вспоминание укрепляет память сильнее, чем перечитывание текста."
        val m = Words.compare(page, "перечитывать хуже; вспоминания укрепляют память")
        assertTrue(m.coverage > 0.6f)
        assertTrue(Words.compare(page, "").coverage == 0f)
        assertEquals(Grade.EASY, Words.suggest(Words.compare("Spacing beats cramming", "spacing beats cramming")))
    }

    @Test
    fun lacunaeHideContentWordsPreferringGold() {
        val text = "Each review flattens the forgetting curve, so the gaps between reviews can stretch further."
        val gaps = Words.lacunae(text, gold = "flattens", every = 4)
        assertTrue(gaps.isNotEmpty())
        assertTrue(gaps.any { text.substring(it.first, it.last + 1) == "flattens" })
        gaps.forEach { assertTrue(Words.isContent(text.substring(it.first, it.last + 1))) }
    }

    @Test
    fun confidentMistakesGoToTheErrata() = kotlinx.coroutines.runBlocking {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<ScriptorApp>()
        val dao = app.container.db.dao()
        val cx = dao.insert(Codex(title = "T", createdAt = 0))
        val id = app.container.scribe.addPage(Page(codexId = cx, title = "P", body = "b", stability = 0.0, difficulty = 0.0, reviewedAt = 0, createdAt = 0), now = 0)
        app.container.scribe.restore(id, Mode.RECALL, Grade.AGAIN, 3, -1f, "", now = 2 * day)
        assertTrue(dao.page(id)!!.erratum)
        assertEquals(1, dao.page(id)!!.lapses)
        app.container.scribe.restore(id, Mode.RECALL, Grade.GOOD, 2, -1f, "", now = 3 * day)
        assertFalse(dao.page(id)!!.erratum)
        assertEquals(1, dao.page(id)!!.reps)
    }

    @Test
    fun modesTakeTurns() {
        val p = Page(id = 1, codexId = 1, title = "t", body = "b", cues = "q?", stability = 1.0, difficulty = 5.0, reviewedAt = 0, createdAt = 0)
        val r = { n: Int -> List(n) { Review(pageId = 1, at = 0, mode = "", grade = 3) } }
        assertEquals(listOf(Mode.RECALL, Mode.CUES, Mode.LACUNAE, Mode.RECALL), (0..3).map { Rules.mode(p, r(it)) })
        assertEquals(Mode.LACUNAE, Rules.mode(p.copy(cues = ""), r(1)))
    }

    @Test
    fun illuminationIsEarned() {
        val p = Page(id = 1, codexId = 1, title = "t", body = "b", stability = 200.0, difficulty = 5.0, reviewedAt = 0, createdAt = 0, reps = 1, cues = "a\nb")
        val g = listOf(Gloss(pageId = 1, kind = GlossKind.SEAL.name, text = "x", createdAt = 0))
        val ill = Rules.illumination(p, g)
        assertTrue(ill.initial && ill.rubric && ill.gold && ill.seal)
        assertFalse(ill.vines)
        assertEquals(1.0, p.recall(10_000 * day), 0.0)
    }

    @Test
    fun calibrationAndStreak() {
        val reviews = listOf(Review(pageId = 1, at = 0, mode = "", confidence = 3, grade = 4), Review(pageId = 1, at = 0, mode = "", confidence = 3, grade = 1))
        assertEquals(0.5f, Rules.calibration(reviews).first { it.first == 3 }.second!!, 1e-6f)
        val today = LocalDate.of(2026, 10, 18)
        assertEquals(2, Rules.streak(setOf(today.minusDays(1), today.minusDays(2), today.minusDays(4)), today))
        assertEquals(3, Rules.streak(setOf(today, today.minusDays(1), today.minusDays(2)), today))
    }

    @Test
    fun bellsRingNextAtTheirMinute() {
        val zone = ZoneOffset.UTC
        val now = LocalDate.of(2026, 10, 18).atTime(22, 0).toInstant(zone).toEpochMilli()
        assertEquals(LocalDate.of(2026, 10, 19).atTime(21, 30).toInstant(zone).toEpochMilli(), Hours.next(now, 21 * 60 + 30, zone))
        assertEquals(LocalDate.of(2026, 10, 18).atTime(23, 0).toInstant(zone).toEpochMilli(), Hours.next(now, 23 * 60, zone))
    }

    @Test
    fun backupRoundTrips() {
        val cx = listOf(Codex(id = 1, title = "Т", preface = "p", questions = "a\nb", answered = 2, createdAt = 5, closedAt = 9))
        val pg = listOf(Page(id = 2, codexId = 1, title = "t", body = "b", cues = "c", stability = 3.5, difficulty = 4.0, reviewedAt = 7, createdAt = 6, erratum = true))
        val gl = listOf(Gloss(id = 3, pageId = 2, kind = GlossKind.NOVICE.name, prompt = "why", text = "because", createdAt = 8))
        val rv = listOf(Review(id = 4, pageId = 2, at = 9, mode = Mode.CUES.name, confidence = 2, grade = 3, coverage = 0.5f, attempt = "a"))
        val th = listOf(Thread(id = 5, fromPage = 2, toPage = 2, note = "n", createdAt = 1))
        val lo = listOf(Locus(id = 6, place = Place.CRYPT.name, pageId = 2, image = "img", createdAt = 2))
        val back = Backup.decode(Backup.encode(cx, pg.map { it.copy(miniature = "0.100,0.200 0.300,0.400") }, gl, rv, th, lo))
        assertEquals(th, back.threads)
        assertEquals(lo, back.loci)
        assertEquals("0.100,0.200 0.300,0.400", back.pages[0].miniature)
        assertEquals(cx, back.codices)
        assertEquals(pg, back.pages.map { it.copy(miniature = "") })
        assertEquals(gl, back.glosses)
        assertEquals(rv, back.reviews)
    }

    @Test
    fun genderMarksResolve() {
        val t = "Ты {уверен|уверена}, что {вспомнил|вспомнила}? Где ты {видел|видела} это сам{|а}?"
        assertEquals("Ты уверен, что вспомнил? Где ты видел это сам?", gendered(t, Gender.MALE))
        assertEquals("Ты уверена, что вспомнила? Где ты видела это сама?", gendered(t, Gender.FEMALE))
        assertEquals("plain | text", gendered("plain | text", Gender.FEMALE))
    }

    @Test
    fun strokesRoundTrip() {
        val s = listOf(listOf(0.1f to 0.2f, 0.3f to 0.4f), listOf(0.5f to 0.5f))
        assertEquals(s, Strokes.decode(Strokes.encode(s)))
        assertTrue(Strokes.decode("").isEmpty())
    }

    @Test
    fun disputeRoundTrips() {
        val d = Dispute("t", listOf("o1" to "r1", "o2" to "r2"))
        assertEquals(d, Dispute.decode("t", d.encode()))
    }

    @Test
    fun examMixesCodices() {
        fun page(id: Long, codex: Long) = Page(id = id, codexId = codex, title = "p$id", body = "b", cues = "q$id?", stability = 1.0, difficulty = 5.0, reviewedAt = 0, createdAt = 0)
        val pages = (1L..6L).map { page(it, if (it <= 3) 1 else 2) } + page(7, 3).copy(cues = "")
        val exam = Rules.exam(pages, n = 6, seed = 1)
        assertEquals(6, exam.size)
        exam.zipWithNext().forEach { (a, b) -> assertTrue(a.page.codexId != b.page.codexId) }
        exam.forEach { assertTrue(it.page.codexId in it.options) }
        assertTrue(Rules.exam(pages.filter { it.codexId == 1L }).isEmpty())
    }

    @Test
    fun craftsOpenWithProgress() {
        assertEquals(setOf(Craft.INK), Craft.open(Progress(0, 0, 0)))
        val some = Craft.open(Progress(pages = 3, reviews = 1, codices = 1))
        assertTrue(Craft.MARGINS in some && Craft.LACUNAE in some && Craft.MINIATURE !in some && Craft.THREADS !in some)
        assertEquals(Craft.entries.toSet(), Craft.open(Progress(10, 10, 2)))
    }

    @Test
    fun lockedCraftsLeaveModesOut() {
        val p = Page(id = 1, codexId = 1, title = "t", body = "b", cues = "q?", miniature = "0.1,0.1 0.2,0.2", stability = 1.0, difficulty = 5.0, reviewedAt = 0, createdAt = 0)
        val r = { n: Int -> List(n) { Review(pageId = 1, at = 0, mode = "", grade = 3) } }
        assertEquals(listOf(Mode.RECALL, Mode.RECALL), (0..1).map { Rules.mode(p, r(it), setOf(Craft.INK)) })
        assertEquals(Mode.MINIATURE, Rules.mode(p, r(3)))
    }
}
