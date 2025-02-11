package com.trendflick.data.model

import com.trendflick.data.api.VideoLexicon
import com.trendflick.ui.navigation.PostType

data class Post(
    val id: String,
    val content: String,
    val author: Author,
    val timestamp: String,
    val mediaUrl: String = "",
    val thumbnailUrl: String = "",
    val type: PostType = PostType.TEXT,
    val replyCount: Int = 0,
    val likeCount: Int = 0,
    val repostCount: Int = 0,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val description: String = "",
    val videoMetadata: VideoMetadata? = null,
    val engagement: VideoEngagement? = null
)

data class VideoMetadata(
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val thumbnail: String? = null,
    val processingStatus: VideoLexicon.ProcessingStatus = VideoLexicon.ProcessingStatus.PROCESSING,
    val category: String? = null,
    val hashtags: List<String> = emptyList(),
    val algorithm: String = VideoLexicon.Algorithms.FOR_YOU
)

data class VideoEngagement(
    val watchTime: Long = 0,
    val completionRate: Float = 0f,
    val hasWatched: Boolean = false,
    val lastWatchPosition: Long = 0
) 