package com.trendflick.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Facet(
    val index: FacetIndex,
    val features: List<FacetFeature>
)

@JsonClass(generateAdapter = true)
data class FacetIndex(
    @Json(name = "byteStart") val start: Int,
    @Json(name = "byteEnd") val end: Int
)

sealed class FacetFeature {
    @Json(name = "\$type") abstract val type: String
}

@JsonClass(generateAdapter = true)
data class MentionFeature(
    @Json(name = "\$type") override val type: String = "app.bsky.richtext.facet#mention",
    val did: String
) : FacetFeature()

@JsonClass(generateAdapter = true)
data class LinkFeature(
    @Json(name = "\$type") override val type: String = "app.bsky.richtext.facet#link",
    val uri: String
) : FacetFeature()

@JsonClass(generateAdapter = true)
data class TagFeature(
    @Json(name = "\$type") override val type: String = "app.bsky.richtext.facet#tag",
    val tag: String
) : FacetFeature() 
