package com.trendflick.data.model

sealed class SuggestionItem {
    data class Mention(
        val did: String,
        val handle: String,
        val displayName: String,
        val avatarUrl: String?
    ) : SuggestionItem()

    data class Hashtag(
        val tag: String,
        val postCount: Int = 0
    ) : SuggestionItem()
} 
