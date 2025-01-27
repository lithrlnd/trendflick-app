package com.trendflick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RepostRecord(
    @Json(name = "type") val type: String = "app.bsky.feed.repost",
    @Json(name = "subject") val subject: PostReference,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "actor") val actor: Actor
)

@JsonClass(generateAdapter = true)
data class PostReference(
    @Json(name = "uri") val uri: String,
    @Json(name = "cid") val cid: String
)

@JsonClass(generateAdapter = true)
data class Actor(
    @Json(name = "did") val did: String,
    @Json(name = "handle") val handle: String,
    @Json(name = "displayName") val displayName: String?
) 
