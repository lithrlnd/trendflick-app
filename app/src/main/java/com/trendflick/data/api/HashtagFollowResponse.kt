package com.trendflick.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HashtagFollowResponse(
    @Json(name = "isFollowing") val isFollowing: Boolean
) 