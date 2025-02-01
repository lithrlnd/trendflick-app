package com.trendflick.data.api

data class TrendingHashtag(
    val tag: String,
    val count: Int? = null,
    val emoji: String? = null
) 