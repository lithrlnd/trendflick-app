package com.trendflick.data.model

import java.time.Instant

data class Comment(
    val id: String,           // AT Protocol URI rkey
    val userId: String,       // AT Protocol DID
    val username: String,
    val avatar: String? = null,
    val content: String,
    val createdAt: String,    // ISO 8601 timestamp from AT Protocol
    val indexedAt: String? = null,  // AT Protocol indexing timestamp
    val likes: Int = 0,
    val isLiked: Boolean = false,
    val replyCount: Int = 0,
    val replyTo: String? = null,  // Parent post URI if this is a reply
    val rootUri: String? = null   // Root post URI of the thread
) {
    // Calculate the proper sort timestamp according to AT Protocol specs
    val sortTimestamp: Long = calculateSortTimestamp()
    
    private fun calculateSortTimestamp(): Long {
        val createdTime = try {
            Instant.parse(createdAt).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val indexedTime = indexedAt?.let { 
            try {
                Instant.parse(it).toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis()

        return when {
            // If createdAt is before Unix epoch, use indexedTime
            createdTime < 0 -> indexedTime
            // If createdAt is in the future beyond indexedTime, use indexedTime
            createdTime > indexedTime -> indexedTime
            // Otherwise use createdAt
            else -> createdTime
        }
    }

    // Convert to AT Protocol URI format
    fun toAtUri(): String = "at://$userId/app.bsky.feed.post/$id"

    // Convert to AT Protocol record format for replies
    fun toAtRecord(parentUri: String): Map<String, Any> = mapOf(
        "\$type" to "app.bsky.feed.post",
        "text" to content,
        "createdAt" to createdAt,
        "reply" to mapOf(
            "root" to mapOf(
                "uri" to (rootUri ?: parentUri),
                "cid" to "" // CID will be generated by the server
            ),
            "parent" to mapOf(
                "uri" to (replyTo ?: parentUri),
                "cid" to "" // CID will be generated by the server
            )
        )
    )
} 