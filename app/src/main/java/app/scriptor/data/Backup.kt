package app.scriptor.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** The whole scriptorium as one JSON file. Nothing leaves the phone unless the scribe exports it. */
object Backup {
    private const val VERSION = 1

    fun encode(codices: List<Codex>, pages: List<Page>, glosses: List<Gloss>, reviews: List<Review>): String = JSONObject()
        .put("app", "scriptor")
        .put("version", VERSION)
        .put("codices", JSONArray(codices.map {
            JSONObject().put("id", it.id).put("kind", it.kind).put("title", it.title).put("author", it.author).put("preface", it.preface)
                .put("questions", it.questions).put("answered", it.answered).put("sentence", it.sentence).put("words", it.words)
                .put("surprise", it.surprise).put("binding", it.binding).put("createdAt", it.createdAt).put("closedAt", it.closedAt ?: JSONObject.NULL)
        }))
        .put("pages", JSONArray(pages.map {
            JSONObject().put("id", it.id).put("codexId", it.codexId).put("title", it.title).put("locator", it.locator).put("body", it.body)
                .put("cues", it.cues).put("gold", it.gold).put("colophon", it.colophon).put("quote", it.quote)
                .put("stability", it.stability).put("difficulty", it.difficulty).put("reviewedAt", it.reviewedAt)
                .put("reps", it.reps).put("lapses", it.lapses).put("erratum", it.erratum).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
        }))
        .put("glosses", JSONArray(glosses.map {
            JSONObject().put("id", it.id).put("pageId", it.pageId).put("kind", it.kind).put("prompt", it.prompt).put("text", it.text).put("createdAt", it.createdAt)
        }))
        .put("reviews", JSONArray(reviews.map {
            JSONObject().put("id", it.id).put("pageId", it.pageId).put("at", it.at).put("mode", it.mode).put("confidence", it.confidence)
                .put("grade", it.grade).put("coverage", it.coverage.toDouble()).put("attempt", it.attempt)
        }))
        .toString(2)

    class Contents(val codices: List<Codex>, val pages: List<Page>, val glosses: List<Gloss>, val reviews: List<Review>)

    /** Throws [IllegalArgumentException] when the text is not a Scriptor backup. */
    fun decode(text: String): Contents {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "scriptor") { "Not a Scriptor backup" }
        fun list(key: String) = (root.optJSONArray(key) ?: JSONArray()).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
        return Contents(
            codices = list("codices").map {
                Codex(
                    id = it.getLong("id"), kind = it.optString("kind", Kind.BOOK.name), title = it.getString("title"), author = it.optString("author"),
                    preface = it.optString("preface"), questions = it.optString("questions"), answered = it.optInt("answered"),
                    sentence = it.optString("sentence"), words = it.optString("words"), surprise = it.optString("surprise"),
                    binding = it.optInt("binding"), createdAt = it.getLong("createdAt"),
                    closedAt = if (it.isNull("closedAt")) null else it.optLong("closedAt"),
                )
            },
            pages = list("pages").map {
                Page(
                    id = it.getLong("id"), codexId = it.getLong("codexId"), title = it.getString("title"), locator = it.optString("locator"),
                    body = it.getString("body"), cues = it.optString("cues"), gold = it.optString("gold"), colophon = it.optString("colophon"),
                    quote = it.optString("quote"), stability = it.getDouble("stability"), difficulty = it.getDouble("difficulty"),
                    reviewedAt = it.getLong("reviewedAt"), reps = it.optInt("reps"), lapses = it.optInt("lapses"), erratum = it.optBoolean("erratum"),
                    createdAt = it.getLong("createdAt"), updatedAt = it.optLong("updatedAt", it.getLong("createdAt")),
                )
            },
            glosses = list("glosses").map {
                Gloss(id = it.getLong("id"), pageId = it.getLong("pageId"), kind = it.getString("kind"), prompt = it.optString("prompt"), text = it.getString("text"), createdAt = it.getLong("createdAt"))
            },
            reviews = list("reviews").map {
                Review(
                    id = it.getLong("id"), pageId = it.getLong("pageId"), at = it.getLong("at"), mode = it.optString("mode", Mode.RECALL.name),
                    confidence = it.optInt("confidence"), grade = it.getInt("grade"), coverage = it.optDouble("coverage", -1.0).toFloat(), attempt = it.optString("attempt"),
                )
            },
        )
    }

    fun write(context: Context, uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("Can't write there")
    }

    fun read(context: Context, uri: Uri): Contents {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Can't read that")
        return decode(text)
    }
}
