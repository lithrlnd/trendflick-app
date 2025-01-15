package com.trendflick.data.lexicon

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AtLexiconValidator @Inject constructor() {
    fun validateRecord(type: String, record: Map<String, Any>): Boolean {
        return when (type) {
            "app.bsky.feed.post" -> validatePost(record)
            "app.bsky.actor.profile" -> validateProfile(record)
            "app.bsky.feed.like" -> validateLike(record)
            "app.bsky.feed.repost" -> validateRepost(record)
            "app.bsky.graph.follow" -> validateFollow(record)
            else -> false
        }
    }

    private fun validatePost(record: Map<String, Any>): Boolean {
        return record["text"] != null &&
               record["createdAt"] != null &&
               (record["\$type"] as? String)?.startsWith("app.bsky.feed.") == true
    }

    private fun validateProfile(record: Map<String, Any>): Boolean {
        return (record["\$type"] as? String) == "app.bsky.actor.profile"
    }

    private fun validateLike(record: Map<String, Any>): Boolean {
        return record["subject"] != null &&
               (record["\$type"] as? String) == "app.bsky.feed.like"
    }

    private fun validateRepost(record: Map<String, Any>): Boolean {
        return record["subject"] != null &&
               (record["\$type"] as? String) == "app.bsky.feed.repost"
    }

    private fun validateFollow(record: Map<String, Any>): Boolean {
        return record["subject"] != null &&
               (record["\$type"] as? String) == "app.bsky.graph.follow"
    }
}

@JsonClass(generateAdapter = true)
data class LexiconDoc(
    val lexicon: Int,
    val id: String,
    val revision: Int,
    val description: String,
    val defs: Map<String, LexiconDef>
)

@JsonClass(generateAdapter = true)
data class LexiconDef(
    val type: String,
    val description: String,
    val required: List<String>?,
    val properties: Map<String, LexiconProperty>?
)

@JsonClass(generateAdapter = true)
data class LexiconProperty(
    val type: String,
    val description: String?,
    val format: String?,
    @Json(name = "maxLength") val maxLength: Int?,
    @Json(name = "minLength") val minLength: Int?,
    val default: Any?
) 