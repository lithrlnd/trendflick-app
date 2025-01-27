package com.trendflick.data.api

import com.trendflick.data.model.AtSession
import okhttp3.MultipartBody
import retrofit2.http.*

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

    data class SearchUsersResponse(
        val users: List<UserProfile>
    )

    data class UserProfile(
        val did: String,
        val handle: String,
        val displayName: String?,
        val avatar: String? = null
    )

    @GET("xrpc/app.bsky.actor.searchActors")
    suspend fun searchUsers(
        @Query("term") query: String,
        @Query("limit") limit: Int = 10
    ): SearchUsersResponse

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

    data class CreateRecordRequest(
        val repo: String,
        val collection: String,
        val record: Any,
        val rkey: String? = null,
        val validate: Boolean = true,
        val swapCommit: String? = null
    )

    data class RepostRecord(
        val type: String = "app.bsky.feed.repost",
        val subject: PostReference,
        val createdAt: String
    )

    data class PostRecord(
        val type: String = "app.bsky.feed.post",
        val text: String,
        val createdAt: String,
        val reply: ReplyReference? = null,
        val embed: RecordEmbed? = null
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
} 