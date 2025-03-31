package com.trendflick.data.model

/**
 * Data class for user profile information
 */
data class UserProfile(
    val handle: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val posts: Int = 0,
    val recentPosts: List<Post> = emptyList(),
    val recentVideos: List<Post> = emptyList(),
    val recentLikes: List<Post> = emptyList(),
    val recentMedia: List<Post> = emptyList(),
    val isVerified: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollower: Boolean = false
)
