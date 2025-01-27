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
    Category("Tech & AI", "💻", setOf(
        "tech", "ai", "coding", "startup", "innovation",
        "technology", "artificialintelligence", "programming",
        "developer", "machinelearning"
    )),
    Category("Entertainment", "🎬", setOf(
        "movies", "tv", "streaming", "cinema", "entertainment",
        "film", "series", "netflix", "hollywood", "shows"
    )),
    Category("Gaming", "🎮", setOf(
        "gaming", "esports", "streamer", "gameplay", "gamer",
        "twitch", "videogames", "ps5", "xbox", "nintendoswitch"
    )),
    Category("Art & Design", "🎨", setOf(
        "art", "design", "illustration", "creative", "artist",
        "digitalart", "graphicdesign", "artwork", "drawing",
        "animation"
    )),
    Category("Beauty", "💄", setOf(
        "beauty", "makeup", "skincare", "fashion", "style",
        "cosmetics", "beautytips", "selfcare", "glam",
        "beautycommunity"
    )),
    Category("Music", "🎵", setOf(
        "music", "newmusic", "artist", "songs", "musician",
        "spotify", "hiphop", "rap", "indie", "livemusic"
    )),
    Category("Food", "🍳", setOf(
        "food", "cooking", "recipe", "foodie", "chef",
        "homecooking", "foodphotography", "baking", "cuisine",
        "foodlover"
    )),
    Category("Fitness", "💪", setOf(
        "fitness", "health", "workout", "wellness", "gym",
        "training", "motivation", "exercise", "healthy",
        "fitnessmotivation"
    ))
) 