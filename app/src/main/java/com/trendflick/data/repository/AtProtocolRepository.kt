package com.trendflick.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.trendflick.data.api.*
import com.trendflick.data.local.UserDao
import com.trendflick.data.model.AtSession
import com.trendflick.data.model.User
import com.trendflick.data.model.TrendingHashtag
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.format.DateTimeFormatter

interface AtProtocolRepository {
    suspend fun createSession(handle: String, password: String): Result<AtSession>
    suspend fun refreshSession(): Result<AtSession>
    suspend fun uploadBlob(uri: Uri): BlobResult
    suspend fun updateProfile(did: String, displayName: String?, description: String?, avatar: String?)
    suspend fun getTimeline(algorithm: String = "reverse-chronological", limit: Int = 50, cursor: String? = null): Result<TimelineResponse>
    suspend fun getPostThread(uri: String): Result<ThreadResponse>
    suspend fun likePost(uri: String): Boolean
    suspend fun isPostLikedByUser(uri: String): Boolean
    suspend fun repost(uri: String, cid: String)
    suspend fun isPostRepostedByUser(uri: String): Boolean
    fun getUserByDid(did: String): Flow<User?>
    fun getUserByHandle(handle: String): Flow<User?>
    suspend fun createPost(text: String, timestamp: String): Result<CreateRecordResponse>
    suspend fun createReply(text: String, parentUri: String, parentCid: String, timestamp: String): Result<CreateRecordResponse>
    suspend fun searchUsers(query: String): List<UserSearchResult>
    suspend fun getCurrentSession(): AtProtocolUser?
    suspend fun createPost(record: Map<String, Any>): AtProtocolPostResult
    suspend fun identityResolveHandle(handle: String): String
    suspend fun getDid(): String
    suspend fun getHandle(): String
    fun clearLikeCache()
    suspend fun deleteSession(refreshJwt: String): Result<Unit>
    suspend fun getFollows(actor: String, limit: Int = 50, cursor: String? = null): Result<FollowsResponse>
    suspend fun getTrendingHashtags(): List<TrendingHashtag>
    suspend fun getPostsByHashtag(hashtag: String, limit: Int = 50, cursor: String? = null): Result<TimelineResponse>
    suspend fun ensureValidSession(): Boolean
    suspend fun searchHandles(query: String): List<UserSearchResult>
    suspend fun searchHashtags(query: String): List<TrendingHashtag>
    suspend fun createQuotePost(text: String, quotedPostUri: String, quotedPostCid: String): Result<CreateRecordResponse>
}

// Data classes used by the interface
data class AtProtocolUser(
    val did: String,
    val handle: String
)

data class AtProtocolPostResult(
    val uri: String,
    val cid: String
)

data class BlobResult(
    val blobUri: String,
    val mimeType: String
)

data class UserSearchResult(
    val did: String,
    val handle: String,
    val displayName: String?,
    val avatar: String? = null
) 