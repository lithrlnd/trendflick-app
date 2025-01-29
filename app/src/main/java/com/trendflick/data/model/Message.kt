package com.trendflick.data.model

import java.time.Instant

data class Message(
    val uri: String,          // AT Protocol URI for the message
    val cid: String,          // Content identifier
    val author: Author,       // Message sender
    val recipient: Author,    // Message recipient
    val text: String,         // Message content
    val createdAt: Instant,   // Timestamp
    val record: MessageRecord // Original AT Protocol record
)

data class MessageRecord(
    val text: String,
    val createdAt: String,
    val reply: ReplyRef? = null,
    val facets: List<Facet>? = null,  // For mentions, links, etc.
    val embed: Embed? = null          // For rich media content
)

data class ReplyRef(
    val parent: Reference,
    val root: Reference
)

data class Reference(
    val uri: String,
    val cid: String
)

data class Author(
    val did: String,         // Decentralized identifier
    val handle: String,      // User handle
    val displayName: String? = null,
    val avatar: String? = null
)

data class Facet(
    val index: FacetIndex,
    val features: List<FacetFeature>
)

data class FacetIndex(
    val byteStart: Int,
    val byteEnd: Int
)

sealed class FacetFeature {
    data class Mention(val did: String) : FacetFeature()
    data class Link(val uri: String) : FacetFeature()
    data class Tag(val tag: String) : FacetFeature()
}

sealed class Embed {
    data class Images(val images: List<Image>) : Embed()
    data class External(val uri: String, val title: String, val description: String?) : Embed()
    data class Record(val uri: String, val cid: String) : Embed()
}

data class Image(
    val alt: String,
    val image: String,  // URI to the image
    val aspectRatio: Float? = null
) 