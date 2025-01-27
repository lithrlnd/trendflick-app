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
    private var likesRef: DatabaseReference? = null

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

    private val TAG = "TF_Home"  // Add this at the top of the class

    private var isLoggedOut = false // Add this at the top with other properties

    private val _trendingHashtags = MutableStateFlow<List<TrendingHashtag>>(emptyList())
    val trendingHashtags: StateFlow<List<TrendingHashtag>> = _trendingHashtags.asStateFlow()

    private val _currentHashtag = MutableStateFlow<String?>(null)
    val currentHashtag: StateFlow<String?> = _currentHashtag.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    companion object {
        private const val MAX_COMMENT_LENGTH = 300 // BlueSky character limit
    }

    init {
        // Restore selected feed from saved state
        savedStateHandle.get<String>("selectedFeed")?.let { feed ->
            _selectedFeed.value = feed
        }

        // Use a single parent coroutine for initialization
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚀 Starting HomeViewModel initialization")
                
                // Step 1: Initialize Firebase and ensure anonymous auth
                try {
                    database = Firebase.database
                    likesRef = database?.getReference("user_likes")
                    
                    // Ensure Firebase Auth is initialized and we have an anonymous user
                    val auth = FirebaseAuth.getInstance()
                    if (auth.currentUser == null) {
                        auth.signInAnonymously().await()
                        Log.d(TAG, "✅ Firebase anonymous auth completed")
                    }
                    Log.d(TAG, "✅ Firebase initialized with auth")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Firebase initialization failed: ${e.message}")
                }
                
                // Step 2: Initialize session and load data sequentially
                try {
                    val handle = credentialsManager.getHandle()
                    val password = credentialsManager.getPassword()
                    
                    Log.d(TAG, "🔍 Credentials check - Handle exists: ${!handle.isNullOrEmpty()}, Password exists: ${!password.isNullOrEmpty()}")
                    
                    if (!handle.isNullOrEmpty() && !password.isNullOrEmpty()) {
                        Log.d(TAG, "🔑 Found credentials, creating session")
                        
                        // Create session and wait for result
                        val sessionResult = atProtocolRepository.createSession(handle, password)
                        
                        sessionResult.onSuccess { session ->
                            Log.d(TAG, "✅ Session created for ${session.handle}")
                            isLoggedOut = false
                            
                            // Load data sequentially to prevent race conditions
                            loadPersistedLikes()
                            delay(500) // Give time for likes to load
                            
                            // Load initial threads
                            loadThreads()
                        }.onFailure { e ->
                            Log.e(TAG, "❌ Session creation failed: ${e.message}")
                            isLoggedOut = true
                        }
                    } else {
                        Log.e(TAG, "❌ No credentials found - Handle: $handle")
                        isLoggedOut = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Session initialization failed: ${e.message}")
                    isLoggedOut = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ HomeViewModel initialization failed: ${e.message}")
                isLoggedOut = true
            }
        }

        loadTrendingHashtags()
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

    private suspend fun ensureValidSession() {
        if (isLoggedOut) {
            Log.d(TAG, "🔒 Skipping session validation - user is logged out")
            return
        }
        
        try {
            Log.d(TAG, "🔐 Starting session validation")
            
            // First check if we have valid credentials
            val (handle, password) = credentialsManager.getCredentials()
            if (handle.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.e(TAG, "❌ No valid credentials found")
                isLoggedOut = true
                return
            }
            
            // Then check if we have a valid session
            val currentSession = atProtocolRepository.getCurrentSession()
            if (currentSession != null) {
                Log.d(TAG, "✅ Found existing valid session for handle: ${currentSession.handle}")
                isLoggedOut = false
                return
            }
            
            // If no valid session, try to create one
            Log.d(TAG, "🔍 No valid session found, attempting to create new session")
            
            atProtocolRepository.createSession(handle, password)
                .onSuccess { 
                    Log.d(TAG, "✅ Successfully created new session for handle: $handle")
                    isLoggedOut = false
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
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session validation failed", e)
            if (e.message?.contains("Not authenticated") == true || 
                e.message?.contains("Unauthorized") == true) {
                Log.d(TAG, "🔒 Setting logged out state due to auth error")
                isLoggedOut = true
                credentialsManager.clearCredentials()
            }
        }
    }

    fun loadThreads(isRefresh: Boolean = false) {
        viewModelScope.launch {
            loadThreadsInternal(isRefresh)
        }
    }

    private suspend fun loadThreadsInternal(isRefresh: Boolean = false) {
        if (isLoggedOut) {
            Log.d(TAG, "🔒 Skipping thread load - user is logged out")
            return
        }

        try {
            _isLoading.value = true
            
            // Ensure we have a valid session before proceeding
            ensureValidSession()
            
            if (isLoggedOut) {
                Log.d(TAG, "🔒 Aborting thread load - lost session during validation")
                return
            }
            
            // Clear cursor but keep existing threads to avoid flicker
            if (isRefresh) {
                currentCursor = null
                _threads.value = emptyList()
            }
            
            // Add delay to ensure session is properly established
            delay(500)
            
            // Single attempt with proper error handling
            try {
                loadMoreThreads(isRefresh)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Thread load failed: ${e.message}")
                // If it's a connection closed error, wait and retry once
                if (e.message?.contains("closed") == true) {
                    Log.d(TAG, "🔄 Connection closed, retrying after delay")
                    delay(1000)
                    try {
                        loadMoreThreads(isRefresh)
                    } catch (retryE: Exception) {
                        Log.e(TAG, "❌ Retry failed: ${retryE.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in loadThreads: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
        } finally {
            _isLoading.value = false
            loadingJob = null
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
                        // Only include posts that:
                        // 1. Have valid URI and CID
                        // 2. Are not replies (don't have a reply.parent field)
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

    // Function to retry loading if it failed
    fun retryLoading() {
        loadThreads()
    }

    fun filterByCategory(category: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                currentCursor = null
                _threads.value = emptyList()
                
                // Update selected feed
                _selectedFeed.value = category
                
                // Get hashtags for the selected category
                val categoryHashtags = categories.find { it.name.lowercase() == category.lowercase() }?.hashtags

                val result = when {
                    category.lowercase() == "fyp" -> atProtocolRepository.getTimeline(
                        algorithm = "whats-hot",
                        limit = 50
                    )
                    category.lowercase() == "following" -> atProtocolRepository.getTimeline(
                        algorithm = "reverse-chronological",
                        limit = 50
                    )
                    categoryHashtags != null -> {
                        Log.d(TAG, """
                            🔍 Category Feed Request:
                            • Category: $category
                            • Hashtags: ${categoryHashtags.joinToString()}
                        """.trimIndent())
                        
                        // Get a larger feed to ensure we have enough posts after filtering
                        val result = atProtocolRepository.getTimeline(
                            algorithm = "whats-hot",
                            limit = 100
                        )
                        
                        result.map { response ->
                            // Filter posts that contain any of the category hashtags
                            val filteredPosts = response.feed.filter { feedPost ->
                                categoryHashtags.any { hashtag ->
                                    feedPost.post.record.text.lowercase().contains("#${hashtag.lowercase()}")
                                }
                            }.take(50)
                            
                            Log.d(TAG, """
                                ✅ Category feed assembled:
                                • Total posts: ${filteredPosts.size}
                                • From hashtags: ${categoryHashtags.joinToString()}
                            """.trimIndent())
                            
                            TimelineResponse(
                                feed = filteredPosts,
                                cursor = response.cursor
                            )
                        }
                    }
                    else -> atProtocolRepository.getTimeline(
                        algorithm = "reverse-chronological",
                        limit = 50
                    )
                }

                result.onSuccess { response ->
                    _threads.value = response.feed
                    currentCursor = response.cursor
                    
                    // Load like states for the new posts
                    loadInitialLikeStates(response.feed)
                }.onFailure { e ->
                    Log.e(TAG, "❌ Failed to filter by category: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to filter by category: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadPersistedLikes() {
        val userId = credentialsManager.getDid() ?: return
        println("DEBUG: TrendFlick 💾 Loading persisted likes for user: $userId")
        
        likesRef?.child(userId)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likes = snapshot.children.mapNotNull { it.key }.toSet()
                println("DEBUG: TrendFlick 💾 Loaded ${likes.size} persisted likes")
                _likedPosts.value = likes
            }

            override fun onCancelled(error: DatabaseError) {
                println("DEBUG: TrendFlick ❌ Failed to load persisted likes: ${error.message}")
            }
        })
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

                // Call AT Protocol first to ensure network state is updated
                Log.d(TAG, "🌐 [TRENDS] Calling AT Protocol like endpoint")
                try {
                    val likeResult = atProtocolRepository.likePost(postUri)
                    if (likeResult != isCurrentlyLiked) {
                        // Update local state only after successful AT Protocol operation
                        _likedPosts.value = if (likeResult) {
                            currentLikes + postUri
                        } else {
                            currentLikes - postUri
                        }
                        Log.d(TAG, "✅ [TRENDS] Local state updated after successful AT Protocol operation")
                        
                        // Update Firebase to match AT Protocol state
                        Log.d(TAG, "💾 [TRENDS] Syncing Firebase with AT Protocol state")
                        likesRef?.child(userId)?.child(postUri)?.setValue(likeResult)
                            ?.addOnSuccessListener {
                                Log.d(TAG, "✅ [TRENDS] Firebase synced with AT Protocol state")
                            }
                            ?.addOnFailureListener { e ->
                                Log.e(TAG, "❌ [TRENDS] Failed to sync Firebase: ${e.message}")
                            }
                    } else {
                        Log.d(TAG, "ℹ️ [TRENDS] No state change needed - AT Protocol matches current state")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [TRENDS] AT Protocol operation failed: ${e.message}")
                    // Don't update local state if AT Protocol operation fails
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [TRENDS] Error in toggleLike: ${e.message}")
                Log.e(TAG, "❌ [TRENDS] Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    private fun loadInitialLikeStates(posts: List<FeedPost>) {
        viewModelScope.launch {
            try {
                println("DEBUG: TrendFlick 💾 [TRENDS] Loading initial like states for ${posts.size} posts")
                
                // Keep existing likes to prevent UI flicker
                val currentLiked = _likedPosts.value.toMutableSet()
                println("DEBUG: TrendFlick 💾 [TRENDS] Current likes in memory before loading: ${currentLiked.size}")
                
                // Check each post's like status
                posts.forEach { feedPost ->
                    try {
                        println("DEBUG: TrendFlick 🔍 [TRENDS] Checking like status for post: ${feedPost.post.uri}")
                        if (atProtocolRepository.isPostLikedByUser(feedPost.post.uri)) {
                            currentLiked.add(feedPost.post.uri)
                            println("DEBUG: TrendFlick ✅ [TRENDS] Post ${feedPost.post.uri} is liked")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: TrendFlick ❌ [TRENDS] Failed to check like status for post ${feedPost.post.uri}: ${e.message}")
                    }
                }
                
                // Update the state
                _likedPosts.value = currentLiked
                println("DEBUG: TrendFlick 💾 [TRENDS] Updated liked posts set, total liked: ${currentLiked.size}")
                println("DEBUG: TrendFlick 💾 [TRENDS] Liked posts URIs: ${currentLiked.joinToString(limit = 5)}")
            } catch (e: Exception) {
                println("DEBUG: TrendFlick ❌ [TRENDS] Failed to load initial like states: ${e.message}")
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
                    // Extract posts from the thread response
                    val threadPosts = threadResponse.thread.replies?.map { reply ->
                        FeedPost(post = reply.post)
                    } ?: emptyList()
                    
                    // Update threads with the new posts
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
                    
                    // Update like states for the thread and its replies
                    val currentLiked = _likedPosts.value.toMutableSet()
                    
                    // Check main post
                    if (atProtocolRepository.isPostLikedByUser(response.thread.post.uri)) {
                        currentLiked.add(response.thread.post.uri)
                    }
                    
                    // Check replies if they exist
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
                atProtocolRepository.repost(uri)
            } catch (e: Exception) {
                System.err.println("Failed to repost: ${e.message}")
            }
        }
    }

    fun sharePost(uri: String) {
        // Implement sharing functionality
    }

    private val _shareEvent = MutableSharedFlow<Intent>()
    val shareEvent = _shareEvent.asSharedFlow()

    // Function to toggle comments visibility
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
                    
                    // Store the original post's author DID
                    val originalAuthorDid = response.thread.post.author.did
                    println("DEBUG: TrendFlick - Original poster DID: $originalAuthorDid")
                    
                    // Process all replies
                    val comments = mutableListOf<ThreadPost>()
                    
                    // First add the original post
                    comments.add(response.thread)
                    
                    // Then process replies
                    response.thread.replies?.forEach { reply ->
                        // Add debug logging
                        println("DEBUG: TrendFlick - Reply from: ${reply.post.author.did}, isOP: ${reply.post.author.did == originalAuthorDid}")
                        comments.add(reply)
                        
                        // Process nested replies
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

    // Function to post a new comment
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
                        // Give network time to propagate
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
        // If this is a reply to a reply, use the original post as root
        // Otherwise, use the parent as root
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
                    // Refresh the thread after successful reply
                    handleThreadFetch(parentUri)
                }.onFailure { error ->
                    _threads.value = emptyList()
                }
            } catch (e: Exception) {
                _threads.value = emptyList()
            }
        }
    }

    // Update selectedFeed value
    fun updateSelectedFeed(feed: String) {
        viewModelScope.launch {
            _selectedFeed.value = feed
            savedStateHandle.set("selectedFeed", feed)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                
                // First clean up test videos
                if (videoRepository is VideoRepositoryImpl) {
                    Log.d(TAG, "🧹 Starting cleanup of test videos...")
                    (videoRepository as VideoRepositoryImpl).cleanupTestVideos()
                }
                
                // Refresh videos
                val refreshedVideos = videoRepository.getVideos()
                _videos.value = refreshedVideos
                
                // Refresh likes states
                refreshLikeStates()
                
                // Verify credentials and session
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
                Log.d(TAG, "🔄 FEED: Starting refresh")
                _isLoadingVideos.value = true
                _videoLoadError.value = null
                
                val videos = videoRepository.getVideos()
                Log.d(TAG, """
                    📱 FEED UPDATE:
                    Total videos: ${videos.size}
                    Videos: ${videos.map { it.uri }}
                """.trimIndent())
                
                _videos.value = videos
                
                if (videos.isEmpty()) {
                    Log.w(TAG, "⚠️ FEED: Empty - no videos found")
                    _videoLoadError.value = "No videos found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ FEED ERROR: ${e.message}")
                Log.e(TAG, "❌ STACK: ${e.stackTraceToString()}")
                _videoLoadError.value = "Failed to refresh videos: ${e.message}"
            } finally {
                _isLoadingVideos.value = false
            }
        }
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
                
                // Save credentials
                credentialsManager.saveCredentials(handle, password)
                
                // Create initial session
                if (credentialsManager.hasValidCredentials()) {
                    Log.d(TAG, "🔑 Creating initial session")
                    atProtocolRepository.createSession(handle, password)
                        .onSuccess { session ->
                            Log.d(TAG, """
                                ✅ Session created:
                                Handle: ${session.handle}
                                DID: ${session.did}
                            """.trimIndent())
                            
                            // Load initial data
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
                
                // Update selected feed to show hashtag
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
                
                // Load posts for hashtag
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
} 