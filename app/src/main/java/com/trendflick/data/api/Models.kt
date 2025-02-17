package com.trendflick.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import android.net.Uri

/**
 * AT Protocol Models
 * Reference: https://atproto.com/specs/atp#session
 */

// Session Models
@JsonClass(generateAdapter = true)
data class AtSession(
    @field:Json(name = "did") val did: String,
    @field:Json(name = "handle") val handle: String,
    @field:Json(name = "accessJwt") val accessJwt: String,
    @field:Json(name = "refreshJwt") val refreshJwt: String,
    @field:Json(name = "email") val email: String? = null
)

// Identity Models
@JsonClass(generateAdapter = true)
data class AtIdentity(
    @field:Json(name = "did") val did: String,
    @field:Json(name = "handle") val handle: String,
    @field:Json(name = "displayName") val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class AtProfile(
    @field:Json(name = "did") val did: String,
    @field:Json(name = "handle") val handle: String,
    @field:Json(name = "displayName") val displayName: String? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "avatar") val avatar: String? = null,
    @field:Json(name = "viewer") val viewer: AtProfileViewer? = null,
    @field:Json(name = "indexedAt") val indexedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AtProfileViewer(
    @field:Json(name = "muted") val muted: Boolean = false,
    @field:Json(name = "blockedBy") val blockedBy: Boolean = false,
    @field:Json(name = "following") val following: String? = null,
    @field:Json(name = "followedBy") val followedBy: String? = null
)

// Request Models
@JsonClass(generateAdapter = true)
data class CreatePostRequest(
    @field:Json(name = "repo") val repo: String,
    @field:Json(name = "collection") val collection: String = "app.bsky.feed.post",
    @field:Json(name = "record") val record: PostRecord
)

@JsonClass(generateAdapter = true)
data class LikeRequest(
    @field:Json(name = "repo") val repo: String,
    @field:Json(name = "collection") val collection: String = "app.bsky.feed.like",
    @field:Json(name = "record") val record: LikeRecord,
    @field:Json(name = "rkey") val rkey: String
)

@JsonClass(generateAdapter = true)
data class RepostRequest(
    @field:Json(name = "repo") val repo: String,
    @field:Json(name = "collection") val collection: String = "app.bsky.feed.repost",
    @field:Json(name = "record") val record: RepostRecord
)

// Record Models
@JsonClass(generateAdapter = true)
data class PostRecord(
    @field:Json(name = "\$type") val type: String = "app.bsky.feed.post",
    @field:Json(name = "text") val text: String,
    @field:Json(name = "createdAt") val createdAt: String,
    @field:Json(name = "reply") val reply: ReplyReference? = null,
    @field:Json(name = "embed") val embed: Embed? = null,
    @field:Json(name = "facets") val facets: List<Facet>? = null
)

@JsonClass(generateAdapter = true)
data class LikeRecord(
    @field:Json(name = "\$type") val type: String = "app.bsky.feed.like",
    @field:Json(name = "subject") val subject: PostReference,
    @field:Json(name = "createdAt") val createdAt: String,
    @field:Json(name = "indexedAt") val indexedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RepostRecord(
    @field:Json(name = "\$type") val type: String = "app.bsky.feed.repost",
    @field:Json(name = "subject") val subject: PostReference,
    @field:Json(name = "createdAt") val createdAt: String
)

// Reference Models
@JsonClass(generateAdapter = true)
data class ReplyReference(
    @field:Json(name = "parent") val parent: PostReference,
    @field:Json(name = "root") val root: PostReference
)

@JsonClass(generateAdapter = true)
data class PostReference(
    @field:Json(name = "uri") val uri: String,
    @field:Json(name = "cid") val cid: String
)

// Response Models
@JsonClass(generateAdapter = true)
data class CreateRecordResponse(
    @field:Json(name = "uri") val uri: String,
    @field:Json(name = "cid") val cid: String
)

@JsonClass(generateAdapter = true)
data class GetRepostsResponse(
    @field:Json(name = "repostedBy") val repostedBy: List<AtProfile>,
    @field:Json(name = "cursor") val cursor: String?,
    @field:Json(name = "uri") val uri: String
)

@JsonClass(generateAdapter = true)
data class BlobResponse(
    @field:Json(name = "blob") val blob: BlobRef
)

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    @field:Json(name = "did") val did: String,
    @field:Json(name = "handle") val handle: String,
    @field:Json(name = "displayName") val displayName: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "avatar") val avatar: String?
)

