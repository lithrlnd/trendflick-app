package com.trendflick.data.api

import com.trendflick.data.model.AtSession
import okhttp3.MultipartBody
import retrofit2.http.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchUsersResponse(
    val actors: List<UserProfile>
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val did: String,
    val handle: String,
    val displayName: String?,
    val avatar: String? = null
)

interface AtProtocolService {
    @POST("xrpc/com.atproto.server.createSession")
    @Headers("Content-Type: application/json")
    suspend fun createSession(@Body credentials: Map<String, String>): AtSession

    @POST("xrpc/com.atproto.server.refreshSession")
    @Headers("Content-Type: application/json")
    suspend fun refreshSession(): AtSession

    @GET("xrpc/app.bsky.feed.getTimeline")
    suspend fun getTimeline(
        @Query("algorithm") algorithm: String = "reverse-chronological",
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): TimelineResponse

    @GET("xrpc/app.bsky.feed.getAuthorFeed")
    suspend fun getAuthorFeed(
        @Query("actor") actor: String,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): TimelineResponse

    @GET("xrpc/app.bsky.feed.getFeed")
    suspend fun getDiscoveryFeed(
        @Query("feed") feed: String = "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.generator/whats-hot",
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): TimelineResponse

    @GET("xrpc/app.bsky.feed.getPostThread")
    suspend fun getPostThread(
        @Query("uri") uri: String,
        @Query("depth") depth: Int? = null
    ): ThreadResponse

    @POST("xrpc/com.atproto.repo.createRecord")
    suspend fun createRecord(@Body request: CreateRecordRequest): CreateRecordResponse

    @POST("xrpc/com.atproto.repo.createRecord")
    suspend fun createLike(@Body request: LikeRequest): CreateRecordResponse

    @POST("xrpc/com.atproto.repo.createRecord")
    suspend fun createRepost(@Body request: RepostRequest): CreateRecordResponse

    @POST("xrpc/com.atproto.repo.uploadBlob")
    @Headers("Content-Type: image/*")
    suspend fun uploadBlob(@Body bytes: ByteArray): BlobResponse

    @POST("xrpc/com.atproto.repo.putRecord")
    suspend fun updateProfile(@Body profile: Map<String, String>): ProfileResponse

    @DELETE("xrpc/com.atproto.repo.deleteRecord")
    suspend fun deleteRecord(
        @Query("repo") repo: String,
        @Query("collection") collection: String,
        @Query("rkey") rkey: String
    )

    @GET("xrpc/app.bsky.feed.getLikes")
    suspend fun getLikes(
        @Query("uri") uri: String,
        @Query("limit") limit: Int = 1,
        @Query("cursor") cursor: String? = null
    ): GetLikesResponse

    @GET("xrpc/app.bsky.feed.getRepostedBy")
    suspend fun getReposts(
        @Query("uri") uri: String,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): GetRepostedByResponse

    @GET("xrpc/app.bsky.actor.searchActors")
    suspend fun searchUsers(@Query("term") query: String): SearchUsersResponse

    @GET("xrpc/app.bsky.feed.searchPosts")
    suspend fun searchHashtags(@Query("term") query: String): HashtagSearchResponse

    @GET("xrpc/com.atproto.identity.resolveHandle")
    suspend fun identityResolveHandle(
        @Query("handle") handle: String
    ): ResolveHandleResponse

    @POST("xrpc/com.atproto.server.deleteSession")
    suspend fun deleteSession(@Body refreshJwt: String): Result<Unit>

    @GET("xrpc/app.bsky.graph.getFollows")
    suspend fun getFollows(
        @Query("actor") actor: String,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): FollowsResponse

    @GET("xrpc/app.bsky.feed.getHashtagFeed")
    suspend fun getPostsByHashtag(
        @Query("hashtag") hashtag: String
    ): TimelineResponse

    @GET("xrpc/app.bsky.graph.getHashtagFollowStatus")
    suspend fun checkHashtagFollowStatus(
        @Query("hashtag") hashtag: String
    ): HashtagFollowResponse

    @POST("xrpc/app.bsky.graph.followHashtag")
    suspend fun followHashtag(
        @Query("hashtag") hashtag: String
    )

