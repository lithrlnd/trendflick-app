package com.trendflick.data.model

data class Comment(
    val id: String,
    val username: String,
    val content: String,
    val timestamp: Long,
    val likes: Int = 0,
    val avatar: String? = null,
    val isLiked: Boolean = false,
    val replies: List<Comment>? = null
)
