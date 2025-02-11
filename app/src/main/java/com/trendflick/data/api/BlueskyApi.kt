package com.trendflick.data.api

import retrofit2.http.*

/**
 * Extension of AtProtocolService specifically for Bluesky social features.
 * Uses the existing AT Protocol infrastructure.
 */
interface BlueskyApi : AtProtocolService {
    // Additional Bluesky-specific endpoints can be added here
    // Most functionality is already provided by AtProtocolService

    @POST("xrpc/app.bsky.feed.getTimeline")
    suspend fun getTimeline(
        @Body request: GetTimelineRequest
    ): GetTimelineResponse

    @GET("xrpc/app.bsky.feed.getPostThread")
    suspend fun getPostThread(
        @Query("uri") uri: String
    ): GetPostThreadResponse

    @POST("xrpc/app.bsky.feed.like")
    suspend fun createLike(
        @Query("uri") uri: String
    )

    @DELETE("xrpc/app.bsky.feed.like")
    suspend fun deleteLike(
        @Query("uri") uri: String
    )

    @POST("xrpc/app.bsky.feed.repost")
    suspend fun createRepost(
        @Query("uri") uri: String
    )

    @DELETE("xrpc/app.bsky.feed.repost")
    suspend fun deleteRepost(
        @Query("uri") uri: String
    )

    @POST("xrpc/app.bsky.feed.post")
    suspend fun createPost(
        @Query("text") text: String,
        @Query("reply") reply: String? = null
    ): Post
} 