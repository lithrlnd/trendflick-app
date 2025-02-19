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
    private val _selectedFeed = MutableStateFlow("Trends")
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
        viewModelScope.launch {
            if (_isLoading.value) {
                Log.d(TAG, "⚠️ Feed update requested while loading, cancelling current load")
                loadingJob?.cancel()
                _isLoading.value = false
                _isRefreshing.value = false
            }

            _selectedFeed.value = feed
            Log.d(TAG, "🔄 Feed updated to: $feed")
            
            delay(100) // Brief delay to ensure state updates
            
            if (feed == "Flicks") {
                refreshVideoFeed()
            } else {
                loadThreads(refresh = true)
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
                Log.d(TAG, "🎥 Refreshing video feed")
                _isLoadingVideos.value = true
                _videoLoadError.value = null
                
                if (!ensureValidSession()) {
                    Log.e(TAG, "❌ Session validation failed, aborting video refresh")
                    _videoLoadError.value = "Session validation failed"
                    return@launch
                }

                val result = atProtocolRepository.getTimeline(
                    algorithm = when (_selectedFeed.value) {
                        "Trends" -> "whats-hot"
                        else -> "reverse-chronological"
                    },
                    cursor = null,
                    limit = 50
                )

                result.onSuccess { response ->
                    Log.d(TAG, """
                        ✅ Raw feed fetch successful:
                        • Feed size: ${response.feed.size}
                        • Algorithm: ${if (_selectedFeed.value == "Trends") "whats-hot" else "reverse-chronological"}
                    """.trimIndent())

                    val videoList = response.feed
                        // First filter for valid posts
                        .filter { feedPost ->
                            feedPost.post.uri.isNotEmpty() && 
                            feedPost.post.cid.isNotEmpty()
                        }
                        // Then filter for video content
                        .filter { feedPost ->
                            feedPost.post.embed?.video != null || 
                            feedPost.post.embed?.external?.uri?.contains("video", ignoreCase = true) == true
                        }
                        .map { feedPost ->
                            val now = Instant.now()
                            val createdAt = try {
                                Instant.parse(feedPost.post.record.createdAt)
                            } catch (e: Exception) {
                                now
                            }
                            
                            Video(
                                uri = feedPost.post.uri,
                                did = feedPost.post.author.did,
                                handle = feedPost.post.author.handle,
                                videoUrl = feedPost.post.embed?.video?.ref?.link?.let { link ->
                                    "https://cdn.bsky.app/video/plain/$link"
                                } ?: feedPost.post.embed?.external?.uri ?: "",
                                description = feedPost.post.record.text,
                                createdAt = createdAt,
                                indexedAt = now,
                                sortAt = now,
                                title = feedPost.post.record.text,
                                thumbnailUrl = feedPost.post.embed?.video?.ref?.link?.let { link ->
                                    "https://cdn.bsky.app/video/thumb/$link"
                                } ?: feedPost.post.embed?.external?.thumb?.link ?: "",
                                likes = feedPost.post.likeCount ?: 0,
                                comments = feedPost.post.replyCount ?: 0,
                                shares = feedPost.post.repostCount ?: 0,
                                username = feedPost.post.author.displayName ?: feedPost.post.author.handle,
                                userId = feedPost.post.author.did,
                                isImage = false,
                                imageUrl = "",
                                aspectRatio = feedPost.post.embed?.video?.aspectRatio?.let { ratio ->
                                    ratio.width.toFloat() / ratio.height.toFloat()
                                } ?: 16f/9f,
                                authorAvatar = feedPost.post.author.avatar ?: "",
                                authorName = feedPost.post.author.displayName ?: feedPost.post.author.handle,
                                caption = feedPost.post.record.text,
                                facets = feedPost.post.record.facets
                            )
                        }

                    Log.d(TAG, """
                        ✅ Video feed refresh successful:
                        • Total videos: ${videoList.size}
                        • First video URI: ${videoList.firstOrNull()?.uri}
                        • First video URL: ${videoList.firstOrNull()?.videoUrl}
                    """.trimIndent())

                    _videos.value = videoList
                }.onFailure { error ->
                    Log.e(TAG, "❌ Video feed refresh failed: ${error.message}")
                    _videoLoadError.value = error.message ?: "Failed to load videos"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Video feed refresh failed with exception: ${e.message}")
                _videoLoadError.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoadingVideos.value = false
            }
        }
    }

    private suspend fun ensureValidSession(): Boolean {
        return try {
            val handle = credentialsManager.getHandle()
            val password = credentialsManager.getPassword()
            
            if (handle.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.e(TAG, "❌ Missing credentials")
                isLoggedOut = true
                return false
            }
            
            // Check if we have a valid session
            val currentSession = atProtocolRepository.getCurrentSession()
            if (currentSession != null) {
                Log.d(TAG, "✅ Found existing valid session for handle: ${currentSession.handle}")
                isLoggedOut = false
                return true
            }
            
            // If no valid session, try to create one with proper coroutine context
            Log.d(TAG, "🔍 No valid session found, attempting to create new session")
            
            try {
                val result = atProtocolRepository.createSession(handle, password)
                return result.isSuccess.also { success ->
                    if (success) {
                        Log.d(TAG, "✅ Successfully created new session for handle: $handle")
                        isLoggedOut = false
                    } else {
                        Log.e(TAG, "❌ Failed to create session")
                        isLoggedOut = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Session creation failed: ${e.message}")
                isLoggedOut = true
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session validation failed", e)
            isLoggedOut = true
            return false
        }
    }

    private fun loadThreads(refresh: Boolean = false) {
        // Cancel any existing load job
        loadingJob?.cancel()
        
        loadingJob = viewModelScope.launch {
            try {
                if (refresh) {
                    _isRefreshing.value = true
                    currentCursor = null
                    _threads.value = emptyList()
                }
                _isLoading.value = true

                Log.d(TAG, """
                    🚀 Starting timeline load:
                    • Feed type: ${_selectedFeed.value}
                    • Algorithm: ${if (_selectedFeed.value == "Trends") "whats-hot" else "reverse-chronological"}
                    • Refresh mode: $refresh
                    • Current cursor: ${currentCursor ?: "null"}
                """.trimIndent())

                // Ensure valid session before proceeding
                if (!ensureValidSession()) {
                    Log.e(TAG, "❌ Session validation failed")
                    return@launch
                }

                // Determine feed parameters based on selected feed type
                val feedParams = when (_selectedFeed.value) {
                    "Trends" -> Pair("whats-hot", 50)  // Fetch 50 posts for trends
                    else -> Pair("reverse-chronological", 25)  // Keep 25 for following feed
                }

                // Get timeline with appropriate parameters
                val result = atProtocolRepository.getTimeline(
                    algorithm = feedParams.first,
                    cursor = if (refresh) null else currentCursor,
                    limit = feedParams.second
                )

                result.onSuccess { response ->
                    Log.d(TAG, """
                        ✅ Timeline fetch successful:
                        • Feed type: ${_selectedFeed.value}
                        • Posts fetched: ${response.feed.size}
                        • Algorithm: ${feedParams.first}
                        • Requested limit: ${feedParams.second}
                    """.trimIndent())

                    // Process posts according to Bluesky timestamp spec
                    val filteredPosts = response.feed.mapNotNull { post ->
                        try {
                            // Step 1: Validate basic post structure
                            if (post.post.uri.isEmpty() || post.post.cid.isEmpty()) {
                                Log.w(TAG, "⚠️ Skipping post with missing URI or CID")
                                return@mapNotNull null
                            }

                            // Step 2: Validate reply structure if present
                            post.reply?.let { reply ->
                                // Check root reference
                                if (reply.root?.cid == null || reply.root.uri == null) {
                                    Log.w(TAG, "⚠️ Post ${post.post.uri} has invalid root reference")
                                    return@mapNotNull null
                                }
                                // Check parent reference
                                if (reply.parent?.cid == null || reply.parent.uri == null) {
                                    Log.w(TAG, "⚠️ Post ${post.post.uri} has invalid parent reference")
                                    return@mapNotNull null
                                }
                            }

                            // Step 3: Parse and validate timestamps
                            val createdAt = try {
                                Instant.parse(post.post.record.createdAt)
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Invalid createdAt for post ${post.post.uri}: ${e.message}")
                                return@mapNotNull null
                            }

                            val indexedAt = try {
                                Instant.parse(post.post.indexedAt)
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Invalid indexedAt for post ${post.post.uri}, using current time")
                                Instant.now()
                            }

                            // Step 4: Apply Bluesky's sortAt logic
                            val sortAt = when {
                                createdAt.isBefore(Instant.EPOCH) -> {
                                    Log.w(TAG, "⚠️ Post ${post.post.uri} has pre-epoch createdAt")
                                    null
                                }
                                createdAt.isAfter(indexedAt.plusSeconds(300)) -> { // Add 5-minute skew window
                                    Log.d(TAG, "📅 Post ${post.post.uri} has future createdAt, using indexedAt")
                                    indexedAt
                                }
                                else -> {
                                    Log.d(TAG, "📅 Post ${post.post.uri} using createdAt for sorting")
                                    createdAt
                                }
                            }

                            // Step 5: Skip posts with null sortAt
                            if (sortAt == null) {
                                Log.w(TAG, "⚠️ Skipping post ${post.post.uri} due to null sortAt")
                                return@mapNotNull null
                            }

                            // Step 6: Validate post record
                            if (post.post.record.text.isBlank()) {
                                Log.w(TAG, "⚠️ Skipping post ${post.post.uri} with empty content")
                                return@mapNotNull null
                            }

                            post
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing post: ${e.message}")
                            null
                        }
                    }

                    // Sort posts by sortAt timestamp with proper null handling
                    val sortedPosts = filteredPosts.sortedByDescending { post ->
                        try {
                            val createdAt = Instant.parse(post.post.record.createdAt)
                            val indexedAt = Instant.parse(post.post.indexedAt)
                            when {
                                createdAt.isBefore(Instant.EPOCH) -> Instant.EPOCH
                                createdAt.isAfter(indexedAt.plusSeconds(300)) -> indexedAt
                                else -> createdAt
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error sorting post ${post.post.uri}: ${e.message}")
                            Instant.EPOCH
                        }
                    }

                    Log.d(TAG, """
                        🔍 Post processing results:
                        • Raw feed size: ${response.feed.size}
                        • Valid posts: ${filteredPosts.size}
                        • Filtered out: ${response.feed.size - filteredPosts.size}
                        • Final sorted size: ${sortedPosts.size}
                        • First post time: ${sortedPosts.firstOrNull()?.post?.record?.createdAt}
                    """.trimIndent())

                    if (!isActive) {
                        Log.d(TAG, "⚠️ Coroutine no longer active, skipping update")
                        return@onSuccess
                    }

                    if (sortedPosts.isNotEmpty()) {
                        _threads.value = if (refresh) {
                            sortedPosts
                        } else {
                            _threads.value + sortedPosts
                        }
                        currentCursor = response.cursor
                        loadInitialLikeStates(sortedPosts)
                    } else {
                        Log.w(TAG, "⚠️ No valid posts after filtering and sorting")
                    }
                }.onFailure { error ->
                    if (!isActive) {
                        Log.d(TAG, "⚠️ Coroutine no longer active, skipping error handling")
                        return@onFailure
                    }
                    Log.e(TAG, "❌ Timeline load failed: ${error.message}")
                    error.printStackTrace()
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "⚠️ Timeline load cancelled")
                    return@launch
                }
                Log.e(TAG, "❌ Fatal error in loadThreads: ${e.message}")
                e.printStackTrace()
            } finally {
                if (isActive) {
                    _isLoading.value = false
                    _isRefreshing.value = false
                }
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
            .filter { feedPost ->
                feedPost.post.embed?.let { embed ->
                    embed.video != null || 
                    (embed.images?.isNotEmpty() == true) ||
                    (embed.external?.uri?.let { uri ->
                        uri.contains("video", ignoreCase = true) ||
                        uri.contains("youtube.com") ||
                        uri.contains("youtube") ||
                        uri.contains("vimeo.com")
                    } == true)
                } ?: false
            }
            .map { feedPost ->
                Video(
                    uri = feedPost.post.uri,
                    did = feedPost.post.author.did,
                    handle = feedPost.post.author.handle,
                    videoUrl = feedPost.post.embed?.video?.ref?.link?.let { 
                        "https://cdn.bsky.app/video/plain/$it" 
                    } ?: feedPost.post.embed?.external?.uri ?: "",
                    description = feedPost.post.record.text,
                    createdAt = Instant.parse(feedPost.post.record.createdAt),
                    indexedAt = Instant.now(),
                    sortAt = Instant.now(),
                    title = feedPost.post.record.text,
                    thumbnailUrl = feedPost.post.embed?.images?.firstOrNull()?.thumb ?: "",
                    likes = feedPost.post.likeCount ?: 0,
                    comments = feedPost.post.replyCount ?: 0,
                    shares = feedPost.post.repostCount ?: 0,
                    username = feedPost.post.author.displayName ?: feedPost.post.author.handle,
                    userId = feedPost.post.author.did,
                    isImage = feedPost.post.embed?.video == null,
                    imageUrl = feedPost.post.embed?.images?.firstOrNull()?.image?.link?.let { link ->
                        "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                    } ?: "",
                    aspectRatio = 16f/9f,
                    authorAvatar = feedPost.post.author.avatar ?: "",
                    authorName = feedPost.post.author.displayName ?: feedPost.post.author.handle
                )
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
