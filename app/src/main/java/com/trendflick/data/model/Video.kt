package com.trendflick.data.model

data class Video(
    val uri: String,
    val userId: String,
    val handle: String,
    val title: String = "",
    val description: String = "",
    val videoUrl: String,
    val thumbnailUrl: String = "",
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    val isLiked: Boolean = false,
    val commentsList: List<Comment>? = null
) {
    // Alias for commentsList to maintain compatibility with existing code
    val comments: List<Comment>?
        get() = commentsList
}
