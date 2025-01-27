package com.trendflick.data.model

data class TrendingHashtag(
    val tag: String,
    val count: Int = 0,
    val description: String? = null,
    val emoji: String? = null
) 
