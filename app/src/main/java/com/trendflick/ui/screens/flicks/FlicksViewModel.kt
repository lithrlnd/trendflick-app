package com.trendflick.ui.screens.flicks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Video
import com.trendflick.data.repository.VideoRepository
import com.trendflick.data.repository.VideoRepositoryImpl
import com.trendflick.data.repository.BlueskyRepository
import com.trendflick.data.repository.AtProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlicksViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val blueskyRepository: BlueskyRepository,
    private val atProtocolRepository: AtProtocolRepository
) : ViewModel() {

    private val TAG = "TF_FlicksViewModel"
    
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentCursor: String? = null
    private var isLoadingMore = false
    private var hasInitiallyLoaded = false

    init {
        Log.d(TAG, "🔄 ViewModel initialized - attempting initial load")
        loadMediaPosts()
    }

    fun loadMediaPosts() {
        if (_isLoading.value) {
            Log.d(TAG, "⚠️ Skipping load - already loading")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(TAG, """
                    🔄 Starting loadMediaPosts:
                    Current Videos: ${_videos.value.size}
                    Loading State: ${_isLoading.value}
                    Has Error: ${_error.value != null}
                """.trimIndent())
                
                _isLoading.value = true
                _error.value = null

                val posts = blueskyRepository.getMediaPosts(currentCursor)
                
                Log.d(TAG, """
                    📊 Media Posts Load Results:
                    Posts Received: ${posts.size}
                    Images: ${posts.count { it.isImage }}
                    Videos: ${posts.count { !it.isImage }}
                    First Post Type: ${posts.firstOrNull()?.let { if (it.isImage) "Image" else "Video" }}
                """.trimIndent())

                if (currentCursor == null) {
                    _videos.value = posts
                } else {
                    _videos.value = _videos.value + posts
                }

                if (posts.isEmpty() && _videos.value.isEmpty()) {
                    _error.value = "No media posts found. Pull down to refresh or try again later."
                    Log.w(TAG, "⚠️ No media posts found in feed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading media posts: ${e.message}", e)
                _error.value = "Failed to load media posts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || _isLoading.value) {
            Log.d(TAG, "⚠️ Skipping loadMore - already loading")
            return
        }
        
        viewModelScope.launch {
            try {
                isLoadingMore = true
                Log.d(TAG, "🔄 Loading more posts with cursor: $currentCursor")
                
                val posts = blueskyRepository.getMediaPosts(currentCursor)
                
                Log.d(TAG, """
                    📊 Load More Results:
                    Additional Posts: ${posts.size}
                    New Total: ${_videos.value.size + posts.size}
                """.trimIndent())
                
                _videos.value = _videos.value + posts
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading more posts: ${e.message}", e)
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun refresh() {
        Log.d(TAG, """
            🔄 Refresh triggered:
            Current Videos: ${_videos.value.size}
            Loading State: ${_isLoading.value}
            Has Error: ${_error.value != null}
        """.trimIndent())
        
        currentCursor = null
        _videos.value = emptyList()
        loadMediaPosts()
    }

    fun loadVideos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔄 Starting video load...")
                
                // Get videos from repository
                val loadedVideos = videoRepository.getVideos()
                
                Log.d(TAG, """
                    📊 Video Load Results:
                    Total Videos: ${loadedVideos.size}
                    Has Videos: ${loadedVideos.isNotEmpty()}
                    First Video URL: ${loadedVideos.firstOrNull()?.videoUrl}
                """.trimIndent())
                
                // Update UI state
                _videos.value = loadedVideos
                
                if (loadedVideos.isEmpty()) {
                    Log.w(TAG, "⚠️ No videos found in feed")
                } else {
                    Log.d(TAG, """
                        ✅ Videos loaded successfully:
                        ${loadedVideos.joinToString("\n") { 
                            "🎥 ${it.uri}: ${it.videoUrl}"
                        }}
                    """.trimIndent())
                }
                
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ Error loading videos:
                    Error: ${e.message}
                    Stack: ${e.stackTraceToString()}
                """.trimIndent())
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun testVideoInFolder() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                (videoRepository as VideoRepositoryImpl).testVideoInFolder()
                loadVideos() // Refresh the feed after test
            } catch (e: Exception) {
                Log.e(TAG, "Test video error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun testFolderAccess() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                (videoRepository as VideoRepositoryImpl).testFolderAccess()
            } catch (e: Exception) {
                Log.e(TAG, "Test folder error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun testSmallFileUpload() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🧪 Starting small file upload test")
                (videoRepository as VideoRepositoryImpl).testSmallFileUpload()
                Log.d(TAG, "✅ Small file test complete")
                loadVideos() // Refresh the feed after test
            } catch (e: Exception) {
                Log.e(TAG, "❌ Small file test failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun testBlueskyPost() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔄 Testing Bluesky post...")
                
                // First ensure we have a valid session
                val sessionValid = atProtocolRepository.ensureValidSession()
                
                if (sessionValid) {
                    Log.d(TAG, "✅ Session valid")
                    // Add your test post logic here if needed
                } else {
                    Log.e(TAG, "❌ No valid session")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Test post failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
} 