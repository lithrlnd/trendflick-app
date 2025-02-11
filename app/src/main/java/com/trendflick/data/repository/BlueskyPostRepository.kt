package com.trendflick.data.repository

import com.trendflick.data.api.*
import com.trendflick.data.model.Author
import com.trendflick.data.model.Post
import com.trendflick.ui.navigation.PostType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlueskyPostRepository @Inject constructor(
    private val blueskyApi: BlueskyApi
) : PostRepository {

    override suspend fun getTrendingPosts(): List<Post> {
        return try {
            val response = blueskyApi.getTimeline(
                GetTimelineRequest(algorithm = "trending")
            )
            response.feed.map { feedView ->
                mapApiPostToDomain(feedView.post)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPostsByCategory(categoryId: String): List<Post> {
        return try {
            val response = blueskyApi.getTimeline(
                GetTimelineRequest(algorithm = categoryId)
            )
            response.feed.map { feedView ->
                mapApiPostToDomain(feedView.post)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapApiPostToDomain(apiPost: com.trendflick.data.api.Post): Post {
        return Post(
            id = apiPost.uri,
            content = apiPost.record.text,
            author = Author(
                did = apiPost.author.did,
                handle = apiPost.author.handle,
                displayName = apiPost.author.displayName ?: apiPost.author.handle,
                avatarUrl = apiPost.author.avatar ?: ""
            ),
            timestamp = formatTimestamp(apiPost.record.createdAt),
            mediaUrl = getMediaUrl(apiPost.record.embed),
            type = determinePostType(apiPost.record.embed),
            replyCount = apiPost.replyCount ?: 0,
            likeCount = apiPost.likeCount ?: 0,
            repostCount = apiPost.repostCount ?: 0,
            isLiked = apiPost.viewer?.like != null,
            isReposted = apiPost.viewer?.repost != null,
            description = apiPost.record.text
        )
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val instant = Instant.parse(timestamp)
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            timestamp
        }
    }

    private fun getMediaUrl(embed: Any?): String {
        return when (embed) {
            is Map<*, *> -> {
                when (embed["\$type"] as? String) {
                    "app.bsky.embed.images" -> {
                        val images = embed["images"] as? List<Map<*, *>>
                        images?.firstOrNull()?.let { image ->
                            (image["image"] as? Map<*, *>)?.get("ref")?.toString() ?: ""
                        } ?: ""
                    }
                    "app.bsky.embed.external" -> {
                        val external = embed["external"] as? Map<*, *>
                        external?.get("thumb")?.toString() ?: ""
                    }
                    else -> ""
                }
            }
            else -> ""
        }
    }

    private fun determinePostType(embed: Any?): PostType {
        return when (embed) {
            is Map<*, *> -> {
                when (embed["\$type"] as? String) {
                    "app.bsky.embed.images" -> PostType.IMAGE
                    "app.bsky.embed.external" -> {
                        val external = embed["external"] as? Map<*, *>
                        if (external?.get("uri")?.toString()?.contains("video") == true) {
                            PostType.VIDEO
                        } else {
                            PostType.TEXT
                        }
                    }
                    "app.bsky.embed.record" -> PostType.THREAD
                    else -> PostType.TEXT
                }
            }
            else -> PostType.TEXT
        }
    }

    override suspend fun likePost(postId: String): Boolean {
        return try {
            blueskyApi.createLike(postId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unlikePost(postId: String): Boolean {
        return try {
            blueskyApi.deleteLike(postId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun repostPost(postId: String): Boolean {
        return try {
            blueskyApi.createRepost(postId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unrepostPost(postId: String): Boolean {
        return try {
            blueskyApi.deleteRepost(postId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getPostComments(postId: String): List<Post> {
        return try {
            val thread = blueskyApi.getPostThread(postId).thread
            thread.replies?.map { reply ->
                mapApiPostToDomain(reply.post)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addComment(postId: String, comment: String): Post {
        return try {
            val response = blueskyApi.createPost(
                text = comment,
                reply = postId
            )
            mapApiPostToDomain(response)
        } catch (e: Exception) {
            throw e
        }
    }
} 