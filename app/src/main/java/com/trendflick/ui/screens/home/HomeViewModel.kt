package com.trendflick.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.api.FeedPost
import com.trendflick.data.api.Post
import com.trendflick.data.api.ThreadPost
import com.trendflick.data.api.ThreadResponse
import com.trendflick.data.api.TimelineResponse
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.repository.VideoRepository
import com.trendflick.data.repository.VideoRepositoryImpl
import com.trendflick.data.auth.BlueskyCredentialsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import retrofit2.HttpException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.ArrayList
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import androidx.lifecycle.SavedStateHandle
import com.trendflick.data.model.Video
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import com.trendflick.data.model.TrendingHashtag
import com.trendflick.data.model.Category
import com.trendflick.data.model.categories
import kotlin.Comparable
import kotlin.comparisons.compareByDescending
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.time.Duration

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val atProtocolRepository: AtProtocolRepository,
    private val credentialsManager: BlueskyCredentialsManager,
    private val videoRepository: VideoRepository,
    private val savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _threads = MutableStateFlow<List<FeedPost>>(emptyList())
    val threads: StateFlow<List<FeedPost>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _likedPosts = MutableStateFlow<Set<String>>(emptySet())
    val likedPosts: StateFlow<Set<String>> = _likedPosts.asStateFlow()

    private val _repostedPosts = MutableStateFlow<Set<String>>(emptySet())
    val repostedPosts: StateFlow<Set<String>> = _repostedPosts.asStateFlow()

    private val _currentThread = MutableStateFlow<ThreadPost?>(null)
    val currentThread: StateFlow<ThreadPost?> = _currentThread.asStateFlow()

    // New states for comments
    private val _currentPostComments = MutableStateFlow<List<ThreadPost>>(emptyList())
    val currentPostComments: StateFlow<List<ThreadPost>> = _currentPostComments.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    private val _showComments = MutableStateFlow(false)
    val showComments: StateFlow<Boolean> = _showComments.asStateFlow()

    // Add Firebase references with proper typing
    private var database: FirebaseDatabase? = null

    private val _selectedFeed = MutableStateFlow("Trends")
    val selectedFeed: StateFlow<String> = _selectedFeed.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    // Add missing properties
    private var currentCursor: String? = null
    private var loadingJob: Job? = null

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    private val _videoLoadError = MutableStateFlow<String?>(null)
    val videoLoadError: StateFlow<String?> = _videoLoadError.asStateFlow()

    private val TAG = "TrendFlick_HomeVM"

    private var isLoggedOut = false

    private val _trendingHashtags = MutableStateFlow<List<TrendingHashtag>>(emptyList())
    val trendingHashtags: StateFlow<List<TrendingHashtag>> = _trendingHashtags.asStateFlow()

    private val _currentHashtag = MutableStateFlow<String?>(null)
    val currentHashtag: StateFlow<String?> = _currentHashtag.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    private val _shareEvent = MutableSharedFlow<Intent>()
    val shareEvent = _shareEvent.asSharedFlow()

    private val _showAuthorOnly = MutableStateFlow(false)
    val showAuthorOnly = _showAuthorOnly.asStateFlow()

    companion object {
        private const val MAX_COMMENT_LENGTH = 300
    }

    init {
        Log.d(TAG, "⭐ HomeViewModel initialization starting")
        
        try {
            savedStateHandle.get<String>("selectedFeed")?.let { feed ->
                Log.d(TAG, "📱 Restored selected feed: $feed")
                _selectedFeed.value = feed
            }

            viewModelScope.launch {
                try {
                    Log.d(TAG, "🚀 Starting main initialization")
                    
                    // Initialize Firebase
                    try {
                        Log.d(TAG, "🔥 Initializing Firebase")
                        database = Firebase.database
                        
                        val auth = FirebaseAuth.getInstance()
                        if (auth.currentUser == null) {
                            Log.d(TAG, "👤 No Firebase user, signing in anonymously")
                            auth.signInAnonymously().await()
                            Log.d(TAG, "✅ Firebase anonymous auth completed")
                        } else {
                            Log.d(TAG, "👤 Using existing Firebase user: ${auth.currentUser?.uid}")
                        }
                        Log.d(TAG, "✅ Firebase initialized with auth")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Firebase initialization failed", e)
                    }
                    
                    // Initialize Bluesky session
                    try {
                        Log.d(TAG, "🔑 Starting Bluesky initialization")
                        val handle = credentialsManager.getHandle()
                        val password = credentialsManager.getPassword()
                        
                        Log.d(TAG, """
                            🔍 Credentials check:
                            Handle exists: ${!handle.isNullOrEmpty()}
                            Password exists: ${!password.isNullOrEmpty()}
                            Handle: ${handle ?: "null"}
                        """.trimIndent())
                        
                        if (!handle.isNullOrEmpty() && !password.isNullOrEmpty()) {
                            Log.d(TAG, "🔐 Creating Bluesky session for handle: $handle")
                            
                            val sessionResult = atProtocolRepository.createSession(handle, password)
                            
                            sessionResult.onSuccess { session ->
                                Log.d(TAG, """
                                    ✅ Session created successfully:
                                    Handle: ${session.handle}
                                    DID: ${session.did}
                                    Email: ${session.email}
                                """.trimIndent())
                                
                                isLoggedOut = false
                                
                                // Load initial content
                                Log.d(TAG, "📥 Loading initial content")
                                loadThreads()
                                if (_selectedFeed.value == "Flicks") {
                                    refreshVideoFeed()
                                }
                            }.onFailure { e ->
                                Log.e(TAG, """
                                    ❌ Session creation failed:
                                    Error: ${e.message}
                                    Type: ${e.javaClass.name}
                                    Stack: ${e.stackTraceToString()}
                                """.trimIndent())
                                isLoggedOut = true
                            }
                        } else {
                            Log.e(TAG, "❌ No valid credentials found")
                            isLoggedOut = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, """
                            ❌ Bluesky initialization failed:
                            Error: ${e.message}
                            Type: ${e.javaClass.name}
                            Stack: ${e.stackTraceToString()}
                        """.trimIndent())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, """
                        ❌ Critical initialization error:
                        Error: ${e.message}
                        Type: ${e.javaClass.name}
                        Stack: ${e.stackTraceToString()}
                    """.trimIndent())
                }
            }

            // Load trending hashtags in parallel
            viewModelScope.launch {
                try {
                    Log.d(TAG, "🏷️ Loading trending hashtags")
                    loadTrendingHashtags()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to load trending hashtags: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Fatal initialization error:
                Error: ${e.message}
                Type: ${e.javaClass.name}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
        }
    }

    private fun verifyCredentials() {
        viewModelScope.launch {
            try {
                val handle = credentialsManager.getHandle()
                val password = credentialsManager.getPassword()
                val did = credentialsManager.getDid()
                
                println("DEBUG: TrendFlick - Credential Check:")
                println("DEBUG: TrendFlick - Handle present: ${!handle.isNullOrEmpty()}")
                println("DEBUG: TrendFlick - Password present: ${!password.isNullOrEmpty()}")
                println("DEBUG: TrendFlick - DID present: ${!did.isNullOrEmpty()}")
                
                if (handle.isNullOrEmpty() || password.isNullOrEmpty()) {
                    println("DEBUG: TrendFlick - ERROR: Missing credentials - Please check BlueSky handle and app password")
                }
            } catch (e: Exception) {
                println("DEBUG: TrendFlick - ERROR checking credentials: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun ensureValidSession(): Boolean {
        if (isLoggedOut) {
            Log.d(TAG, "🔒 Skipping session validation - user is logged out")
            return false
        }
        
        try {
            Log.d(TAG, "🔐 Starting session validation")
            
            val (handle, password) = credentialsManager.getCredentials()
            if (handle.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.e(TAG, "❌ No valid credentials found")
                isLoggedOut = true
                return false
            }
            
            val currentSession = atProtocolRepository.getCurrentSession()
            if (currentSession != null) {
                Log.d(TAG, "✅ Found existing valid session for handle: ${currentSession.handle}")
                isLoggedOut = false
                return true
            }
            
            Log.d(TAG, "🔍 No valid session found, attempting to create new session")
            
            var sessionCreated = false
            atProtocolRepository.createSession(handle, password)
                .onSuccess { 
                    Log.d(TAG, "✅ Successfully created new session for handle: $handle")
                    isLoggedOut = false
                    sessionCreated = true
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Failed to create session: ${e.message}")
                    Log.e(TAG, "❌ Error type: ${e.javaClass.name}")
                    if (e.message?.contains("Unauthorized") == true || 
                        e.message?.contains("Invalid credentials") == true ||
                        e.message?.contains("Not authenticated") == true) {
                        Log.d(TAG, "🔒 Setting logged out state due to auth error")
                        isLoggedOut = true
                        credentialsManager.clearCredentials()
                    }
                    e.printStackTrace()
                }
            return sessionCreated
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session validation failed", e)
            if (e.message?.contains("Not authenticated") == true || 
                e.message?.contains("Unauthorized") == true) {
                Log.d(TAG, "🔒 Setting logged out state due to auth error")
                isLoggedOut = true
                credentialsManager.clearCredentials()
            }
            return false
        }
    }

    fun loadThreads(isRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Loading threads (refresh: $isRefresh)")
                _isLoading.value = true
                
                // Ensure we have valid credentials
                if (!credentialsManager.hasValidCredentials()) {
                    Log.e(TAG, "❌ No valid credentials found")
                    _isLoading.value = false
                    return@launch
                }

                // Ensure valid session
                ensureValidSession()
                
                if (isLoggedOut) {
                    Log.e(TAG, "❌ User is logged out")
                    _isLoading.value = false
                    return@launch
                }
                
                if (isRefresh) {
                    currentCursor = null
                    _threads.value = emptyList()
                }
                
                Log.d(TAG, "🌐 Fetching timeline with cursor: $currentCursor")
                
                val result = if (_currentHashtag.value != null) {
                    Log.d(TAG, "🏷️ Fetching posts for hashtag: ${_currentHashtag.value}")
                    atProtocolRepository.getPostsByHashtag(
                        hashtag = _currentHashtag.value!!,
                        cursor = if (isRefresh) null else currentCursor
                    )
                } else {
                    atProtocolRepository.getTimeline(
                        algorithm = when (_selectedFeed.value) {
                            "Following" -> "reverse-chronological"
                            else -> "whats-hot"
                        },
                        cursor = if (isRefresh) null else currentCursor
                    )
                }
                
                result.onSuccess { response ->
                    Log.d(TAG, """
                        ✅ Timeline fetched:
                        Posts: ${response.feed.size}
                        Cursor: ${response.cursor}
                        First post: ${response.feed.firstOrNull()?.post?.uri}
                    """.trimIndent())
                    
                    val filteredPosts = response.feed.filter { post ->
                        post.post.uri.isNotEmpty() && 
                        post.post.cid.isNotEmpty() &&
                        post.post.record.reply == null
                    }
                    
                    _threads.value = if (isRefresh) {
                        filteredPosts
                    } else {
                        _threads.value + filteredPosts
                    }
                    
                    loadInitialLikeStates(filteredPosts)
                    currentCursor = response.cursor
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load threads: ${error.message}")
                    error.printStackTrace()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading threads: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreThreads(isRefresh: Boolean = false) {
        if (_isLoading.value || loadingJob?.isActive == true) return
        
        loadingJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val result = if (_currentHashtag.value != null) {
                    atProtocolRepository.getPostsByHashtag(
                        hashtag = _currentHashtag.value!!,
                        cursor = if (isRefresh) null else currentCursor
                    )
                } else {
                    atProtocolRepository.getTimeline(
                        algorithm = when (_selectedFeed.value) {
                            "Following" -> "reverse-chronological"
                            else -> "whats-hot"
                        },
                        cursor = if (isRefresh) null else currentCursor
                    )
                }
                
                result.onSuccess { response ->
                    val filteredPosts = response.feed.filter { post ->
                        post.post.uri.isNotEmpty() && 
                        post.post.cid.isNotEmpty() &&
                        post.post.record.reply == null
                    }
                    _threads.value = if (isRefresh) {
                        filteredPosts
                    } else {
                        _threads.value + filteredPosts
                    }
                    loadInitialLikeStates(filteredPosts)
                    currentCursor = response.cursor
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more threads: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLoading() {
        val currentCategory = _selectedFeed.value
        if (currentCategory != null) {
            filterByCategory(currentCategory)
        } else {
            loadThreads()
        }
    }

    fun filterByCategory(category: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 [AT Protocol] Starting category filter for: '$category'")
                _isLoading.value = true
                currentCursor = null
                _threads.value = emptyList()
                
                _selectedFeed.value = category
                Log.d(TAG, "📌 [AT Protocol] Selected feed updated to: '$category'")
                
                val categoryHashtags = categories.find { 
                    it.name.equals(category, ignoreCase = true) 
                }?.hashtags
                
                Log.d(TAG, "📋 [AT Protocol] Category hashtags: ${categoryHashtags?.joinToString() ?: "none found"}")

                val result = when {
                    category.equals("FYP", ignoreCase = true) -> {
                        Log.d(TAG, "📱 [AT Protocol] Loading FYP feed with whats-hot algorithm")
                        atProtocolRepository.getTimeline(algorithm = "whats-hot", limit = 50)
                    }
                    category.equals("Following", ignoreCase = true) -> {
                        Log.d(TAG, "👥 [AT Protocol] Loading Following feed with reverse-chronological algorithm")
                        atProtocolRepository.getTimeline(algorithm = "reverse-chronological", limit = 50)
                    }
                    !categoryHashtags.isNullOrEmpty() -> {
                        Log.d(TAG, "🏷️ [AT Protocol] Loading posts for category: $category with ${categoryHashtags.size} hashtags")
                        
                        var foundPosts = false
                        var finalResult: Result<TimelineResponse>? = null
                        
                        // Try each hashtag in parallel for better performance
                        val hashtagResults = categoryHashtags.map { hashtag ->
                            async {
                                try {
                                    Log.d(TAG, "🔍 [AT Protocol] Searching hashtag: #$hashtag")
                                    val result = atProtocolRepository.getPostsByHashtag(
                                        hashtag = hashtag,
                                        limit = 100  // Increased limit for better results
                                    )
                                    Log.d(TAG, "📥 [AT Protocol] Result for #$hashtag: ${result.isSuccess}")
                                    result to hashtag
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ [AT Protocol] Error searching #$hashtag: ${e.message}")
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                        
                        // Combine all successful results
                        val allPosts = hashtagResults.flatMap { (result, hashtag) ->
                            result.getOrNull()?.feed?.map { it to hashtag } ?: emptyList()
                        }
                        
                        Log.d(TAG, "📊 [AT Protocol] Found ${allPosts.size} total posts across all hashtags")
                        
                        if (allPosts.isNotEmpty()) {
                            // Group posts by their hashtags for better filtering
                            val postsByHashtag = allPosts.groupBy { it.second }
                            
                            // Filter and deduplicate posts
                            val filteredPosts = allPosts.map { it.first }
                                .distinctBy { it.post.uri }
                                .filter { feedPost ->
                                    val postText = feedPost.post.record.text.lowercase()
                                    Log.d(TAG, "📝 [AT Protocol] Checking post: ${postText.take(100)}...")
                                    
                                    categoryHashtags.any { tag ->
                                        val variations = listOf(
                                            "#${tag.lowercase()}",
                                            "#${tag.lowercase().replace(" ", "")}",
                                            "#${tag.lowercase().replace("&", "and")}",
                                            "#${tag.lowercase().replace(" ", "_")}",
                                            tag.lowercase()  // Also match without hashtag for context
                                        )
                                        
                                        val containsVariation = variations.any { variation -> 
                                            postText.contains(variation)
                                        }
                                        
                                        if (containsVariation) {
                                            Log.d(TAG, "✅ [AT Protocol] Post matches hashtag: #$tag")
                                        }
                                        
                                        containsVariation
                                    }
                                }
                            
                            Log.d(TAG, "✨ [AT Protocol] After filtering: ${filteredPosts.size} unique relevant posts")
                            
                            if (filteredPosts.isNotEmpty()) {
                                // Sort by engagement and recency
                                val sortedPosts = filteredPosts.sortedByDescending { post ->
                                    val engagement = (post.post.likeCount ?: 0) + (post.post.repostCount ?: 0)
                                    val age = Duration.between(
                                        Instant.parse(post.post.record.createdAt),
                                        Instant.now()
                                    ).toHours()
                                    engagement.toDouble() / (age + 1) // Add 1 to avoid division by zero
                                }
                                
                                finalResult = Result.success(TimelineResponse(
                                    feed = sortedPosts.take(50),
                                    cursor = null  // We'll handle pagination differently for combined results
                                ))
                            }
                        }
                        
                        finalResult ?: run {
                            Log.d(TAG, "⚠️ [AT Protocol] No posts found with hashtags, trying timeline")
                            atProtocolRepository.getTimeline(
                                algorithm = "whats-hot",
                                limit = 200
                            ).map { response ->
                                TimelineResponse(
                                    feed = response.feed.filter { feedPost ->
                                        val postText = feedPost.post.record.text.lowercase()
                                        categoryHashtags.any { tag ->
                                            postText.contains(tag.lowercase())
                                        }
                                    }.take(50),
                                    cursor = response.cursor
                                )
                            }
                        }
                    }
                    else -> {
                        Log.d(TAG, "⚠️ [AT Protocol] No category hashtags found, defaulting to timeline")
                        atProtocolRepository.getTimeline(algorithm = "whats-hot", limit = 50)
                    }
                }

                result.onSuccess { response ->
                    Log.d(TAG, "✅ [AT Protocol] Successfully loaded ${response.feed.size} posts for category $category")
                    _threads.value = response.feed
                    currentCursor = response.cursor
                    loadInitialLikeStates(response.feed)
                }.onFailure { e ->
                    Log.e(TAG, "❌ [AT Protocol] Failed to filter by category: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [AT Protocol] Failed to filter by category: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadInitialLikeStates(posts: List<FeedPost>) {
        viewModelScope.launch {
            try {
                println("DEBUG: TrendFlick 💾 [TRENDS] Loading initial like and repost states for ${posts.size} posts")
                
                val currentLiked = _likedPosts.value.toMutableSet()
                val currentReposted = _repostedPosts.value.toMutableSet()
                println("DEBUG: TrendFlick 💾 [TRENDS] Current likes in memory before loading: ${currentLiked.size}")
                println("DEBUG: TrendFlick 💾 [TRENDS] Current reposts in memory before loading: ${currentReposted.size}")
                
                posts.forEach { feedPost ->
                    try {
                        println("DEBUG: TrendFlick 🔍 [TRENDS] Checking like and repost status for post: ${feedPost.post.uri}")
                        if (atProtocolRepository.isPostLikedByUser(feedPost.post.uri)) {
                            currentLiked.add(feedPost.post.uri)
                            println("DEBUG: TrendFlick ✅ [TRENDS] Post ${feedPost.post.uri} is liked")
                        }
                        if (atProtocolRepository.isPostRepostedByUser(feedPost.post.uri)) {
                            currentReposted.add(feedPost.post.uri)
                            println("DEBUG: TrendFlick ✅ [TRENDS] Post ${feedPost.post.uri} is reposted")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: TrendFlick ❌ [TRENDS] Failed to check like/repost status for post ${feedPost.post.uri}: ${e.message}")
                    }
                }
                
                _likedPosts.value = currentLiked
                _repostedPosts.value = currentReposted
                println("DEBUG: TrendFlick 💾 [TRENDS] Updated liked posts set, total liked: ${currentLiked.size}")
                println("DEBUG: TrendFlick 💾 [TRENDS] Updated reposted posts set, total reposted: ${currentReposted.size}")
            } catch (e: Exception) {
                println("DEBUG: TrendFlick ❌ [TRENDS] Failed to load initial like/repost states: ${e.message}")
                println("DEBUG: TrendFlick ❌ [TRENDS] Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    fun refreshLikeStates() {
        viewModelScope.launch {
            loadInitialLikeStates(_threads.value)
        }
    }

    private suspend fun fetchThreadDetails(uri: String): Result<ThreadResponse> {
        return atProtocolRepository.getPostThread(uri)
    }

    private fun handleThreadFetch(threadUri: String) {
        viewModelScope.launch {
            try {
                val threadResult = fetchThreadDetails(threadUri)
                threadResult.onSuccess { threadResponse ->
                    val threadPosts = threadResponse.thread.replies?.map { reply ->
                        FeedPost(post = reply.post)
                    } ?: emptyList()
                    
                    _threads.value = threadPosts
                }.onFailure { error ->
                    _threads.value = emptyList()
                }
            } catch (e: Exception) {
                _threads.value = emptyList()
            }
        }
    }

    fun loadThread(uri: String) {
        viewModelScope.launch {
            try {
                _isLoadingComments.value = true
                val threadResult = atProtocolRepository.getPostThread(uri)
                threadResult.onSuccess { response ->
                    _currentThread.value = response.thread
                    
                    val currentLiked = _likedPosts.value.toMutableSet()
                    
                    if (atProtocolRepository.isPostLikedByUser(response.thread.post.uri)) {
                        currentLiked.add(response.thread.post.uri)
                    }
                    
                    response.thread.replies?.forEach { reply: ThreadPost ->
                        if (atProtocolRepository.isPostLikedByUser(reply.post.uri)) {
                            currentLiked.add(reply.post.uri)
                        }
                    }
                    
                    _likedPosts.value = currentLiked
                }
            } catch (e: Exception) {
                println("DEBUG: ViewModel - Failed to load thread: ${e.message}")
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    fun repost(uri: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Starting repost process for URI: $uri")
                val postThread = atProtocolRepository.getPostThread(uri)
                postThread.onSuccess { threadResponse ->
                    val post = threadResponse.thread.post
                    Log.d(TAG, """
                        📝 Thread Response Details:
                        Post URI: ${post.uri}
                        Post CID: ${post.cid}
                        Raw CID value type: ${post.cid::class.java.simpleName}
                        Raw Response: $threadResponse
                    """.trimIndent())
                    
                    if (post.cid.startsWith("bafyrei") || post.cid.startsWith("bafy")) {
                        Log.d(TAG, "📝 Post details retrieved - URI: ${post.uri}, CID: ${post.cid}")
                        atProtocolRepository.repost(post.uri, post.cid)
                        Log.d(TAG, "✅ Repost request sent successfully")
                        _repostedPosts.value = if (uri in _repostedPosts.value) {
                            _repostedPosts.value - uri
                        } else {
                            _repostedPosts.value + uri
                        }
                    } else {
                        Log.e(TAG, "❌ Invalid CID format: ${post.cid}")
                        throw IllegalStateException("Invalid CID format received from server")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to get post details for repost: ${error.message}")
                    Log.e(TAG, "Stack trace: ${error.stackTraceToString()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to repost: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    fun checkRepostStatus(uri: String) {
        viewModelScope.launch {
            try {
                val isReposted = atProtocolRepository.isPostRepostedByUser(uri)
                if (isReposted) {
                    _repostedPosts.value = _repostedPosts.value + uri
                } else {
                    _repostedPosts.value = _repostedPosts.value - uri
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check repost status: ${e.message}")
            }
        }
    }

    fun sharePost(uri: String) {
        viewModelScope.launch {
            try {
                val threadResult = atProtocolRepository.getPostThread(uri)
                threadResult.onSuccess { response ->
                    val post = response.thread.post
                    val handle = post.author.handle
                    
                    val rkey = uri.substringAfterLast('/')
                    val shareUrl = "https://bsky.app/profile/$handle/post/$rkey"
                    
                    val postPreview = post.record.text.take(100).let {
                        if (post.record.text.length > 100) "$it..." else it
                    }
                    
                    val shareText = """
                        📱 Shared via TrendFlick
                        
                        ${post.author.displayName ?: "@$handle"}:
                        "$postPreview"
                        
                        View on BlueSky: $shareUrl
                        
                        Download TrendFlick: [Coming Soon to Play Store]
                    """.trimIndent()
                    
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    
                    _shareEvent.emit(shareIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to share post: ${e.message}")
                _errorEvents.emit("Failed to share post")
            }
        }
    }

    fun toggleComments(show: Boolean) {
        _showComments.value = show
    }

    fun loadComments(postUri: String) {
        viewModelScope.launch {
            try {
                _isLoadingComments.value = true
                
                val threadResult = atProtocolRepository.getPostThread(postUri)
                threadResult.onSuccess { response ->
                    _currentThread.value = response.thread
                    
                    val originalAuthorDid = response.thread.post.author.did
                    println("DEBUG: TrendFlick - Original poster DID: $originalAuthorDid")
                    
                    val comments = mutableListOf<ThreadPost>()
                    
                    comments.add(response.thread)
                    
                    response.thread.replies?.forEach { reply ->
                        println("DEBUG: TrendFlick - Reply from: ${reply.post.author.did}, isOP: ${reply.post.author.did == originalAuthorDid}")
                        comments.add(reply)
                        
                        reply.replies?.forEach { nestedReply ->
                            println("DEBUG: TrendFlick - Nested reply from: ${nestedReply.post.author.did}, isOP: ${nestedReply.post.author.did == originalAuthorDid}")
                            comments.add(nestedReply)
                        }
                    }
                    
                    _currentPostComments.value = comments
                    println("DEBUG: TrendFlick - Total comments loaded: ${comments.size}")
                }
            } catch (e: Exception) {
                println("DEBUG: TrendFlick - Failed to load comments: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    fun postComment(parentUri: String, text: String) {
        if (text.length > MAX_COMMENT_LENGTH) {
            viewModelScope.launch {
                _errorEvents.emit("Comment exceeds BlueSky character limit of $MAX_COMMENT_LENGTH")
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "🌐 Creating reply to post: $parentUri")
                _isLoadingComments.value = true
                val threadResult = atProtocolRepository.getPostThread(parentUri)
                threadResult.onSuccess { response ->
                    val replyRefs = buildReplyReferences(response.thread)
                    
                    val timestamp = Instant.now().toString()
                    Log.d(TAG, "🌐 Sending reply to AT Protocol")
                    atProtocolRepository.createReply(
                        text = text,
                        parentUri = parentUri,
                        parentCid = replyRefs.parentCid,
                        timestamp = timestamp
                    ).onSuccess {
                        Log.d(TAG, "✅ Reply created successfully")
                        delay(1000)
                        loadComments(parentUri)
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Failed to create reply: ${error.message}")
                        _errorEvents.emit("Failed to create reply: ${error.message}")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to get thread: ${error.message}")
                    _errorEvents.emit("Failed to get thread: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to post comment: ${e.message}")
                _errorEvents.emit("Error posting comment: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    private data class ReplyReferences(
        val rootUri: String,
        val parentCid: String
    )

    private fun buildReplyReferences(threadPost: ThreadPost): ReplyReferences {
        val rootRef = threadPost.parent?.post ?: threadPost.post
        
        return ReplyReferences(
            rootUri = rootRef.uri,
            parentCid = threadPost.post.cid
        )
    }

    fun createReply(text: String, parentUri: String, parentCid: String) {
        viewModelScope.launch {
            try {
                val timestamp = Instant.now().toString()
                val result = atProtocolRepository.createReply(
                    text = text,
                    parentUri = parentUri,
                    parentCid = parentCid,
                    timestamp = timestamp
                )
                
                result.onSuccess {
                    handleThreadFetch(parentUri)
                }.onFailure { error ->
                    _threads.value = emptyList()
                }
            } catch (e: Exception) {
                _threads.value = emptyList()
            }
        }
    }

    fun updateSelectedFeed(feed: String) {
        viewModelScope.launch {
            Log.d(TAG, "📱 Switching feed to: $feed")
            _selectedFeed.value = feed
            savedStateHandle.set("selectedFeed", feed)
            
            when (feed) {
                "Flicks" -> {
                    Log.d(TAG, "🎥 Loading video feed")
                    refreshVideoFeed()
                }
                else -> {
                    Log.d(TAG, "📜 Loading posts feed")
                    loadThreads(isRefresh = true)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                
                if (videoRepository is VideoRepositoryImpl) {
                    Log.d(TAG, "🧹 Starting cleanup of test videos...")
                    (videoRepository as VideoRepositoryImpl).cleanupTestVideos()
                }
                
                val refreshedVideos = videoRepository.getVideos()
                _videos.value = refreshedVideos
                
                refreshLikeStates()
                
                verifyCredentials()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error refreshing: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun testFirestore() {
        viewModelScope.launch {
            try {
                if (videoRepository is VideoRepositoryImpl) {
                    videoRepository.testSaveVideo()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error testing Firestore: ${e.message}")
            }
        }
    }

    fun refreshVideoFeed() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🎥 Refreshing video feed")
                _isLoadingVideos.value = true
                _videoLoadError.value = null
                
                // Ensure valid session
                val hasValidSession = ensureValidSession()
                if (!hasValidSession) {
                    Log.e(TAG, "❌ No valid session for video feed")
                    _videoLoadError.value = "Please log in to view videos"
                    return@launch
                }
                
                val posts = atProtocolRepository.getMediaPosts()
                Log.d(TAG, "📱 Found ${posts.size} media posts")
                
                if (posts.isEmpty()) {
                    Log.w(TAG, "⚠️ No media posts found")
                    _videoLoadError.value = "No media posts found. Pull down to refresh."
                } else {
                    posts.forEach { video ->
                        Log.d(TAG, """
                            🎬 Video:
                            URI: ${video.uri}
                            URL: ${video.videoUrl}
                            Type: ${if (video.isImage) "Image" else "Video"}
                            Description: ${video.description}
                        """.trimIndent())
                    }
                }
                
                _videos.value = posts
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading video feed: ${e.message}")
                e.printStackTrace()
                _videoLoadError.value = e.message ?: "Failed to load media posts"
            } finally {
                _isLoadingVideos.value = false
            }
        }
    }

    fun loadMoreVideos() {
        // TODO: Implement pagination for videos if needed
    }

    fun testVideoLoading() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 TEST: Starting video load test")
                val videos = videoRepository.getVideos()
                Log.d(TAG, "🧪 TEST: Found ${videos.size} videos")
                videos.forEach { video ->
                    Log.d(TAG, """
                        📼 VIDEO DETAILS:
                        URI: ${video.uri}
                        URL: ${video.videoUrl}
                        Created: ${video.createdAt}
                        Description: ${video.description}
                        BlueSky: ${video.uri.startsWith("at://")}
                    """.trimIndent())
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ TEST ERROR: ${e.message}")
                Log.e(TAG, "❌ STACK: ${e.stackTraceToString()}")
            }
        }
    }

    fun initializeBlueSky(handle: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔐 Initializing BlueSky credentials")
                
                credentialsManager.saveCredentials(handle, password)
                
                if (credentialsManager.hasValidCredentials()) {
                    Log.d(TAG, "🔑 Creating initial session")
                    atProtocolRepository.createSession(handle, password)
                        .onSuccess { session ->
                            Log.d(TAG, """
                                ✅ Session created:
                                Handle: ${session.handle}
                                DID: ${session.did}
                            """.trimIndent())
                            
                            loadThreads()
                        }
                        .onFailure { e ->
                            Log.e(TAG, "❌ Failed to create session: ${e.message}")
                        }
                } else {
                    Log.e(TAG, "❌ Invalid credentials provided")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing BlueSky: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    private fun loadTrendingHashtags() {
        viewModelScope.launch {
            try {
                val hashtags = atProtocolRepository.getTrendingHashtags()
                _trendingHashtags.value = hashtags
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load trending hashtags: ${e.message}")
            }
        }
    }

    fun setCurrentHashtag(hashtag: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _currentHashtag.value = hashtag
                
                if (hashtag != null) {
                    _selectedFeed.value = "#$hashtag"
                }
                
                if (hashtag != null) {
                    val result = atProtocolRepository.getPostsByHashtag(hashtag)
                    result.onSuccess { response ->
                        _threads.value = response.feed
                        currentCursor = response.cursor
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to load hashtag posts: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load hashtag posts: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onHashtagSelected(hashtag: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _currentHashtag.value = hashtag
                currentCursor = null
                _threads.value = emptyList()
                
                atProtocolRepository.getPostsByHashtag(hashtag)
                    .onSuccess { response ->
                        val filteredPosts = response.feed.filter { post ->
                            post.post.uri.isNotEmpty() && post.post.cid.isNotEmpty()
                        }
                        _threads.value = filteredPosts
                        loadInitialLikeStates(filteredPosts)
                        currentCursor = response.cursor
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load hashtag posts: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearHashtagFilter() {
        _currentHashtag.value = null
        loadThreads(isRefresh = true)
    }

    fun toggleAuthorOnly() {
        _showAuthorOnly.value = !_showAuthorOnly.value
    }

    fun toggleLike(postUri: String) {
        Log.d(TAG, "💜 [TRENDS] Toggle like called for post: $postUri")
        val userId = credentialsManager.getDid()
        if (userId == null) {
            Log.e(TAG, "❌ [TRENDS] Cannot toggle like - user not logged in")
            return
        }

        viewModelScope.launch {
            try {
                val currentLikes = _likedPosts.value
                val isCurrentlyLiked = currentLikes.contains(postUri)
                Log.d(TAG, "🔍 [TRENDS] Current like state - isLiked: $isCurrentlyLiked")

                Log.d(TAG, "🌐 [TRENDS] Calling AT Protocol like endpoint")
                try {
                    val likeResult = atProtocolRepository.likePost(postUri)
                    _likedPosts.value = if (likeResult) {
                        currentLikes + postUri
                    } else {
                        currentLikes - postUri
                    }
                    Log.d(TAG, "✅ [TRENDS] Local state updated after successful AT Protocol operation")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [TRENDS] AT Protocol operation failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [TRENDS] Error in toggleLike: ${e.message}")
                Log.e(TAG, "❌ [TRENDS] Stack trace: ${e.stackTraceToString()}")
            }
        }
    }
}

   
   