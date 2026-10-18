package app.scriptor.domain

import org.json.JSONArray
import org.json.JSONObject

/** A scholastic disputation, as Aquinas set them out: the thesis, objections to it, and a reply to each. */
data class Dispute(val thesis: String, val points: List<Pair<String, String>>) {
    fun encode(): String = JSONArray(points.map { (o, r) -> JSONObject().put("o", o).put("r", r) }).toString()

    companion object {
        fun decode(thesis: String, text: String): Dispute {
            val a = runCatching { JSONArray(text) }.getOrNull() ?: return Dispute(thesis, emptyList())
            return Dispute(thesis, (0 until a.length()).map { a.getJSONObject(it).let { o -> o.optString("o") to o.optString("r") } })
        }
    }
}
