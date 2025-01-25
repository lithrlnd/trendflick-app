package com.trendflick.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResolveHandleResponse(
    @field:Json(name = "did") val did: String
) 