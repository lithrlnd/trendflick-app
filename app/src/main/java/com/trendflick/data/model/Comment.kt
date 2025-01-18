package com.trendflick.data.model

import androidx.room.Ignore

data class Comment(
    val id: String,
    val uri: String, // AT Protocol URI
    val userId: String, // AT Protocol DID
    val username: String,
    val content: String,
    val createdAt: Long, // Original creation timestamp
    val indexedAt: String? = null, // AT Protocol indexing timestamp
    val likes: Int = 0,
    val replyCount: Int = 0,
    @Ignore val replies: List<Comment> = emptyList(),
    val isLiked: Boolean = false,
    val replyTo: String? = null, // Parent comment URI if this is a reply
    val sortAt: Long = calculateSortTimestamp(createdAt, indexedAt) // For chronological sorting
) {
    val timestamp: Long get() = sortAt // For backwards compatibility
}

private fun calculateSortTimestamp(createdAt: Long, indexedAt: String?): Long {
    val indexedTime = indexedAt?.let { 
        try {
            java.time.Instant.parse(it).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    } ?: System.currentTimeMillis()

    return when {
        // If createdAt is before Unix epoch, use indexedTime
        createdAt < 0 -> indexedTime
        // If createdAt is in the future beyond indexedTime, use indexedTime
        createdAt > indexedTime -> indexedTime
        // Otherwise use createdAt
        else -> createdAt
    }
}

// AT Protocol specific extensions
fun Comment.toAtUri(): String = "at://$userId/app.bsky.feed.post/$id"

fun Comment.toAtRecord(videoUri: String) = mapOf(
    "text" to content,
    "createdAt" to createdAt,
    "indexedAt" to indexedAt,
    "reply" to mapOf(
        "root" to videoUri,
        "parent" to (replyTo ?: videoUri)
    )
) 