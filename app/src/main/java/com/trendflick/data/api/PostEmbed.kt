package com.trendflick.data.api

sealed class PostEmbed {
    data class Images(
        val images: List<ImageEmbed>
    ) : PostEmbed()

    data class External(
        val external: ExternalEmbed
    ) : PostEmbed()

    data class Record(
        val record: RecordEmbed
    ) : PostEmbed()
}

data class ImageEmbed(
    val fullsize: String,
    val thumb: String,
    val alt: String? = null
)

data class RecordEmbed(
    val uri: String,
    val cid: String,
    val author: EmbedAuthor
)

data class EmbedAuthor(
    val did: String,
    val handle: String,
    val displayName: String? = null,
    val avatar: String? = null
) 