// Timeline Models
@JsonClass(generateAdapter = true)
data class TimelineResponse(
    @field:Json(name = "feed") val feed: List<FeedPost>,
    @field:Json(name = "cursor") val cursor: String?
)

@JsonClass(generateAdapter = true)
data class ThreadResponse(
    @field:Json(name = "thread") val thread: ThreadPost
)

@JsonClass(generateAdapter = true)
data class FeedPost(
    @field:Json(name = "post") val post: Post,
    @field:Json(name = "reply") val reply: ReplyRef? = null,
    @field:Json(name = "reason") val reason: ReasonType? = null
)

@JsonClass(generateAdapter = true)
data class ThreadPost(
    @field:Json(name = "post") val post: Post,
    @field:Json(name = "parent") val parent: ThreadPost? = null,
    @field:Json(name = "replies") val replies: List<ThreadPost>? = null
)

@JsonClass(generateAdapter = true)
data class Post(
    @field:Json(name = "uri") val uri: String,
    @field:Json(name = "cid") val cid: String,
    @field:Json(name = "author") val author: Author,
    @field:Json(name = "record") val record: PostRecord,
    @field:Json(name = "embed") val embed: Embed?,
    @field:Json(name = "indexedAt") val indexedAt: String,
    @field:Json(name = "likeCount") val likeCount: Int?,
    @field:Json(name = "replyCount") val replyCount: Int?,
    @field:Json(name = "repostCount") val repostCount: Int?,
    @field:Json(name = "viewer") val viewer: PostViewer?
)

@JsonClass(generateAdapter = true)
data class Author(
    @field:Json(name = "did") val did: String,
    @field:Json(name = "handle") val handle: String,
    @field:Json(name = "displayName") val displayName: String? = null,
    @field:Json(name = "avatar") val avatar: String? = null
)

// Embed Models
@JsonClass(generateAdapter = true)
data class Embed(
    @field:Json(name = "\$type") val type: String? = null,
    @field:Json(name = "images") val images: List<ImageEmbed>? = null,
    @field:Json(name = "video") val video: VideoEmbed? = null,
    @field:Json(name = "external") val external: ExternalEmbed? = null
) {
    fun getValidImages(): List<ImageEmbed> {
        return images?.filterNotNull()?.filter { it.isValid() } ?: emptyList()
    }
}

@JsonClass(generateAdapter = true)
data class ExternalEmbed(
    @field:Json(name = "uri") val uri: String,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "thumb") val thumb: BlobRef? = null,
    @field:Json(name = "oEmbedUrl") val oEmbedUrl: String? = null,
    @field:Json(name = "siteName") val siteName: String? = null,
    @field:Json(name = "platform") val platform: String? = null
) {
    fun getSocialMediaInfo(): SocialMediaInfo? {
        val uri = Uri.parse(uri)
        val host = uri.host ?: return null
        return when {
            host.contains("twitter.com") || host.contains("x.com") -> 
                SocialMediaInfo("X/Twitter", "post")
            host.contains("instagram.com") -> 
                SocialMediaInfo("Instagram", if (uri.path?.contains("/p/") == true) "post" else "profile")
            host.contains("youtube.com") || host.contains("youtu.be") -> 
                SocialMediaInfo("YouTube", "video")
            host.contains("tiktok.com") -> 
                SocialMediaInfo("TikTok", if (uri.path?.contains("/video/") == true) "video" else "post")
            else -> null
        }
    }
}

data class SocialMediaInfo(
    val platform: String,
    val type: String
)

@JsonClass(generateAdapter = true)
data class VideoEmbed(
    @field:Json(name = "ref") val ref: BlobRef,
    @field:Json(name = "mimeType") val mimeType: String,
    @field:Json(name = "size") val size: Int,
    @field:Json(name = "aspectRatio") val aspectRatio: AspectRatio? = null
)

