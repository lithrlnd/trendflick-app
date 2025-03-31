package com.trendflick.data.model

/**
 * Data class for posts in the feed
 */
data class Post(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatar: String? = null,
    val content: String,
    val timestamp: Long,
    val likes: Int = 0,
    val comments: Int = 0,
    val reposts: Int = 0,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val mediaUrl: String? = null,
    val isVideo: Boolean = false,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val embedPost: Post? = null,
    val linkPreview: LinkPreview? = null
)

/**
 * Data class for link previews in posts
 */
data class LinkPreview(
    val url: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null
)
