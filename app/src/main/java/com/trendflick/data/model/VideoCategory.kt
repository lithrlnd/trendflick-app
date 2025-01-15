package com.trendflick.data.model

import androidx.compose.ui.graphics.Color

data class VideoCategory(
    val id: String,
    val name: String,
    val icon: String,
    val color: Color,
    val description: String = "",
    val priority: Int = 0
) {
    companion object {
        val defaultCategories = listOf(
            VideoCategory(
                id = "fyp",
                name = "For You",
                icon = "✨",
                color = Color(0xFF9C27B0),
                description = "Your personalized feed"
            ),
            VideoCategory(
                id = "trending",
                name = "Trending",
                icon = "🔥",
                color = Color(0xFFFF4B4B),
                description = "What's hot right now"
            ),
            VideoCategory(
                id = "comedy",
                name = "Comedy",
                icon = "😂",
                color = Color(0xFFFFB347),
                description = "Laugh out loud"
            ),
            VideoCategory(
                id = "music",
                name = "Music",
                icon = "🎵",
                color = Color(0xFF98FB98),
                description = "Tunes and beats"
            ),
            VideoCategory(
                id = "gaming",
                name = "Gaming",
                icon = "🎮",
                color = Color(0xFF87CEEB),
                description = "Game on!"
            ),
            VideoCategory(
                id = "tech",
                name = "Tech",
                icon = "💻",
                color = Color(0xFF9370DB),
                description = "Digital world"
            )
        )
    }
} 