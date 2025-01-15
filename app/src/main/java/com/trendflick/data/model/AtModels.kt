package com.trendflick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents an AT Protocol session with DID and handle as per protocol spec
 * DID format: did:plc:{identifier}
 * Handle format: {username}.bsky.social
 * 
 * Reference: https://atproto.com/specs/atp#session
 */
@JsonClass(generateAdapter = true)
data class AtSession(
    @Json(name = "did") val did: String, // Non-null as per AT Protocol spec
    @Json(name = "handle") val handle: String, // Non-null as per AT Protocol spec
    @Json(name = "accessJwt") val accessJwt: String,
    @Json(name = "refreshJwt") val refreshJwt: String,
    @Json(name = "email") val email: String? = null // Optional field
)

@JsonClass(generateAdapter = true)
data class AtIdentity(
    @Json(name = "did") val did: String,
    @Json(name = "handle") val handle: String,
    @Json(name = "displayName") val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class AtProfile(
    @Json(name = "did") val did: String,
    @Json(name = "handle") val handle: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "viewer") val viewer: AtProfileViewer? = null,
    @Json(name = "indexedAt") val indexedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AtProfileViewer(
    @Json(name = "muted") val muted: Boolean = false,
    @Json(name = "blockedBy") val blockedBy: Boolean = false,
    @Json(name = "following") val following: String? = null,
    @Json(name = "followedBy") val followedBy: String? = null
)

@JsonClass(generateAdapter = true)
data class TimelineResponse(
    @Json(name = "feed") val feed: List<AtPost>,
    @Json(name = "cursor") val cursor: String?
)

@JsonClass(generateAdapter = true)
data class FeedResponse(
    @Json(name = "feed") val feed: List<AtPost>,
    @Json(name = "cursor") val cursor: String?
)

@JsonClass(generateAdapter = true)
data class AtPost(
    @Json(name = "\$type") val type: String = "app.bsky.feed.post",
    @Json(name = "text") val text: String,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "embed") val embed: AtEmbed? = null,
    @Json(name = "facets") val facets: List<AtFacet>? = null
)

@JsonClass(generateAdapter = true)
data class AtEmbed(
    @Json(name = "\$type") val type: String,
    @Json(name = "images") val images: List<AtImage>? = null,
    @Json(name = "external") val external: AtExternal? = null
)

@JsonClass(generateAdapter = true)
data class AtImage(
    @Json(name = "alt") val alt: String,
    @Json(name = "image") val image: AtBlob
)

@JsonClass(generateAdapter = true)
data class AtBlob(
    @Json(name = "\$type") val type: String = "blob",
    @Json(name = "ref") val ref: AtBlobRef,
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "size") val size: Long
)

@JsonClass(generateAdapter = true)
data class AtBlobRef(
    @Json(name = "\$link") val link: String
)

@JsonClass(generateAdapter = true)
data class AtExternal(
    @Json(name = "uri") val uri: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "thumb") val thumb: AtBlob? = null
)

@JsonClass(generateAdapter = true)
data class AtFacet(
    @Json(name = "index") val index: AtByteSlice,
    @Json(name = "features") val features: List<AtFacetFeature>
)

@JsonClass(generateAdapter = true)
data class AtByteSlice(
    @Json(name = "byteStart") val byteStart: Int,
    @Json(name = "byteEnd") val byteEnd: Int
)

@JsonClass(generateAdapter = true)
data class AtFacetFeature(
    @Json(name = "\$type") val type: String,
    @Json(name = "did") val did: String? = null,
    @Json(name = "uri") val uri: String? = null
) 