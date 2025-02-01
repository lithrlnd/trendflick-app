package com.trendflick.data.mapper

import com.trendflick.data.api.TrendingHashtag as ApiTrendingHashtag
import com.trendflick.data.model.TrendingHashtag as ModelTrendingHashtag

fun ApiTrendingHashtag.toModel(): ModelTrendingHashtag {
    return ModelTrendingHashtag(
        tag = this.tag,
        count = this.count ?: 0,
        emoji = this.emoji
    )
}

fun List<ApiTrendingHashtag>.toModelList(): List<ModelTrendingHashtag> {
    return this.map { it.toModel() }
} 