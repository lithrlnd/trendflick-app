package com.trendflick.ui.screens.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.api.FeedPost
import com.trendflick.data.repository.AtProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.trendflick.data.auth.BlueskyCredentialsManager
import com.trendflick.data.api.VideoModel
import java.time.Instant
import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val atProtocolRepository: AtProtocolRepository,
    private val credentialsManager: BlueskyCredentialsManager
) : ViewModel() {
    private val _threads = MutableStateFlow<List<FeedPost>>(emptyList())
    val threads: StateFlow<List<FeedPost>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _likedPosts = MutableStateFlow<Set<String>>(emptySet())
    val likedPosts: StateFlow<Set<String>> = _likedPosts.asStateFlow()

    private val _repostedPosts = MutableStateFlow<Set<String>>(emptySet())
    val repostedPosts: StateFlow<Set<String>> = _repostedPosts.asStateFlow()

    // Video-related state
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    private val _videoLoadError = MutableStateFlow<String?>(null)
    val videoLoadError: StateFlow<String?> = _videoLoadError.asStateFlow()

    private var currentCursor: String? = null
    private var videoCursor: String? = null
    private var loadingJob: Job? = null
    private var isLoggedOut = false
    private val _selectedFeed = MutableStateFlow("Following")
    val selectedFeed: StateFlow<String> = _selectedFeed.asStateFlow()

    // Add ShareEvent Flow
    private val _shareEvent = MutableSharedFlow<android.content.Intent>()
    val shareEvent = _shareEvent.asSharedFlow()

    init {
        Log.d(TAG, "🚀 ViewModel initialized")
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚀 Starting FollowingViewModel initialization")
                
                // Reset loading states at initialization
                _isLoading.value = false
                _isRefreshing.value = false
                
                // Initialize with Following feed type
                _selectedFeed.value = "Following"
                
                // Step 1: Check credentials and create session
                if (ensureValidSession()) {
                    Log.d(TAG, "✅ Session validated")
                    // Initial load will be triggered by LaunchedEffect in the UI
                } else {
                    Log.e(TAG, "❌ Session validation failed")
                    isLoggedOut = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ FollowingViewModel initialization failed: ${e.message}")
                isLoggedOut = true
            }
        }
    }

    fun updateSelectedFeed(feed: String) {
        loadingJob?.let { existingJob ->
            if (existingJob.isActive) {
                Log.d(TAG, "⚠️ Cancelling existing video feed job")
                existingJob.cancel()
                viewModelScope.launch {
                    try {
                        existingJob.join()
                    } catch (e: Exception) {
                        Log.d(TAG, "⚠️ Job cancellation completed")
                    }
                }
            }
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Feed update requested: $feed")
                
                // Reset states
                _isLoading.value = false
                _isRefreshing.value = false
                _isLoadingVideos.value = false
                _videoLoadError.value = null
                
                // Update selected feed
                _selectedFeed.value = feed
                Log.d(TAG, "📱 Selected feed updated to: $feed")
                
                // Ensure we have a valid session before proceeding
                if (!ensureValidSession()) {
                    Log.e(TAG, "❌ Feed update failed: No valid session")
                    return@launch
                }
                
                // Clear appropriate list and load new content
                when (feed) {
                    "Flicks" -> {
                        clearVideos()
                        delay(100)
                        refreshVideoFeed()
                    }
                    else -> {
                        clearThreads()
                        delay(100)
                        loadThreads(refresh = true)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "⚠️ Feed update cancelled")
                    return@launch
                }
                Log.e(TAG, "❌ Feed update failed: ${e.message}")
            }
        }
    }

    fun clearThreads() {
        _threads.value = emptyList()
        currentCursor = null
        Log.d(TAG, "🧹 Threads cleared")
    }

    fun clearVideos() {
        _videos.value = emptyList()
        videoCursor = null
        Log.d(TAG, "🧹 Videos cleared")
    }

    fun refreshVideoFeed() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🎥 Starting video feed refresh")
                _isLoadingVideos.value = true
                _videoLoadError.value = null
                
                // Get timeline with reverse-chronological order for following feed
                val result = atProtocolRepository.getTimeline(
                    algorithm = "reverse-chronological",
                    limit = 100  // Increased limit to find more media posts
                )
                
                result.onSuccess { response ->
                    Log.d(TAG, "✅ Timeline fetched, processing ${response.feed.size} posts for media")
                    
                    // Filter and transform posts with media content
                    val mediaResult = response.feed
                        .filter { feedPost -> 
                            // Skip posts with replies to avoid reply structure issues
                            feedPost.reply == null
                        }
                        .mapNotNull { feedPost ->
                            try {
                                // Basic validation
                                if (feedPost.post.uri.isEmpty() || feedPost.post.cid.isEmpty()) {
                                    Log.d(TAG, "⚠️ Skipping post with missing URI/CID: ${feedPost.post.uri}")
                                    return@mapNotNull null
                                }

                                // Check for media content
                                val hasMediaContent = feedPost.post.embed?.let { embed ->
                                    (embed.video != null) || 
                                    (embed.images?.isNotEmpty() == true) ||
                                    (embed.external?.let { ext ->
                                        (ext.thumb != null) ||
                                        (ext.uri?.let { uri ->
                                            uri.contains("video", ignoreCase = true) ||
                                            uri.contains("youtube.com") ||
                                            uri.contains("vimeo.com")
                                        } == true)
                                    } == true)
                                } ?: false

                                if (!hasMediaContent) {
                                    return@mapNotNull null
                                }

                                // Create Video object
                                Video(
                                    uri = feedPost.post.uri,
                                    did = feedPost.post.author.did,
                                    handle = feedPost.post.author.handle,
                                    videoUrl = feedPost.post.embed?.video?.ref?.link?.let { 
                                        "https://cdn.bsky.app/video/plain/$it" 
                                    } ?: feedPost.post.embed?.external?.uri ?: "",
                                    description = feedPost.post.record.text,
                                    createdAt = Instant.parse(feedPost.post.record.createdAt),
                                    indexedAt = Instant.parse(feedPost.post.indexedAt),
                                    sortAt = Instant.parse(feedPost.post.indexedAt),
                                    title = feedPost.post.record.text,
                                    thumbnailUrl = feedPost.post.embed?.images?.firstOrNull()?.thumb ?: 
                                                 feedPost.post.embed?.external?.thumb?.link?.let {
                                                     "https://cdn.bsky.app/img/feed_thumbnail/plain/$it@jpeg"
                                                 } ?: "",
                                    likes = feedPost.post.likeCount ?: 0,
                                    comments = feedPost.post.replyCount ?: 0,
                                    shares = feedPost.post.repostCount ?: 0,
                                    username = feedPost.post.author.displayName ?: feedPost.post.author.handle,
                                    userId = feedPost.post.author.did,
                                    isImage = feedPost.post.embed?.video == null,
                                    imageUrl = feedPost.post.embed?.images?.firstOrNull()?.image?.link?.let { link ->
                                        "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                                    } ?: "",
                                    aspectRatio = feedPost.post.embed?.video?.aspectRatio?.let { 
                                        it.width.toFloat() / it.height.toFloat() 
                                    } ?: 16f/9f,
                                    authorAvatar = feedPost.post.author.avatar ?: "",
                                    authorName = feedPost.post.author.displayName ?: feedPost.post.author.handle,
                                    facets = feedPost.post.record.facets,
                                    caption = feedPost.post.record.text
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error processing post for media: ${e.message}")
                                null
                            }
                        }

                    Log.d(TAG, """
                        ✅ Video feed refresh result:
                        • Total posts processed: ${response.feed.size}
                        • Posts with replies skipped: ${response.feed.count { it.reply != null }}
                        • Media posts found: ${mediaResult.size}
                        • Images: ${mediaResult.count { it.isImage }}
                        • Videos: ${mediaResult.count { !it.isImage }}
                    """.trimIndent())
                    
                    _videos.value = mediaResult
                    _isLoadingVideos.value = false
                    
                }.onFailure { error ->
                    Log.e(TAG, """
                        ❌ Failed to refresh video feed:
                        • Error: ${error.message}
                        • Type: ${error.javaClass.name}
                        • Stack: ${error.stackTraceToString()}
                    """.trimIndent())
                    _videoLoadError.value = error.message ?: "Failed to load media"
                    _isLoadingVideos.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ Error in refreshVideoFeed:
                    • Message: ${e.message}
                    • Type: ${e.javaClass.name}
                    • Stack: ${e.stackTraceToString()}
                """.trimIndent())
                _videoLoadError.value = e.message ?: "Failed to load media"
                _isLoadingVideos.value = false
            }
        }
    }

    private suspend fun ensureValidSession(): Boolean {
        return try {
            // First check if we're already logged in
            if (!isLoggedOut) {
                val existingSession = atProtocolRepository.getCurrentSession()
                if (existingSession != null) {
                    Log.d(TAG, "✅ Using existing valid session for handle: ${existingSession.handle}")
                    return true
                }
            }

            // Get credentials
            val handle = credentialsManager.getHandle()
            val password = credentialsManager.getPassword()
            
            if (handle.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.e(TAG, "❌ Missing credentials")
                isLoggedOut = true
                return false
            }

            // Add delay to avoid race conditions
            delay(100)
            
            // Double-check session again after delay
            val currentSession = atProtocolRepository.getCurrentSession()
            if (currentSession != null) {
                Log.d(TAG, "✅ Found existing valid session for handle: ${currentSession.handle}")
                isLoggedOut = false
                return true
            }
            
            Log.d(TAG, "🔍 No valid session found, attempting to create new session for handle: $handle")
            
            // Try to create session with retries
            var attempts = 0
            var lastError: Exception? = null
            
            while (attempts < 3) {
                try {
                    val result = atProtocolRepository.createSession(handle, password)
                    if (result.isSuccess) {
                        Log.d(TAG, "✅ Successfully created new session for handle: $handle")
                        isLoggedOut = false
                        return true
                    } else {
                        Log.w(TAG, "⚠️ Session creation attempt ${attempts + 1} failed")
                        delay(500) // Add delay between retries
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Session creation attempt ${attempts + 1} failed: ${e.message}")
                    lastError = e
                    delay(500) // Add delay between retries
                }
                attempts++
            }

            // Final check after all retries
            val finalCheck = atProtocolRepository.getCurrentSession()
            if (finalCheck != null) {
                Log.d(TAG, "✅ Found valid session after retries for handle: ${finalCheck.handle}")
                isLoggedOut = false
                return true
            }

            // If we get here, all attempts failed
            Log.e(TAG, "❌ All session creation attempts failed. Last error: ${lastError?.message}")
            isLoggedOut = true
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session validation failed", e)
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            isLoggedOut = true
            return false
        }
    }

    private fun loadThreads(refresh: Boolean = false) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Loading threads (refresh: $refresh)")
                _isLoading.value = true
                
                if (!credentialsManager.hasValidCredentials()) {
                    Log.e(TAG, "❌ No valid credentials found")
                    _isLoading.value = false
                    return@launch
                }

                ensureValidSession()
                
                if (isLoggedOut) {
                    Log.e(TAG, "❌ User is logged out")
                    _isLoading.value = false
                    return@launch
                }
                
                if (refresh) {
                    currentCursor = null
                    _threads.value = emptyList()
                }
                
                val algorithm = "reverse-chronological"
                val limit = 30 // Reduced limit to handle data more safely
                
                Log.d(TAG, """
                    📱 Feed Parameters:
                    • Algorithm: $algorithm (Following feed)
                    • Limit: $limit
                    • Cursor: $currentCursor
                    • Screen: Following
                """.trimIndent())
                
                val result = atProtocolRepository.getTimeline(
                    algorithm = algorithm,
                    limit = limit,
                    cursor = if (refresh) null else currentCursor
                )
                
                result.onSuccess { response ->
                    Log.d(TAG, "✅ Timeline fetch successful with ${response.feed.size} posts")

                    // Process posts with safe fallbacks for reply structures
                    val filteredPosts = response.feed.mapNotNull { post ->
                        try {
                            // Basic validation
                            if (post.post.uri.isEmpty() || post.post.cid.isEmpty()) {
                                Log.w(TAG, "⚠️ Skipping post with missing URI/CID")
                                return@mapNotNull null
                            }

                            // Handle reply structures safely
                            val safePost = if (post.reply != null) {
                                try {
                                    // Validate reply structure
                                    val rootRef = post.reply.root
                                    val parentRef = post.reply.parent
                                    
                                    // Check if we have valid references
                                    if (rootRef == null || parentRef == null ||
                                        rootRef.cid.isNullOrEmpty() || rootRef.uri.isNullOrEmpty() ||
                                        parentRef.cid.isNullOrEmpty() || parentRef.uri.isNullOrEmpty()) {
                                        
                                        // Create a new post without the reply structure
                                        Log.d(TAG, "⚠️ Removing invalid reply structure from post: ${post.post.uri}")
                                        post.copy(reply = null)
                                    } else {
                                        post
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "⚠️ Error processing reply structure: ${e.message}")
                                    post.copy(reply = null)
                                }
                            } else {
                                post
                            }

                            // Validate timestamps
                            val createdAt = try {
                                Instant.parse(safePost.post.record.createdAt)
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Invalid createdAt, using current time")
                                Instant.now()
                            }

                            val indexedAt = try {
                                Instant.parse(safePost.post.indexedAt)
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Invalid indexedAt, using current time")
                                Instant.now()
                            }

                            // Apply sortAt logic with safe fallbacks
                            val sortAt = when {
                                createdAt.isBefore(Instant.EPOCH) -> indexedAt
                                createdAt.isAfter(indexedAt.plusSeconds(300)) -> indexedAt
                                else -> createdAt
                            }

                            // Skip empty posts
                            if (safePost.post.record.text.isBlank()) {
                                Log.w(TAG, "⚠️ Skipping empty post")
                                return@mapNotNull null
                            }

                            safePost
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing post: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, """
                        📊 Feed Processing Results:
                        • Original posts: ${response.feed.size}
                        • Valid posts: ${filteredPosts.size}
                        • Posts removed: ${response.feed.size - filteredPosts.size}
                        • Posts with replies: ${filteredPosts.count { it.reply != null }}
                    """.trimIndent())

                    _threads.value = if (refresh) {
                        filteredPosts
                    } else {
                        _threads.value + filteredPosts
                    }
                    
                    loadInitialLikeStates(filteredPosts)
                    currentCursor = response.cursor
                    
                }.onFailure { error ->
                    Log.e(TAG, """
                        ❌ Failed to load threads:
                        • Error: ${error.message}
                        • Type: ${error.javaClass.name}
                        • Stack: ${error.stackTraceToString()}
                    """.trimIndent())
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in loadThreads: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreThreads(refresh: Boolean = false) {
        loadThreads(refresh = refresh)
    }

    fun refreshThreads() {
        loadThreads(refresh = true)
    }

    fun toggleLike(postUri: String) {
        viewModelScope.launch {
            try {
                val currentLikes = _likedPosts.value.toMutableSet()
                val likeResult = atProtocolRepository.likePost(postUri)
                
                _likedPosts.value = if (likeResult) {
                    currentLikes + postUri
                } else {
                    currentLikes - postUri
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle like: ${e.message}")
            }
        }
    }

    fun repost(uri: String) {
        viewModelScope.launch {
            try {
                // First get the post details to get the correct CID
                val postThread = atProtocolRepository.getPostThread(uri)
                postThread.onSuccess { threadResponse ->
                    val post = threadResponse.thread.post
                    // Now create the repost with the correct CID
                    atProtocolRepository.repost(post.uri, post.cid)
                    // Toggle repost state
                    _repostedPosts.value = if (uri in _repostedPosts.value) {
                        _repostedPosts.value - uri
                    } else {
                        _repostedPosts.value + uri
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to get post details for repost: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to repost: ${e.message}")
            }
        }
    }

    private fun loadInitialLikeStates(posts: List<FeedPost>) {
        viewModelScope.launch {
            try {
                println("DEBUG: TrendFlick 💾 [FOLLOWING] Loading initial like and repost states for ${posts.size} posts")
                
                // Keep existing likes and reposts to prevent UI flicker
                val currentLiked = _likedPosts.value.toMutableSet()
                val currentReposted = _repostedPosts.value.toMutableSet()
                println("DEBUG: TrendFlick 💾 [FOLLOWING] Current likes in memory before loading: ${currentLiked.size}")
                println("DEBUG: TrendFlick 💾 [FOLLOWING] Current reposts in memory before loading: ${currentReposted.size}")
                
                // Check each post's like and repost status
                posts.forEach { feedPost ->
                    try {
                        println("DEBUG: TrendFlick 🔍 [FOLLOWING] Checking like and repost status for post: ${feedPost.post.uri}")
                        if (atProtocolRepository.isPostLikedByUser(feedPost.post.uri)) {
                            currentLiked.add(feedPost.post.uri)
                            println("DEBUG: TrendFlick ✅ [FOLLOWING] Post ${feedPost.post.uri} is liked")
                        }
                        if (atProtocolRepository.isPostRepostedByUser(feedPost.post.uri)) {
                            currentReposted.add(feedPost.post.uri)
                            println("DEBUG: TrendFlick ✅ [FOLLOWING] Post ${feedPost.post.uri} is reposted")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: TrendFlick ❌ [FOLLOWING] Failed to check like/repost status for post ${feedPost.post.uri}: ${e.message}")
                    }
                }
                
                // Update the states
                _likedPosts.value = currentLiked
                _repostedPosts.value = currentReposted
                println("DEBUG: TrendFlick 💾 [FOLLOWING] Updated liked posts set, total liked: ${currentLiked.size}")
                println("DEBUG: TrendFlick 💾 [FOLLOWING] Updated reposted posts set, total reposted: ${currentReposted.size}")
            } catch (e: Exception) {
                println("DEBUG: TrendFlick ❌ [FOLLOWING] Failed to load initial like/repost states: ${e.message}")
                println("DEBUG: TrendFlick ❌ [FOLLOWING] Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    fun getVideoFeed(): List<Video> {
        // Convert feed posts with video/image embeds to Video objects
        return _threads.value
            .mapNotNull { feedPost ->
                try {
                    // Step 1: Basic validation
                    if (feedPost.post.uri.isEmpty() || feedPost.post.cid.isEmpty()) {
                        return@mapNotNull null
                    }

                    // Step 2: Handle reply structures safely
                    val safePost = if (feedPost.reply != null) {
                        try {
                            // Validate reply structure
                            val rootRef = feedPost.reply.root
                            val parentRef = feedPost.reply.parent
                            
                            // Check if we have valid references
                            if (rootRef == null || parentRef == null ||
                                rootRef.cid.isNullOrEmpty() || rootRef.uri.isNullOrEmpty() ||
                                parentRef.cid.isNullOrEmpty() || parentRef.uri.isNullOrEmpty()) {
                                
                                // Create a new post without the reply structure
                                feedPost.copy(reply = null)
                            } else {
                                feedPost
                            }
                        } catch (e: Exception) {
                            feedPost.copy(reply = null)
                        }
                    } else {
                        feedPost
                    }

                    // Step 3: Check for media content
                    val hasMediaContent = safePost.post.embed?.let { embed ->
                        (embed.video != null) || 
                        (embed.images?.isNotEmpty() == true) ||
                        (embed.external?.let { ext ->
                            (ext.thumb != null) ||
                            (ext.uri?.let { uri ->
                                uri.contains("video", ignoreCase = true) ||
                                uri.contains("youtube.com") ||
                                uri.contains("vimeo.com")
                            } == true)
                        } == true)
                    } ?: false

                    if (!hasMediaContent) {
                        return@mapNotNull null
                    }

                    // Step 4: Create Video object
                    Video(
                        uri = safePost.post.uri,
                        did = safePost.post.author.did,
                        handle = safePost.post.author.handle,
                        videoUrl = safePost.post.embed?.video?.ref?.link?.let { 
                            "https://cdn.bsky.app/video/plain/$it" 
                        } ?: safePost.post.embed?.external?.uri ?: "",
                        description = safePost.post.record.text,
                        createdAt = Instant.parse(safePost.post.record.createdAt),
                        indexedAt = Instant.now(),
                        sortAt = Instant.now(),
                        title = safePost.post.record.text,
                        thumbnailUrl = safePost.post.embed?.images?.firstOrNull()?.thumb ?: 
                                     safePost.post.embed?.external?.thumb?.link?.let {
                                         "https://cdn.bsky.app/img/feed_thumbnail/plain/$it@jpeg"
                                     } ?: "",
                        likes = safePost.post.likeCount ?: 0,
                        comments = safePost.post.replyCount ?: 0,
                        shares = safePost.post.repostCount ?: 0,
                        username = safePost.post.author.displayName ?: safePost.post.author.handle,
                        userId = safePost.post.author.did,
                        isImage = safePost.post.embed?.video == null,
                        imageUrl = safePost.post.embed?.images?.firstOrNull()?.image?.link?.let { link ->
                            "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                        } ?: "",
                        aspectRatio = safePost.post.embed?.video?.aspectRatio?.let { 
                            it.width.toFloat() / it.height.toFloat() 
                        } ?: 16f/9f,
                        authorAvatar = safePost.post.author.avatar ?: "",
                        authorName = safePost.post.author.displayName ?: safePost.post.author.handle,
                        facets = safePost.post.record.facets,
                        caption = safePost.post.record.text
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    fun sharePost(uri: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔗 Preparing to share post: $uri")
                
                // Get the post details
                val postThread = atProtocolRepository.getPostThread(uri)
                postThread.onSuccess { threadResponse ->
                    val post = threadResponse.thread.post
                    
                    // Create share text
                    val shareText = buildString {
                        append("${post.author.displayName ?: post.author.handle}: ")
                        append(post.record.text)
                        append("\n\n")
                        append("https://bsky.app/profile/${post.author.handle}/post/${uri.split("/").lastOrNull()}")
                    }
                    
                    // Create share intent
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    
                    // Emit the share intent
                    _shareEvent.emit(sendIntent)
                    
                    Log.d(TAG, "✅ Share intent created for post: $uri")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to get post details for sharing: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to share post: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "FollowingViewModel"
    }
} 
