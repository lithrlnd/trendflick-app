package com.trendflick.ui.model

sealed class SuggestionItem {
    data class Mention(
        val handle: String,
        val displayName: String? = null,
        val avatar: String? = null
    ) : SuggestionItem()

    data class Hashtag(
        val tag: String,
        val postCount: Int = 0
    ) : SuggestionItem()
} 