package com.trendflick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AtSession(
    @field:Json(name = "accessJwt") val accessJwt: String,
    @field:Json(name = "refreshJwt") val refreshJwt: String,
    @field:Json(name = "handle") val handle: String,
    @field:Json(name = "did") val did: String,
    @field:Json(name = "email") val email: String? = null
) 