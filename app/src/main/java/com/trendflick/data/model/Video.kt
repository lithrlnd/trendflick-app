package com.trendflick.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.trendflick.data.db.Converters
import java.time.Instant

@Entity(tableName = "videos")
data class Video(
    @PrimaryKey var id: Int = 0,
    var uri: String = "", // AT Protocol URI (at://did/app.bsky.feed.post/tid)
    var did: String = "", // AT Protocol Decentralized Identity
    var handle: String = "", // User's handle (e.g., @username.bsky.social)
    var videoUrl: String = "",
    var thumbnailUrl: String = "",
    var title: String = "",
    var description: String = "",
    var likes: Int = 0,
    var commentCount: Int = 0,
    var shares: Int = 0,
    @TypeConverters(Converters::class)
    var hashtags: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis(), // Original creation timestamp
    var indexedAt: String? = Instant.now().toString(), // AT Protocol indexing timestamp
    var sortAt: Long = calculateSortTimestamp(System.currentTimeMillis(), indexedAt), // For chronological sorting
    @TypeConverters(Converters::class)
    var labels: List<String> = emptyList(), // AT Protocol content labels
    @TypeConverters(Converters::class)
    var facets: List<Map<String, Any>> = emptyList(), // Rich text features (mentions, links, etc.)
    @Ignore var relatedVideos: List<Video> = emptyList(),
    @Ignore var comments: List<Comment> = emptyList()
) {
    // For backwards compatibility
    val timestamp: Long get() = sortAt
    val userId: String get() = did // For backwards compatibility
    val username: String get() = handle.substringBefore('.')
}

private fun calculateSortTimestamp(createdAt: Long, indexedAt: String?): Long {
    val indexedTime = indexedAt?.let { 
        try {
            Instant.parse(it).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    } ?: System.currentTimeMillis()

    return when {
        // If createdAt is before Unix epoch, set to indexedTime
        createdAt < 0 -> indexedTime
        // If createdAt is between epoch and indexedAt, use createdAt
        createdAt in 1..indexedTime -> createdAt
        // If createdAt is in future beyond indexedTime, use indexedTime
        else -> indexedTime
    }
}

// AT Protocol specific extensions
fun Video.toAtUri(): String = "at://$did/app.bsky.feed.post/$id"

fun Video.toAtRecord() = mapOf(
    "${'$'}type" to "app.bsky.feed.post",
    "text" to description,
    "createdAt" to Instant.ofEpochMilli(createdAt).toString(),
    "embed" to mapOf(
        "${'$'}type" to "app.bsky.embed.external",
        "external" to mapOf(
            "uri" to videoUrl,
            "title" to title,
            "description" to description,
            "thumb" to thumbnailUrl
        )
    ),
    "facets" to facets,
    "labels" to labels
) 