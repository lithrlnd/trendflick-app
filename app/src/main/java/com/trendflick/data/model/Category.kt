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
    Category("Arts & Culture", "🎨", setOf(
        "art", "artist", "creative", 
        "illustration", "artistsofbsky", "digitalart",
        "photography", "design", "create"
    )),
    
    Category("Books & Writing", "📚", setOf(
        "writing", "books", "author",
        "booktok", "bookclub", "amwriting",
        "writingcommunity", "publishing", "reads"
    )),
    
    Category("Tech & Dev", "💻", setOf(
        "tech", "coding", "dev",
        "programming", "opensource", "webdev",
        "ai", "technology", "software"
    )),
    
    Category("Gaming & Streams", "🎮", setOf(
        "gaming", "games", "twitch",
        "streamer", "indiegame", "gamedev",
        "rpg", "esports", "nintendoswitch"
    )),
    
    Category("Music & Audio", "🎵", setOf(
        "music", "musician", "indie",
        "newmusic", "producer", "livemusic",
        "musicproduction", "band", "song"
    )),
    
    Category("Creative Works", "✨", setOf(
        "commission", "portfolio", "freelance",
        "creative", "artist", "design",
        "artwork", "creator", "studio"
    )),
    
    Category("Tabletop & Board Games", "🎲", setOf(
        "boardgames", "ttrpg", "dnd",
        "tabletop", "rpg", "dungeonsanddragons",
        "boardgame", "tabletopgames", "gamenight"
    ))
) 