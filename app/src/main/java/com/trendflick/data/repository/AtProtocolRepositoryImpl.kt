package com.trendflick.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.trendflick.data.api.*
import com.trendflick.data.local.UserDao
import com.trendflick.data.model.AtSession
import com.trendflick.data.model.User
import com.trendflick.data.model.TrendingHashtag
import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import kotlinx.coroutines.delay
import com.trendflick.data.auth.BlueskyCredentialsManager
import java.text.SimpleDateFormat
import java.util.*
import com.trendflick.ui.model.AIEnhancement
import java.nio.charset.StandardCharsets

@Singleton
class AtProtocolRepositoryImpl @Inject constructor(
    private val service: AtProtocolService,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val credentialsManager: BlueskyCredentialsManager
) : AtProtocolRepository {

    private var lastSessionAttempt: Long = 0
    private var consecutiveFailures: Int = 0
    private val baseRetryInterval = 60_000L // 1 minute base interval
    private val likeStatusCache = mutableMapOf<String, Pair<Boolean, Long>>()
    private val repostStatusCache = mutableMapOf<String, Pair<Boolean, Long>>()
    private val cacheDuration = 60_000L // 1 minute cache
    private val TAG = "TF_Repository"

    private suspend fun waitForRateLimit(retryCount: Int) {
        val waitTime = when (retryCount) {
            0 -> 1000L    // 1 second
            1 -> 5000L    // 5 seconds
            2 -> 15000L   // 15 seconds
            else -> 30000L // 30 seconds
        }
        Log.d(TAG, "⏳ Rate limited, waiting for $waitTime ms")
        delay(waitTime)
    }

    override suspend fun refreshSession(): Result<AtSession> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Attempting to refresh session")
            
            // Get current credentials
            val handle = credentialsManager.getHandle()
            val password = credentialsManager.getPassword()
            
            if (handle.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.e(TAG, "❌ No credentials available for refresh")
                return@withContext Result.failure(Exception("No credentials available"))
            }
            
            // Try to refresh first
            try {
                Log.d(TAG, "🔄 Attempting token refresh")
                val refreshResult = service.refreshSession()
                sessionManager.saveSession(refreshResult)
                return@withContext Result.success(refreshResult)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Refresh failed, falling back to new session: ${e.message}")
            }
            
            // If refresh fails, try creating a new session
            Log.d(TAG, "🔄 Creating new session")
            try {
                val credentials = mapOf(
                    "identifier" to handle,
                    "password" to password
                )
                val result = service.createSession(credentials)
                sessionManager.saveSession(result)
                
                Log.d(TAG, "✅ New session created successfully for ${result.handle}")
                return@withContext Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create new session: ${e.message}")
                return@withContext Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session refresh failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    override suspend fun ensureValidSession(): Boolean {
        try {
            Log.d(TAG, "🔐 Validating session")
            
            // Check if we have valid credentials
            val handle = credentialsManager.getHandle()
            val password = credentialsManager.getPassword()
            
            if (handle.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.e(TAG, "❌ No valid credentials found")
                return false
            }
            
            // Check if we have a valid access token
            val accessToken = sessionManager.getAccessToken()
            if (!accessToken.isNullOrEmpty()) {
                Log.d(TAG, "✅ Found valid access token")
                return true
            }
            
            Log.d(TAG, "⚠️ No valid session found, attempting refresh")
            val refreshResult = refreshSession()
            return refreshResult.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session validation failed: ${e.message}")
            return false
        }
    }

    override suspend fun getDid(): String {
        return getCurrentSession()?.did ?: throw IllegalStateException("Not authenticated")
    }

    override suspend fun getHandle(): String {
        return getCurrentSession()?.handle ?: throw IllegalStateException("Not authenticated")
    }

    override suspend fun createSession(handle: String, password: String): Result<AtSession> {
        return try {
            Log.d(TAG, "🔐 Creating session for handle: $handle")
            
            // Clear any existing session first
            sessionManager.clearSession()
            
            val credentials = mapOf(
                "identifier" to handle,
                "password" to password
            )

            try {
                val session = service.createSession(credentials)
                
                // Validate session response
                if (session.accessJwt.isNullOrEmpty() || session.refreshJwt.isNullOrEmpty() || 
                    session.did.isNullOrEmpty() || session.handle.isNullOrEmpty()) {
                    Log.e(TAG, "❌ Invalid session response: Missing required fields")
                    return Result.failure(Exception("Invalid session response"))
                }
                
                Log.d(TAG, """
                    ✅ Session created successfully:
                    DID: ${session.did}
                    Handle: ${session.handle}
                    Has Access Token: ${!session.accessJwt.isNullOrEmpty()}
                    Has Refresh Token: ${!session.refreshJwt.isNullOrEmpty()}
                """.trimIndent())
                
                // Save credentials for future use
                credentialsManager.saveCredentials(handle, password)
                
                // Save the session
                sessionManager.saveSession(session)
                
                Result.success(session)
            } catch (e: HttpException) {
                when (e.code()) {
                    400 -> {
                        Log.e(TAG, "❌ Invalid credentials or token type: ${e.message()}")
                        Result.failure(Exception("Invalid credentials"))
                    }
                    401 -> {
                        Log.e(TAG, "❌ Unauthorized: ${e.message()}")
                        Result.failure(Exception("Unauthorized"))
                    }
                    429 -> {
                        Log.e(TAG, "❌ Rate limited: ${e.message()}")
                        Result.failure(Exception("Too many attempts"))
                    }
                    else -> {
                        Log.e(TAG, "❌ HTTP error during session creation: ${e.code()} - ${e.message()}")
                        Result.failure(e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session creation failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun uploadBlob(uri: Uri): BlobResult {
        try {
            val file = createTempFileFromUri(uri)
            val mimeType = getMimeType(uri) ?: "image/jpeg"
            val bytes = file.readBytes()
            val response = service.uploadBlob(bytes)
            file.delete()
            
            return BlobResult(
                blobUri = response.blob.link ?: "",
                mimeType = mimeType
            )
        } catch (e: Exception) {
            throw Exception("Failed to upload image: ${e.message}")
        }
    }

    override suspend fun updateProfile(
        did: String,
        displayName: String?,
        description: String?,
        avatar: String?
    ) {
        try {
            val profile = mapOf(
                "did" to did,
                "displayName" to (displayName ?: ""),
                "description" to (description ?: ""),
                "avatar" to (avatar ?: "")
            )
            
            service.updateProfile(profile)
        } catch (e: Exception) {
            throw Exception("Failed to update profile: ${e.message}")
        }
    }

    override suspend fun getTimeline(
        algorithm: String,
        limit: Int,
        cursor: String?
    ): Result<TimelineResponse> = withContext(Dispatchers.IO) {
        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                // Ensure valid session before making request
                if (!ensureValidSession()) {
                    Log.e(TAG, "❌ No valid session available for timeline request")
                    return@withContext Result.failure(Exception("No valid session available"))
                }
                
                Log.d(TAG, """
                    🌐 Timeline Request:
                    Algorithm: $algorithm
                    Limit: $limit
                    Cursor: $cursor
                    Attempt: ${retryCount + 1}
                """.trimIndent())
                
                val response = try {
                    when (algorithm) {
                        "whats-hot" -> {
                            Log.d(TAG, "🔍 Using discovery feed endpoint with whats-hot algorithm")
                            service.getDiscoveryFeed(
                                feed = "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.generator/whats-hot",
                                limit = limit,
                                cursor = cursor
                            )
                        }
                        "reverse-chronological", "following" -> {
                            Log.d(TAG, "🔍 Using following timeline endpoint")
                            try {
                                service.getTimeline(
                                    algorithm = "reverse-chronological",
                                    limit = limit,
                                    cursor = cursor
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Failed with reverse-chronological, trying following algorithm")
                                service.getTimeline(
                                    algorithm = "following",
                                    limit = limit,
                                    cursor = cursor
                                )
                            }
                        }
                        else -> {
                            Log.d(TAG, "🔍 Using default timeline endpoint")
                            service.getTimeline(
                                algorithm = algorithm,
                                limit = limit,
                                cursor = cursor
                            )
                        }
                    }
                } catch (e: HttpException) {
                    when (e.code()) {
                        400 -> {
                            Log.e(TAG, "❌ Invalid request: ${e.message()}")
                            // Try personal timeline as fallback
                            Log.d(TAG, "🔄 Falling back to personal timeline")
                            service.getTimeline(
                                algorithm = "following",
                                limit = limit,
                                cursor = cursor
                            )
                        }
                        401 -> {
                            Log.w(TAG, "🔄 Token expired, attempting refresh")
                            val refreshResult = refreshSession()
                            if (refreshResult.isSuccess) {
                                Log.d(TAG, "✅ Session refreshed successfully")
                                continue // Retry with new token
                            } else {
                                Log.w(TAG, "🔄 Session refresh failed, clearing session")
                                sessionManager.clearSession()
                                throw Exception("Session expired")
                            }
                        }
                        429 -> {
                            Log.w(TAG, "⏳ Rate limited")
                            waitForRateLimit(retryCount)
                            throw e // Let the retry logic handle it
                        }
                        else -> {
                            Log.e(TAG, "❌ HTTP Error: ${e.code()} - ${e.message()}")
                            throw e
                        }
                    }
                }
                
                // Filter and validate posts
                val validatedResponse = response.copy(
                    feed = response.feed.filter { feedPost ->
                        try {
                            val post = feedPost.post
                            val isValid = post.uri.isNotEmpty() && post.cid.isNotEmpty() &&
                                // Validate reply references if they exist
                                (feedPost.reply?.let { reply ->
                                    reply.root?.uri?.isNotEmpty() == true && 
                                    reply.root?.cid?.isNotEmpty() == true &&
                                    reply.parent?.uri?.isNotEmpty() == true && 
                                    reply.parent?.cid?.isNotEmpty() == true
                                } ?: true)
                            
                            if (!isValid) {
                                Log.w(TAG, "⚠️ Filtered out invalid post: ${post.uri}")
                            }
                            isValid
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Error validating post: ${e.message}")
                            false
                        }
                    }
                )
                
                if (validatedResponse.feed.isEmpty()) {
                    Log.w(TAG, "⚠️ Empty feed received after validation")
                } else {
                    Log.d(TAG, """
                        ✅ Timeline Response:
                        Feed size: ${validatedResponse.feed.size}
                        Has cursor: ${validatedResponse.cursor != null}
                        First post: ${validatedResponse.feed.firstOrNull()?.post?.uri}
                    """.trimIndent())
                }
                
                return@withContext Result.success(validatedResponse)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Timeline fetch failed (Attempt ${retryCount + 1}): ${e.message}")
                
                // Check if this is an auth error
                if (e.message?.contains("Session expired") == true ||
                    e.message?.contains("Unauthorized") == true ||
                    e.message?.contains("Not authenticated") == true) {
                    Log.w(TAG, "🔒 Authentication error - attempting refresh")
                    val refreshResult = refreshSession()
                    if (refreshResult.isSuccess) {
                        Log.d(TAG, "✅ Session refreshed successfully")
                        continue // Retry with new token
                    } else {
                        Log.w(TAG, "🔒 Authentication error - stopping retries")
                        return@withContext Result.failure(e)
                    }
                }
                
                retryCount++
                
                if (retryCount >= maxRetries) {
                    return@withContext Result.failure(e)
                }
                
                // Wait before retrying
                delay(1000L * (1 shl retryCount))
            }
        }
        
        return@withContext Result.failure(Exception("Failed after $maxRetries retries"))
    }

    override suspend fun getPostThread(uri: String): Result<ThreadResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = service.getPostThread(uri)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(uri: String): Boolean {
        try {
            Log.d(TAG, "🌐 AT Protocol: Handling like operation for URI: $uri")
            val did = userDao.getCurrentUserDid() ?: sessionManager.getDid() 
                ?: throw IllegalStateException("No DID found")
            
            val isCurrentlyLiked = isPostLikedByUser(uri)
            Log.d(TAG, "🔍 Current like state - isLiked: $isCurrentlyLiked")
            
            return if (!isCurrentlyLiked) {
                // Create like record following AT Protocol schema
                val postThread = service.getPostThread(uri)
                val post = postThread.thread.post
                
                val request = LikeRequest(
                    repo = did,
                    collection = "app.bsky.feed.like",
                    record = LikeRecord(
                        type = "app.bsky.feed.like",
                        subject = PostReference(
                            uri = uri,
                            cid = post.cid
                        ),
                        createdAt = getCurrentTimestamp()
                    ),
                    rkey = generateStableRkey(uri)
                )
                
                try {
                    Log.d(TAG, "🌐 Creating like record in AT Protocol repository")
                    service.createLike(request)
                    likeStatusCache[uri] = true to System.currentTimeMillis()
                    Log.d(TAG, "✅ Like record created successfully")
                    true
                } catch (e: HttpException) {
                    if (e.code() == 409) {
                        // Record already exists, consider it a success
                        Log.d(TAG, "⚠️ Like record already exists (409)")
                        likeStatusCache[uri] = true to System.currentTimeMillis()
                        true
                    } else {
                        Log.e(TAG, "❌ Failed to create like record: ${e.message}")
                        throw e
                    }
                }
            } else {
                // Unlike by deleting the like record
                Log.d(TAG, "🌐 Retrieving existing like record for deletion")
                val likes = service.getLikes(uri, limit = 10).likes
                val myLike = likes.find { like -> like.actor.did == did }
                
                if (myLike != null) {
                    Log.d(TAG, "🌐 Deleting like record from AT Protocol repository")
                    service.deleteRecord(
                        repo = did,
                        collection = "app.bsky.feed.like",
                        rkey = generateStableRkey(uri)
                    )
                    likeStatusCache[uri] = false to System.currentTimeMillis()
                    Log.d(TAG, "✅ Like record deleted successfully")
                    false
                } else {
                    Log.d(TAG, "⚠️ No like record found to delete")
                    likeStatusCache[uri] = false to System.currentTimeMillis()
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Like operation failed: ${e.message}")
            throw e
        }
    }

    override suspend fun isPostLikedByUser(uri: String): Boolean {
        val cached = likeStatusCache[uri]
        if (cached != null) {
            val (status, timestamp) = cached
            if (System.currentTimeMillis() - timestamp < cacheDuration) {
                Log.d(TAG, "🔍 Using cached like status for $uri: $status")
                return status
            }
        }

        return try {
            Log.d(TAG, "🌐 Fetching like status from AT Protocol for $uri")
            val did = userDao.getCurrentUserDid() ?: sessionManager.getDid()
            val response = service.getLikes(uri, limit = 1)
            val isLiked = response.likes.any { like -> like.actor.did == did }
            likeStatusCache[uri] = isLiked to System.currentTimeMillis()
            Log.d(TAG, "✅ Like status fetched successfully: $isLiked")
            isLiked
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch like status: ${e.message}")
            false
        }
    }

    override suspend fun repost(uri: String, cid: String) {
        try {
            Log.d(TAG, """
                🔄 Starting repost operation:
                URI: $uri
                CID: $cid
            """.trimIndent())
            
            val did = sessionManager.getDid() ?: throw IllegalStateException("No DID found")
            
            // Check current repost status first
            val currentlyReposted = isPostRepostedByUser(uri)
            Log.d(TAG, "🔍 Current repost status: $currentlyReposted")
            
            if (!currentlyReposted) {
                val record = AtProtocolService.RepostRecord(
                    createdAt = getCurrentTimestamp(),
                    subject = AtProtocolService.PostReference(
                        uri = uri,
                        cid = cid
                    )
                )
                
                val request = AtProtocolService.CreateRecordRequest(
                    repo = did,
                    collection = "app.bsky.feed.repost",
                    record = record,
                    rkey = generateStableRkey("repost_$cid")
                )
                
                service.createRecord(request)
                Log.d(TAG, "✅ Repost created successfully")
                
                // Clear cache for this URI to force a fresh check
                repostStatusCache.remove(uri)
                
                // Verify repost status with retries
                var verified = false
                var attempts = 0
                while (!verified && attempts < 3) {
                    delay(1000L * (attempts + 1)) // Increasing delay between attempts
                    verified = isPostRepostedByUser(uri)
                    if (verified) {
                        Log.d(TAG, "✅ Repost verified on attempt ${attempts + 1}")
                        break
                    }
                    attempts++
                }
                
                if (!verified) {
                    Log.w(TAG, "⚠️ Repost creation succeeded but verification failed")
                }
            } else {
                Log.d(TAG, "⚠️ Post already reposted")
            }
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Failed to create repost:
                URI: $uri
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            throw e
        }
    }

    override suspend fun isPostRepostedByUser(uri: String): Boolean {
        // Check cache first
        val cached = repostStatusCache[uri]
        if (cached != null) {
            val (status, timestamp) = cached
            if (System.currentTimeMillis() - timestamp < cacheDuration) {
                Log.d(TAG, "🔍 Using cached repost status for $uri: $status")
                return status
            }
        }

        return try {
            Log.d(TAG, "🌐 Fetching repost status from AT Protocol for $uri")
            val did = userDao.getCurrentUserDid() ?: sessionManager.getDid() 
                ?: throw IllegalStateException("No DID found")
            
            // Fetch reposts with increased limit and pagination
            var cursor: String? = null
            var isReposted = false
            var totalChecked = 0
            val maxToCheck = 500 // Increased max to check
            
            do {
                val response = service.getReposts(uri, limit = 100, cursor = cursor)
                
                // Enhanced logging for repost checking
                Log.d(TAG, """
                    📝 Repost Check Details (Page):
                    URI: $uri
                    Reposters in current page: ${response.repostedBy.size}
                    Total checked so far: $totalChecked
                    Current User DID: $did
                    Has Matching DID: ${response.repostedBy.any { it.did == did }}
                """.trimIndent())
                
                if (response.repostedBy.any { it.did == did }) {
                    isReposted = true
                    break
                }
                
                totalChecked += response.repostedBy.size
                cursor = response.cursor
            } while (cursor != null && totalChecked < maxToCheck && !isReposted)
            
            // Update cache with new status
            repostStatusCache[uri] = isReposted to System.currentTimeMillis()
            
            Log.d(TAG, """
                ✅ Repost Status Result:
                Is Reposted: $isReposted
                Total Reposters Checked: $totalChecked
                Cache Updated: ${System.currentTimeMillis()}
                URI: $uri
            """.trimIndent())
            
            isReposted
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Failed to fetch repost status:
                URI: $uri
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            
            // If we have a cached value, use it as fallback
            cached?.let { (status, _) ->
                Log.d(TAG, "⚠️ Using cached status as fallback: $status")
                return status
            }
            
            false
        }
    }

    override fun getUserByDid(did: String): Flow<User?> {
        return userDao.getUserByDid(did)
    }

    override fun getUserByHandle(handle: String): Flow<User?> {
        return userDao.getUserByHandle(handle)
    }

    override suspend fun createPost(text: String, timestamp: String): Result<CreateRecordResponse> {
        return try {
            val did = sessionManager.getDid() ?: throw IllegalStateException("No DID found")
            
            val record = AtProtocolService.PostRecord(
                text = text,
                createdAt = timestamp.ifEmpty { getCurrentTimestamp() }
            )
            
            val request = AtProtocolService.CreateRecordRequest(
                repo = did,
                collection = "app.bsky.feed.post",
                record = record
            )
            
            val response = service.createRecord(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createReply(
        text: String,
        parentUri: String,
        parentCid: String,
        timestamp: String
    ): Result<CreateRecordResponse> {
        return try {
            val did = sessionManager.getDid() ?: throw IllegalStateException("No DID found")
            
            val processedText = text
            
            val record = AtProtocolService.PostRecord(
                text = processedText,
                createdAt = timestamp.ifEmpty { getCurrentTimestamp() },
                reply = AtProtocolService.ReplyReference(
                    parent = AtProtocolService.PostReference(uri = parentUri, cid = parentCid),
                    root = AtProtocolService.PostReference(uri = parentUri, cid = parentCid)
                )
            )
            
            val request = AtProtocolService.CreateRecordRequest(
                repo = did,
                collection = "app.bsky.feed.post",
                record = record
            )
            
            val response = service.createRecord(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): SearchUsersResponse {
        return service.searchUsers(query = query)
    }

    override suspend fun getCurrentSession(): AtProtocolUser? {
        return try {
            Log.d(TAG, "🔍 Checking current session")
            val did = sessionManager.getDid()
            val handle = userDao.getCurrentUserHandle()
            
            if (did != null && handle != null) {
                AtProtocolUser(did = did, handle = handle)
            } else {
                refreshSession().fold(
                    onSuccess = { session ->
                        AtProtocolUser(did = session.did, handle = session.handle)
                    },
                    onFailure = { null }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session check failed: ${e.message}")
            null
        }
    }

    override suspend fun createPost(record: Map<String, Any>): AtProtocolPostResult {
        val did = record["did"] as? String ?: throw IllegalArgumentException("Missing did in record")
        val timestamp = getCurrentTimestamp()
        val postId = timestamp.hashCode().toString()
        
        return AtProtocolPostResult(
            uri = "at://$did/app.bsky.feed.post/$postId",
            cid = "temporary-cid-$postId"
        )
    }

    override suspend fun identityResolveHandle(handle: String): String {
        return try {
            val response = service.identityResolveHandle(handle)
            response.did
        } catch (e: Exception) {
            throw Exception("Failed to resolve handle: ${e.message}")
        }
    }

    private fun generateStableRkey(uri: String): String {
        val postId = uri.substringAfterLast('/')
        return "like_$postId"
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Failed to read image file")
        
        val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }
        
        return tempFile
    }

    private fun getMimeType(uri: Uri): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    override fun clearLikeCache() {
        likeStatusCache.clear()
        repostStatusCache.clear()
    }

    override suspend fun deleteSession(refreshJwt: String): Result<Unit> = runCatching {
        Log.d(TAG, "🔄 Attempting to delete BlueSky session")
        
        if (refreshJwt.isBlank()) {
            Log.w(TAG, "⚠️ No refresh token provided for session deletion")
            return@runCatching
        }
        
        service.deleteSession(refreshJwt)
            .onSuccess {
                Log.d(TAG, "✅ Session deleted successfully on BlueSky servers")
            }
            .onFailure { error ->
                Log.e(TAG, "❌ Failed to delete session: ${error.message}")
                throw error
            }
            .getOrThrow()
    }

    override suspend fun getFollows(actor: String, limit: Int, cursor: String?): Result<FollowsResponse> = withContext(Dispatchers.IO) {
        try {
            // Ensure valid session before making request
            if (!ensureValidSession()) {
                Log.e(TAG, "❌ No valid session available for follows request")
                return@withContext Result.failure(Exception("No valid session available"))
            }
            
            Log.d(TAG, """
                🌐 Follows Request:
                Actor: $actor
                Limit: $limit
                Cursor: $cursor
            """.trimIndent())
            
            val response = service.getFollows(actor, limit, cursor)
            Log.d(TAG, "✅ Follows fetch successful - Found ${response.follows.size} follows")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch follows: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getTrendingHashtags(): List<TrendingHashtag> = withContext(Dispatchers.IO) {
        try {
            // Since BlueSky doesn't have a direct trending hashtags API yet,
            // we'll create a curated list based on popular categories and current trends
            listOf(
                TrendingHashtag("photography", 1500, "Beautiful captures and visual stories", "📸"),
                TrendingHashtag("music", 1200, "Latest tracks and music discussions", "🎵"),
                TrendingHashtag("tech", 1000, "Technology news and discussions", "💻"),
                TrendingHashtag("art", 900, "Digital and traditional artworks", "🎨"),
                TrendingHashtag("gaming", 800, "Gaming highlights and discussions", "🎮"),
                TrendingHashtag("food", 700, "Culinary adventures and recipes", "🍳"),
                TrendingHashtag("nature", 600, "Nature and outdoor experiences", "🌿"),
                TrendingHashtag("fitness", 500, "Health and workout motivation", "💪"),
                TrendingHashtag("books", 400, "Book recommendations and reviews", "📚"),
                TrendingHashtag("travel", 300, "Travel experiences and destinations", "✈️")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get trending hashtags: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getPostsByHashtag(hashtag: String): Result<TimelineResponse> {
        return getPostsByHashtag(hashtag, 50, null)
    }

    override suspend fun getPostsByHashtag(hashtag: String, limit: Int, cursor: String?): Result<TimelineResponse> {
        return try {
            val response = service.getPostsByHashtag(hashtag)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkHashtagFollowStatus(hashtag: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = service.checkHashtagFollowStatus(hashtag)
            response.isFollowing
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun followHashtag(hashtag: String) = withContext(Dispatchers.IO) {
        service.followHashtag(hashtag)
    }

    override suspend fun unfollowHashtag(hashtag: String) = withContext(Dispatchers.IO) {
        service.unfollowHashtag(hashtag)
    }

    override suspend fun searchHandles(query: String): List<UserSearchResult> {
        return try {
            if (query.length < 2) return emptyList()
            
            val response = service.searchUsers(query)
            response.actors.map { actor ->
                UserSearchResult(
                    did = actor.did,
                    handle = actor.handle,
                    displayName = actor.displayName,
                    avatar = actor.avatar
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to search handles: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchHashtags(query: String): List<TrendingHashtag> {
            // TODO: Implement actual hashtag search when AT Protocol adds support
            // For now, return mock data
        return listOf(
                TrendingHashtag("trending", 1000),
                TrendingHashtag("popular", 500),
                TrendingHashtag(query, 100)
            )
    }

    override suspend fun createQuotePost(
        text: String,
        quotedPostUri: String,
        quotedPostCid: String
    ): Result<CreateRecordResponse> {
        return try {
            val embed = AtProtocolService.RecordEmbed(
                type = "app.bsky.embed.record",
                record = AtProtocolService.StrongRef(
                    uri = quotedPostUri,
                    cid = quotedPostCid
                )
            )
            
            val postRecord = AtProtocolService.PostRecord(
                type = "app.bsky.feed.post",
                text = text,
                createdAt = getCurrentTimestamp(),
                embed = embed
            )
            
            val request = AtProtocolService.CreateRecordRequest(
                repo = getCurrentSession()?.did ?: throw IllegalStateException("No active session"),
                collection = "app.bsky.feed.post",
                record = postRecord
            )
            
            val response = service.createRecord(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPost(text: String, timestamp: String, facets: List<Facet>?) {
        try {
            val did = sessionManager.getDid() ?: throw IllegalStateException("No DID found")
            
            val record = AtProtocolService.PostRecord(
                text = text,
                createdAt = timestamp.ifEmpty { getCurrentTimestamp() },
                facets = facets?.map { facet ->
                    AtProtocolService.Facet(
                        index = AtProtocolService.ByteIndex(
                            byteStart = facet.index.byteStart,
                            byteEnd = facet.index.byteEnd
                        ),
                        features = facet.features.map { feature ->
                            when (feature) {
                                is Feature.Mention -> AtProtocolService.Feature.Mention(did = feature.did)
                                is Feature.Link -> AtProtocolService.Feature.Link(uri = feature.uri)
                                is Feature.Tag -> AtProtocolService.Feature.Tag(tag = feature.tag)
                            }
                        }
                    )
                }
            )
            
            val request = AtProtocolService.CreateRecordRequest(
                repo = did,
                collection = "app.bsky.feed.post",
                record = record
            )
            
            service.createRecord(request)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create post: ${e.message}")
            throw e
        }
    }

    override suspend fun parseFacets(text: String): List<Facet>? {
        val facets = mutableListOf<Facet>()
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)

        // Parse mentions using AT Protocol regex
        val mentionRegex = Regex("""(?:^|\s)(@([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)""")
        mentionRegex.findAll(text).forEach { matchResult ->
            val handle = matchResult.groupValues[1].substring(1) // Remove @ symbol
            try {
                val response = service.identityResolveHandle(handle)
                val startIndex = text.substring(0, matchResult.range.first + 1).toByteArray(StandardCharsets.UTF_8).size
                val endIndex = startIndex + matchResult.value.substring(1).toByteArray(StandardCharsets.UTF_8).size
                
                facets.add(Facet(
                    index = ByteIndex(startIndex, endIndex),
                    features = listOf(Feature.Mention(response.did))
                ))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve handle $handle: ${e.message}")
            }
        }

        // Parse URLs with AT Protocol URL regex
        val urlRegex = Regex("""(?:^|\s)(https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*[-a-zA-Z0-9@%_\+~#//=])?)""")
        urlRegex.findAll(text).forEach { matchResult ->
            val url = matchResult.groupValues[1]
            val startIndex = text.substring(0, matchResult.range.first + 1).toByteArray(StandardCharsets.UTF_8).size
            val endIndex = startIndex + url.toByteArray(StandardCharsets.UTF_8).size
            
            facets.add(Facet(
                index = ByteIndex(startIndex, endIndex),
                features = listOf(Feature.Link(url))
            ))
        }

        // Parse hashtags with AT Protocol tag regex
        val hashtagRegex = Regex("""(?:^|\s)(#[^\d\s]\S*)(?=\s|$)""")
        hashtagRegex.findAll(text).forEach { matchResult ->
            val tag = matchResult.groupValues[1].substring(1) // Remove # symbol
            if (tag.length <= 64) { // AT Protocol max length
                val startIndex = text.substring(0, matchResult.range.first + 1).toByteArray(StandardCharsets.UTF_8).size
                val endIndex = startIndex + matchResult.value.substring(1).toByteArray(StandardCharsets.UTF_8).size
                
                facets.add(Facet(
                    index = ByteIndex(startIndex, endIndex),
                    features = listOf(Feature.Tag(tag))
                ))
            }
        }

        return if (facets.isNotEmpty()) facets else null
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    override suspend fun enhancePostWithAI(text: String): AIEnhancement {
        // For now, return a simple enhancement
        // In a real implementation, this would call an AI service
        return AIEnhancement(
            enhancedPost = text,
            hashtags = emptyList()
        )
    }

    private fun VideoModel.toVideo(): Video {
        return Video(
            uri = this.uri,
            did = this.authorDid,
            handle = this.authorHandle,
            videoUrl = this.videoUrl ?: "",
            description = this.description,
            createdAt = Instant.now(), // Convert string to Instant
            indexedAt = Instant.now(),
            sortAt = Instant.now(),
            title = this.title ?: "",
            thumbnailUrl = this.thumbnailUrl ?: "",
            likes = this.likes,
            comments = this.comments,
            shares = this.reposts,
            username = this.authorName ?: "",
            userId = this.authorDid,
            isImage = false,
            imageUrl = "",
            aspectRatio = this.aspectRatio ?: 1.0f,
            authorAvatar = this.authorAvatar ?: ""
        )
    }

    override suspend fun getMediaPosts(): List<Video> {
        try {
            Log.d(TAG, "🎬 Starting media posts fetch")
            
            val timeline = getTimeline(algorithm = "whats-hot", limit = 100).getOrThrow()
            Log.d(TAG, "📥 Retrieved ${timeline.feed.size} total posts from timeline")
            
            val mediaVideos = timeline.feed
                .filter { feedPost ->
                    val hasEmbed = feedPost.post.embed != null
                    val isMediaPost = feedPost.post.embed?.let { embed ->
                        embed.type?.startsWith("app.bsky.embed.images") == true ||
                        embed.type?.startsWith("app.bsky.embed.external") == true ||
                        embed.type?.startsWith("app.bsky.embed.video") == true
                    } ?: false
                    
                    Log.d(TAG, """
                        🔍 Checking post for media:
                        URI: ${feedPost.post.uri}
                        Has embed: $hasEmbed
                        Embed type: ${feedPost.post.embed?.type}
                        Is media post: $isMediaPost
                    """.trimIndent())
                    
                    hasEmbed && isMediaPost
                }
                .mapNotNull { feedPost ->
                    mapPostToVideo(feedPost.post)
                }
                .filter { video ->
                    val hasMedia = video.isImage || video.videoUrl.isNotBlank()
                    Log.d(TAG, """
                        ✨ Validating mapped video:
                        URI: ${video.uri}
                        Is image: ${video.isImage}
                        Has video URL: ${video.videoUrl.isNotBlank()}
                        Has media: $hasMedia
                    """.trimIndent())
                    hasMedia
                }
            
            Log.d(TAG, """
                📊 Media posts summary:
                Total posts processed: ${timeline.feed.size}
                Media posts found: ${mediaVideos.size}
                Images: ${mediaVideos.count { it.isImage }}
                Videos: ${mediaVideos.count { !it.isImage }}
            """.trimIndent())
            
            return mediaVideos
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching media posts: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    private fun mapPostToVideo(post: Post): Video? {
        try {
            // Enhanced oEmbed URL handling
            fun getOEmbedUrl(url: String, platform: String? = null): String {
                return when {
                    url.contains("bsky.app/profile") -> {
                        val encodedUrl = Uri.encode(url)
                        "https://embed.bsky.app/oembed?url=$encodedUrl&format=json"
                    }
                    platform == "twitter" || url.contains("twitter.com") || url.contains("x.com") -> {
                        val encodedUrl = Uri.encode(url)
                        "https://publish.twitter.com/oembed?url=$encodedUrl"
                    }
                    platform == "instagram" || url.contains("instagram.com") -> {
                        val encodedUrl = Uri.encode(url)
                        "https://api.instagram.com/oembed?url=$encodedUrl"
                    }
                    platform == "youtube" || url.contains("youtube.com") || url.contains("youtu.be") -> {
                        val encodedUrl = Uri.encode(url)
                        "https://www.youtube.com/oembed?url=$encodedUrl&format=json"
                    }
                    platform == "tiktok" || url.contains("tiktok.com") -> {
                        val encodedUrl = Uri.encode(url)
                        "https://www.tiktok.com/oembed?url=$encodedUrl"
                    }
                    else -> url
                }
            }

            // Add detailed embed type logging
            Log.d(TAG, """
                🎥 Detailed Embed Analysis:
                URI: ${post.uri}
                Embed Type: ${post.embed?.type}
                Record Type: ${post.record.type}
                Has Video Embed: ${post.embed?.video != null}
                Is Repost: ${post.record.type == "app.bsky.feed.repost"}
                Video Details: ${post.embed?.video?.let { video ->
                    """
                    |  - Ref Link: ${video.ref?.link}
                    |  - Aspect Ratio: ${video.aspectRatio?.width}:${video.aspectRatio?.height}
                    """.trimMargin()
                } ?: "No video details"}
                External Details: ${post.embed?.external?.let { external ->
                    """
                    |  - URI: ${external.uri}
                    |  - Title: ${external.title}
                    |  - Description: ${external.description}
                    |  - Thumb: ${external.thumb?.link}
                    |  - Social Info: ${external.getSocialMediaInfo()?.let { "${it.platform} ${it.type}" }}
                    |  - OEmbed URL: ${external.uri?.let { getOEmbedUrl(it, external.getSocialMediaInfo()?.platform?.lowercase()) }}
                    """.trimMargin()
                } ?: "No external details"}
            """.trimIndent())

            // Enhanced embed validation
            val embed = post.embed
            if (embed == null) {
                Log.d(TAG, "⚠️ Skipping post - no embed found")
                return null
            }

            // Enhanced video URL handling with oEmbed support
            val videoUrl = when {
                // Direct Bluesky video
                embed.video?.ref?.link != null -> {
                    val link = embed.video.ref.link
                    val url = "https://cdn.bsky.app/video/plain/$link"
                    Log.d(TAG, "🎥 Using Bluesky CDN video URL: $url")
                    url
                }
                // External video with oEmbed support
                embed.external?.uri?.let { uri ->
                    val socialInfo = embed.external.getSocialMediaInfo()
                    val isVideo = uri.endsWith(".mp4", ignoreCase = true) ||
                        uri.endsWith(".mov", ignoreCase = true) ||
                        uri.endsWith(".webm", ignoreCase = true) ||
                        uri.contains("video", ignoreCase = true) ||
                        uri.contains("cdn.bsky.social/video", ignoreCase = true) ||
                        socialInfo?.type == "video"
                    if (isVideo) getOEmbedUrl(uri, socialInfo?.platform?.lowercase()) else null
                } != null -> {
                    val uri = embed.external.uri
                    val oEmbedUrl = getOEmbedUrl(uri, embed.external.getSocialMediaInfo()?.platform?.lowercase())
                    Log.d(TAG, "🎥 Using oEmbed video URL: $oEmbedUrl")
                    oEmbedUrl
                }
                else -> {
                    Log.d(TAG, "⚠️ No valid video URL found in embed")
                    ""
                }
            }

            // Enhanced thumbnail handling
            val thumbnailUrl = when {
                embed.external?.thumb?.link != null -> {
                    val link = embed.external.thumb.link
                    if (link.startsWith("http")) {
                        link
                    } else {
                        "https://cdn.bsky.app/img/feed_thumbnail/plain/$link@jpeg"
                    }
                }
                embed.external?.uri != null -> {
                    val socialInfo = embed.external.getSocialMediaInfo()
                    if (socialInfo != null) {
                        getOEmbedUrl(embed.external.uri, socialInfo.platform.lowercase())
                    } else {
                        embed.external.uri
                    }
                }
                else -> ""
            }

            // Return enhanced Video object
            return Video(
                uri = post.uri,
                did = post.author.did,
                handle = post.author.handle,
                videoUrl = videoUrl,
                description = post.record.text,
                createdAt = Instant.parse(post.record.createdAt),
                indexedAt = Instant.parse(post.indexedAt),
                sortAt = Instant.parse(post.indexedAt),
                title = embed.external?.title ?: "",
                thumbnailUrl = thumbnailUrl,
                likes = post.likeCount ?: 0,
                comments = post.replyCount ?: 0,
                shares = post.repostCount ?: 0,
                username = post.author.displayName ?: post.author.handle,
                userId = post.author.did,
                isImage = embed.type?.startsWith("app.bsky.embed.images") == true,
                imageUrl = if (embed.type?.startsWith("app.bsky.embed.images") == true) {
                    embed.images?.firstOrNull()?.fullsize ?: ""
                } else "",
                aspectRatio = embed.video?.aspectRatio?.let { it.width.toFloat() / it.height.toFloat() } ?: 1.0f,
                authorAvatar = post.author.avatar ?: "",
                caption = post.record.text,
                facets = post.record.facets
            ).also { video ->
                Log.d(TAG, """
                    ✅ Enhanced Media Result:
                    URI: ${video.uri}
                    Is Image: ${video.isImage}
                    Image URL: ${video.imageUrl}
                    Video URL: ${video.videoUrl}
                    Has Media: ${video.isImage || video.videoUrl.isNotBlank()}
                    Thumbnail: ${video.thumbnailUrl}
                    Social Platform: ${embed.external?.getSocialMediaInfo()?.platform}
                """.trimIndent())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error mapping post to video: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            return null
        }
    }

    private fun mapToPost(post: Post): Post {
        return Post(
            uri = post.uri,
            cid = post.cid,
            author = post.author,
            record = post.record,
            embed = post.embed,
            indexedAt = post.indexedAt,
            likeCount = post.likeCount,
            replyCount = post.replyCount,
            repostCount = post.repostCount,
            viewer = post.viewer
        )
    }

    // Add this helper function to get repost information
    private suspend fun getRepostInfo(uri: String): RepostInfo? {
        return try {
            val response = service.getReposts(uri, limit = 1)
            if (response.repostedBy.isNotEmpty()) {
                val reposter = response.repostedBy.first()
                RepostInfo(
                    repostedBy = AtProfile(
                        did = reposter.did,
                        handle = reposter.handle,
                        displayName = reposter.displayName,
                        avatar = reposter.avatar,
                        viewer = reposter.viewer,
                        description = reposter.description,
                        indexedAt = reposter.indexedAt
                    ),
                    timestamp = response.cursor ?: getCurrentTimestamp()
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get repost info: ${e.message}")
            null
        }
    }

    data class RepostInfo(
        val repostedBy: AtProfile,  // Using AtProfile from the API package
        val timestamp: String
    )

    companion object {
        private const val TAG = "AtProtocolRepo"
    }
} 