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
    Category("Tech & AI", "��", setOf(
        "tech", "ai", "technology", "coding", 
        "programming", "artificialintelligence", "chatgpt", "gpt",
        "web3", "blockchain", "innovation", "software",
        "machinelearning", "ml", "developer", "tech",
        "computerscience", "future", "startup", "digital"
    )),
    Category("Politics", "🗳️", setOf(
        "politics", "news", "democracy", "political",
        "election2025", "vote2025", "congress", "government",
        "policy", "activism", "campaign", "climateaction",
        "current", "world", "breaking", "senate",
        "house", "law", "justice", "reform"
    )),
    Category("Entertainment", "🎬", setOf(
        "entertainment", "movies", "tv", "film",
        "netflix", "streaming", "hollywood", "series",
        "show", "tvshow", "bingeworthy", "cinema",
        "actor", "actress", "director", "premiere",
        "newmovie", "television", "drama", "comedy"
    )),
    Category("Gaming", "🎮", setOf(
        "gaming", "games", "gamer", "videogames",
        "esports", "twitch", "streamer", "ps5",
        "xbox", "nintendoswitch", "pcgaming", "steam",
        "gamedev", "gaming", "rpg", "fps",
        "indiegame", "retrogaming", "gamingcommunity", "gameplay"
    )),
    Category("Art & Design", "🎨", setOf(
        "art", "artist", "design", "artwork",
        "illustration", "creative", "digitalart", "artistsofbsky",
        "graphicdesign", "contemporary", "aiart", "modernart",
        "drawing", "painting", "sketch", "artistic",
        "artgallery", "artonbsky", "artcommunity", "create"
    )),
    Category("Beauty", "💄", setOf(
        "beauty", "makeup", "skincare", "selfcare",
        "beautytips", "glam", "mua", "beautycommunity",
        "cosmetics", "beautycare", "makeupartist", "skincareroutine",
        "beautyblog", "makeuptutorial", "beautyproducts", "natural",
        "organic", "wellness", "beautytips", "glow"
    )),
    Category("Music", "🎵", setOf(
        "music", "newmusic", "spotify", "musician",
        "hiphop", "rap", "indie", "rock",
        "pop", "musicproducer", "livemusic", "song",
        "album", "artist", "band", "concert",
        "musicproduction", "singer", "producer", "dj"
    )),
    Category("Food", "🍳", setOf(
        "food", "foodie", "cooking", "recipe",
        "homecooking", "foodphotography", "chef", "healthyfood",
        "foodblog", "foodlover", "cuisine", "baking",
        "vegan", "vegetarian", "dinner", "lunch",
        "breakfast", "foodstagram", "delicious", "tasty"
    )),
    Category("Fitness", "💪", setOf(
        "fitness", "gym", "workout", "health",
        "wellness", "training", "nutrition", "mindfulness",
        "exercise", "fitnessmotivation", "healthy", "fit",
        "bodybuilding", "yoga", "running", "strength",
        "personaltrainer", "fitlife", "active", "lifestyle"
    ))
) 