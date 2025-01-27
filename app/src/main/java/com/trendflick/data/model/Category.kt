package com.trendflick.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val name: String,
    val emoji: String,
    val hashtags: Set<String>
)

data class MainFeed(
    val name: String,
    val icon: ImageVector,
    val emoji: String
)

val mainFeeds = listOf(
    MainFeed("FYP", Icons.Default.Home, "🏠"),
    MainFeed("Following", Icons.Default.People, "👥")
)

val categories = listOf(
    Category("Tech & AI", "💻", setOf("tech", "ai", "coding", "startup", "innovation")),
    Category("Entertainment", "🎬", setOf("movies", "tv", "streaming", "cinema")),
    Category("Gaming", "🎮", setOf("gaming", "esports", "streamer", "gameplay")),
    Category("Art & Design", "🎨", setOf("art", "design", "illustration", "creative")),
    Category("Beauty", "💄", setOf("beauty", "makeup", "skincare", "fashion")),
    Category("Music", "🎵", setOf("music", "newmusic", "artist", "songs")),
    Category("Food", "🍳", setOf("food", "cooking", "recipe", "foodie")),
    Category("Fitness", "💪", setOf("fitness", "health", "workout", "wellness"))
) 