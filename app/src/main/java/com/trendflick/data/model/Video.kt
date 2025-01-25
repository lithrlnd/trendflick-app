package com.trendflick.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.Ignore
import com.trendflick.data.db.Converters
import java.time.Instant
import java.util.UUID

@Entity(tableName = "videos")
@TypeConverters(Converters::class)
data class Video(
    @PrimaryKey var uri: String,  // AT Protocol URI
    var did: String,              // Creator's DID
    var handle: String,           // Creator's handle
    var videoUrl: String,         // URL to video content
    var description: String,      // Post text
    var createdAt: Instant,       // Original creation time
    var indexedAt: Instant,       // When first seen by server
    var sortAt: Instant,          // For chronological sorting
    var title: String = "",       // Optional title
    var thumbnailUrl: String = "", // Preview image URL
    var likes: Int = 0,
    var comments: Int = 0,
    var shares: Int = 0,
    var username: String = "",    // Display name
    var userId: String = "",       // For AT Protocol reference
    @Ignore var relatedVideos: List<Video> = emptyList()
) {
    // Required no-arg constructor for Room
    constructor() : this(
        uri = "",
        did = "",
        handle = "",
        videoUrl = "",
        description = "",
        createdAt = Instant.now(),
        indexedAt = Instant.now(),
        sortAt = Instant.now()
    )

    // For backwards compatibility
    val timestamp: String get() = sortAt.toString()
    val authorId: String get() = did

    // AT Protocol specific extensions
    fun toAtUri(): String = "at://$did/app.bsky.feed.post/$uri"

    fun toAtRecord() = mapOf(
        "${'$'}type" to "app.bsky.feed.post",
        "text" to description,
        "createdAt" to createdAt.toString(),
        "embed" to mapOf(
            "${'$'}type" to "app.bsky.embed.external",
            "external" to mapOf(
                "uri" to videoUrl,
                "title" to title,
                "description" to description,
                "thumb" to thumbnailUrl
            )
        ),
        "labels" to emptyList<String>()
    )
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
        createdAt < 0 -> indexedTime // If createdAt is before Unix epoch, use indexedTime
        createdAt in 1..indexedTime -> createdAt // If createdAt is between epoch and indexedAt, use createdAt
        else -> indexedTime // If createdAt is in future beyond indexedTime, use indexedTime
    }
} 