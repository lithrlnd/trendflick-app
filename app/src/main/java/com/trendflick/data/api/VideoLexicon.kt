package com.trendflick.data.api

/**
 * Custom lexicon for video content in TrendFlick
 * Following AT Protocol's lexicon format
 */
object VideoLexicon {
    const val LEXICON = 1
    const val ID = "app.trendflick.video"
    
    // Record types
    object Records {
        const val VIDEO = "$ID.video"
        const val FEED = "$ID.feed"
        const val ENGAGEMENT = "$ID.engagement"
    }
    
    // Feed types
    object Feeds {
        const val TRENDING = "trending"
        const val FOLLOWING = "following"
        const val CATEGORY = "category"
        const val HASHTAG = "hashtag"
    }
    
    // Video metadata
    data class VideoMetadata(
        val duration: Long,
        val width: Int,
        val height: Int,
        val thumbnail: String,
        val processingStatus: ProcessingStatus,
        val category: String? = null,
        val hashtags: List<String> = emptyList()
    )
    
    enum class ProcessingStatus {
        PROCESSING,
        READY,
        FAILED
    }
    
    // Feed algorithms
    object Algorithms {
        const val FOR_YOU = "fyp"
        const val TRENDING = "trending"
        const val CATEGORY_BASED = "category"
        const val FOLLOWING = "following"
    }
    
    // Engagement types
    object Engagement {
        const val LIKE = "like"
        const val COMMENT = "comment"
        const val SHARE = "share"
        const val WATCH_TIME = "watchTime"
        const val COMPLETION_RATE = "completionRate"
    }
    
    // Content filters
    object Filters {
        const val SAFE_MODE = "safeMode"
        const val AGE_RESTRICTION = "ageRestriction"
        const val CONTENT_WARNING = "contentWarning"
    }
} 