@JsonClass(generateAdapter = true)
data class ImageEmbed(
    @field:Json(name = "alt") val alt: String = "",
    @field:Json(name = "image") val image: BlobRef? = null,
    @field:Json(name = "thumb") val thumb: String? = null,
    @field:Json(name = "fullsize") val fullsize: String? = null
) {
    fun isValid(): Boolean = image != null && image.link != null
}

@JsonClass(generateAdapter = true)
data class BlobRef(
    @field:Json(name = "\$link") val link: String? = null,
    @field:Json(name = "mimeType") val mimeType: String? = null,
    @field:Json(name = "size") val size: Long? = null
)

@JsonClass(generateAdapter = true)
data class AspectRatio(
    @field:Json(name = "width") val width: Int,
    @field:Json(name = "height") val height: Int
)

@JsonClass(generateAdapter = true)
data class PostViewer(
    @field:Json(name = "like") val like: String?,
    @field:Json(name = "repost") val repost: String?
)

@JsonClass(generateAdapter = true)
data class ReplyRef(
    @field:Json(name = "root") val root: PostRef,
    @field:Json(name = "parent") val parent: PostRef
)

@JsonClass(generateAdapter = true)
data class PostRef(
    @field:Json(name = "uri") val uri: String,
    @field:Json(name = "cid") val cid: String
)

@JsonClass(generateAdapter = true)
data class ReasonType(
    @field:Json(name = "\$type") val type: String? = null,
    @field:Json(name = "by") val by: Author? = null,
    @field:Json(name = "indexedAt") val indexedAt: String? = null,
    @field:Json(name = "repost") val repost: RepostInfo? = null
)

@JsonClass(generateAdapter = true)
data class RepostInfo(
    @field:Json(name = "uri") val uri: String,
    @field:Json(name = "cid") val cid: String,
    @field:Json(name = "author") val author: Author,
    @field:Json(name = "record") val record: PostRecord,
    @field:Json(name = "indexedAt") val indexedAt: String
)

@JsonClass(generateAdapter = true)
data class GetTimelineRequest(
    @field:Json(name = "algorithm") val algorithm: String = "reverse-chronological",
    @field:Json(name = "limit") val limit: Int = 50,
    @field:Json(name = "cursor") val cursor: String? = null
)

@JsonClass(generateAdapter = true)
data class GetTimelineResponse(
    @field:Json(name = "feed") val feed: List<FeedView>,
    @field:Json(name = "cursor") val cursor: String?
)

@JsonClass(generateAdapter = true)
data class GetPostThreadResponse(
    @field:Json(name = "thread") val thread: ThreadView,
    @field:Json(name = "uri") val uri: String
)

@JsonClass(generateAdapter = true)
data class ThreadView(
    @field:Json(name = "post") val post: Post,
    @field:Json(name = "parent") val parent: ThreadView?,
    @field:Json(name = "replies") val replies: List<ThreadView>?
)

@JsonClass(generateAdapter = true)
data class FeedView(
    @field:Json(name = "post") val post: Post,
    @field:Json(name = "reply") val reply: ReplyRef?,
    @field:Json(name = "reason") val reason: ReasonType?
)

@JsonClass(generateAdapter = true)
data class VideoModel(
    val uri: String,
    val title: String?,
    val description: String,
    val thumbnailUrl: String?,
    val videoUrl: String?,
    val authorDid: String,
    val authorHandle: String,
    val authorName: String?,
    val authorAvatar: String?,
    val createdAt: String,
    val likes: Int = 0,
    val comments: Int = 0,
    val reposts: Int = 0,
    val aspectRatio: Float? = null
)

@JsonClass(generateAdapter = true)
data class GetLikesResponse(
    @field:Json(name = "likes") val likes: List<Like>,
    @field:Json(name = "cursor") val cursor: String?
)

@JsonClass(generateAdapter = true)
data class Like(
    @field:Json(name = "actor") val actor: AtIdentity,
    @field:Json(name = "createdAt") val createdAt: String,
    @field:Json(name = "indexedAt") val indexedAt: String?
)

@JsonClass(generateAdapter = true)
data class FollowsResponse(
    @field:Json(name = "follows") val follows: List<AtProfile>,
    @field:Json(name = "cursor") val cursor: String?
) 