package com.trendflick.ui.screens.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.api.FeedPost
import com.trendflick.data.repository.AtProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.trendflick.data.auth.BlueskyCredentialsManager

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

    private var currentCursor: String? = null
    private var loadingJob: Job? = null
    private var isLoggedOut = false

    init {
        Log.d(TAG, "🚀 ViewModel initialized")
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚀 Starting FollowingViewModel initialization")
                
                // Step 1: Check credentials and create session
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
                Log.e(TAG, "❌ FollowingViewModel initialization failed: ${e.message}")
                isLoggedOut = true
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
            
            // If no valid session, try to create one
            Log.d(TAG, "🔍 No valid session found, attempting to create new session")
            
            atProtocolRepository.createSession(handle, password)
                .onSuccess { 
                    Log.d(TAG, "✅ Successfully created new session for handle: $handle")
                    isLoggedOut = false
                    return true
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Failed to create session: ${e.message}")
                    if (e.message?.contains("Unauthorized") == true || 
                        e.message?.contains("Invalid credentials") == true ||
                        e.message?.contains("Not authenticated") == true) {
                        Log.d(TAG, "🔒 Setting logged out state due to auth error")
                        isLoggedOut = true
                        credentialsManager.clearCredentials()
                    }
                    return false
                }
            
            return false
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

    fun loadThreads(refresh: Boolean = false) {
        if (_isLoading.value && !refresh) {
            Log.d(TAG, "⏳ Already loading threads, skipping request")
            return
        }

        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            try {
                Log.d(TAG, """
                    🚀 Starting timeline load:
                    • Refresh mode: $refresh
                    • Current cursor: ${currentCursor ?: "null"}
                    • Current thread count: ${_threads.value.size}
                    • Loading state: ${_isLoading.value}
                    • Refresh state: ${_isRefreshing.value}
                """.trimIndent())

                if (refresh) {
                    _isRefreshing.value = true
                    currentCursor = null
                } else {
                    _isLoading.value = true
                }

                // Ensure session is valid before making request
                if (!ensureValidSession()) {
                    Log.e(TAG, "❌ Session validation failed, aborting timeline load")
                    return@launch
                }

                val currentUser = atProtocolRepository.getCurrentSession()
                if (currentUser == null) {
                    Log.e(TAG, "❌ No current user found, aborting timeline load")
                    return@launch
                }

                // Add delay to ensure session is properly established
                delay(500)

                Log.d(TAG, """
                    📡 Making timeline request:
                    • User: ${currentUser.handle}
                    • Feed: following
                    • Limit: 50
                    • Cursor: ${currentCursor ?: "null"}
                """.trimIndent())

                val result = atProtocolRepository.getTimeline(
                    algorithm = "reverse-chronological",
                    cursor = if (refresh) null else currentCursor,
                    limit = 50
                )

                result.onSuccess { response ->
                    Log.d(TAG, """
                        ✅ Timeline fetch successful - Raw feed size: ${response.feed.size}
                        • First post URI: ${response.feed.firstOrNull()?.post?.uri}
                        • First post text: ${response.feed.firstOrNull()?.post?.record?.text?.take(50)}
                        • First post author: ${response.feed.firstOrNull()?.post?.author?.handle}
                    """.trimIndent())
                    
                    currentCursor = response.cursor
                    
                    val filteredPosts = response.feed.filter { feedPost ->
                        feedPost.post.uri.isNotEmpty() && feedPost.post.cid.isNotEmpty()
                    }
                    
                    val newThreads = if (refresh) {
                        filteredPosts
                    } else {
                        _threads.value + filteredPosts
                    }
                    
                    if (newThreads.isNotEmpty()) {
                        Log.d(TAG, """
                            📱 Thread Update Success:
                            Total threads: ${newThreads.size}
                            New posts: ${filteredPosts.size}
                            Previous posts: ${_threads.value.size}
                            Refresh: $refresh
                            Cursor: $currentCursor
                        """.trimIndent())
                        
                        _threads.value = newThreads
                    } else {
                        Log.w(TAG, "⚠️ No valid threads in response after filtering")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Timeline load failed: ${error.message}")
                    if (error is retrofit2.HttpException) {
                        Log.e(TAG, "HTTP Error Code: ${error.code()}")
                        // Handle specific HTTP errors
                        when (error.code()) {
                            401 -> {
                                // Session expired, trigger reauth
                                isLoggedOut = true
                            }
                            429 -> {
                                // Rate limited, wait and retry
                                delay(5000)
                                loadMoreThreads(refresh = refresh)
                            }
                        }
                    }
                    error.printStackTrace()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in loadThreads: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
                Log.d(TAG, """
                    🏁 Timeline load complete:
                    • Final thread count: ${_threads.value.size}
                    • Has cursor: ${currentCursor != null}
                """.trimIndent())
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
                atProtocolRepository.repost(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to repost: ${e.message}")
            }
        }
    }

    private fun loadInitialLikeStates(posts: List<FeedPost>) {
        viewModelScope.launch {
            try {
                val currentLiked = _likedPosts.value.toMutableSet()
                
                posts.forEach { feedPost ->
                    try {
                        if (atProtocolRepository.isPostLikedByUser(feedPost.post.uri)) {
                            currentLiked.add(feedPost.post.uri)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to check like status: ${e.message}")
                    }
                }
                
                _likedPosts.value = currentLiked
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load initial like states: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "FollowingViewModel"
    }
} 