    @POST("xrpc/app.bsky.graph.unfollowHashtag")
    suspend fun unfollowHashtag(
        @Query("hashtag") hashtag: String
    )

    @POST("xrpc/com.atproto.repo.createRecord")
    suspend fun follow(
        @Body request: FollowRequest
    ): CreateRecordResponse

    @DELETE("xrpc/com.atproto.repo.deleteRecord")
    suspend fun unfollow(
        @Query("repo") repo: String,
        @Query("collection") collection: String = "app.bsky.graph.follow",
        @Query("rkey") rkey: String
    )

    @GET("xrpc/app.bsky.graph.getFollow")
    suspend fun getFollow(
        @Query("repo") repo: String,
        @Query("rkey") rkey: String
    ): GetFollowResponse

    // Base record type for all AT Protocol records
    sealed class Record {
        abstract val type: String
        abstract val createdAt: String
    }

    @JsonClass(generateAdapter = true)
    data class PostRecord(
        @Json(name = "\$type") override val type: String = "app.bsky.feed.post",
        override val createdAt: String,
        val text: String,
        val facets: List<Facet>? = null,
        val reply: ReplyReference? = null,
        val embed: RecordEmbed? = null
    ) : Record()

    @JsonClass(generateAdapter = true)
    data class RepostRecord(
        @Json(name = "\$type") override val type: String = "app.bsky.feed.repost",
        override val createdAt: String,
        val subject: PostReference
    ) : Record()

    data class CreateRecordRequest(
        val repo: String,
        val collection: String,
        val record: Record,
        val rkey: String? = null
    )

    data class Facet(
        val index: ByteIndex,
        val features: List<Feature>
    )

    data class ByteIndex(
        val byteStart: Int,
        val byteEnd: Int
    )

    sealed class Feature {
        @JsonClass(generateAdapter = true)
        data class Mention(
            @Json(name = "\$type") val type: String = "app.bsky.richtext.facet#mention",
            val did: String
        ) : Feature()

        @JsonClass(generateAdapter = true)
        data class Link(
            @Json(name = "\$type") val type: String = "app.bsky.richtext.facet#link",
            val uri: String
        ) : Feature()

        @JsonClass(generateAdapter = true)
        data class Tag(
            @Json(name = "\$type") val type: String = "app.bsky.richtext.facet#tag",
            val tag: String
        ) : Feature()
    }

    data class ResolveHandleResponse(
        val did: String
    )

    data class RecordEmbed(
        val type: String = "app.bsky.embed.record",
        val record: StrongRef
    )

    data class StrongRef(
        val uri: String,
        val cid: String
    )

    data class ReplyReference(
        val parent: PostReference,
        val root: PostReference
    )

    data class PostReference(
        val uri: String,
        val cid: String
    )

    data class GetRepostedByResponse(
        val repostedBy: List<AtProfile>,
        val uri: String,
        val cursor: String?
    )

    data class RepostViewer(
        val repost: String?,
        val repostedAt: String?
    )

    data class RepostView(
        val uri: String,
        val cid: String,
        val author: AtProfile,
        val record: RepostRecord,
        val indexedAt: String
    )

    data class RepostsResponse(
        val uri: String,
        val reposts: List<RepostView>,
        val cursor: String?,
        val viewer: RepostViewer?
    )

    data class HashtagSearchResponse(
        val tags: List<HashtagResult>
    )

    data class HashtagResult(
        val tag: String,
        val count: Int
    )

    @JsonClass(generateAdapter = true)
    data class FollowRequest(
        @Json(name = "repo") val repo: String,
        @Json(name = "collection") val collection: String = "app.bsky.graph.follow",
        @Json(name = "record") val record: FollowRecord,
        @Json(name = "rkey") val rkey: String? = null
    )

    @JsonClass(generateAdapter = true)
    data class FollowRecord(
        @Json(name = "\$type") val type: String = "app.bsky.graph.follow",
        @Json(name = "subject") val subject: String,
        @Json(name = "createdAt") val createdAt: String
    )

    @JsonClass(generateAdapter = true)
    data class GetFollowResponse(
        @Json(name = "uri") val uri: String,
        @Json(name = "cid") val cid: String,
        @Json(name = "value") val value: FollowRecord
    )
} 