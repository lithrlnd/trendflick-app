package com.trendflick.ui.screens.flicks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Comment
import com.trendflick.data.model.Video
import com.trendflick.data.repository.VideoRepository
import com.trendflick.data.repository.VideoRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class FlicksViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val TAG = "TF_FlicksViewModel"
    
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _likedVideos = MutableStateFlow<Set<String>>(emptySet())
    private val _likedComments = MutableStateFlow<Set<String>>(emptySet())

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔄 Starting video load...")
                
                // Get videos from repository - filter to only include videos
                val allMedia = videoRepository.getVideos()
                val videoOnly = allMedia.filter { 
                    it.videoUrl.isNotEmpty() && 
                    (it.videoUrl.endsWith(".mp4", ignoreCase = true) || 
                     it.videoUrl.endsWith(".mov", ignoreCase = true) ||
                     it.videoUrl.endsWith(".webm", ignoreCase = true) ||
                     it.videoUrl.contains("video", ignoreCase = true))
                }
                
                Log.d(TAG, """
                    📊 Video Load Results:
                    Total Media: ${allMedia.size}
                    Video Only: ${videoOnly.size}
                    Has Videos: ${videoOnly.isNotEmpty()}
                    First Video URL: ${videoOnly.firstOrNull()?.videoUrl}
                """.trimIndent())
                
                // Update UI state with video-only content
                _videos.value = videoOnly
                
                if (videoOnly.isEmpty()) {
                    Log.w(TAG, "⚠️ No videos found in feed")
                } else {
                    Log.d(TAG, """
                        ✅ Videos loaded successfully:
                        ${videoOnly.joinToString("\n") { 
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
    
    fun likeVideo(videoUri: String) {
        viewModelScope.launch {
            try {
                val currentLikedVideos = _likedVideos.value.toMutableSet()
                
                if (currentLikedVideos.contains(videoUri)) {
                    // Unlike video
                    currentLikedVideos.remove(videoUri)
                    // Update repository
                    videoRepository.unlikeVideo(videoUri)
                } else {
                    // Like video
                    currentLikedVideos.add(videoUri)
                    // Update repository
                    videoRepository.likeVideo(videoUri)
                }
                
                _likedVideos.value = currentLikedVideos
                
                // Update the videos list to reflect like status
                val updatedVideos = _videos.value.map { video ->
                    if (video.uri == videoUri) {
                        video.copy(
                            isLiked = currentLikedVideos.contains(videoUri),
                            likes = if (currentLikedVideos.contains(videoUri)) video.likes + 1 else video.likes - 1
                        )
                    } else {
                        video
                    }
                }
                _videos.value = updatedVideos
                
            } catch (e: Exception) {
                Log.e(TAG, "Error liking video: ${e.message}")
            }
        }
    }
    
    fun shareVideo(videoUri: String) {
        viewModelScope.launch {
            try {
                // Increment share count locally
                val updatedVideos = _videos.value.map { video ->
                    if (video.uri == videoUri) {
                        video.copy(shares = video.shares + 1)
                    } else {
                        video
                    }
                }
                _videos.value = updatedVideos
                
                // Call repository to handle actual sharing
                videoRepository.shareVideo(videoUri)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing video: ${e.message}")
            }
        }
    }
    
    fun addComment(videoUri: String, commentText: String) {
        viewModelScope.launch {
            try {
                // Create a new comment
                val newComment = Comment(
                    id = UUID.randomUUID().toString(),
                    username = "You", // This would come from user profile
                    content = commentText,
                    timestamp = System.currentTimeMillis(),
                    likes = 0,
                    avatar = null // This would come from user profile
                )
                
                // Add comment to video
                val updatedVideos = _videos.value.map { video ->
                    if (video.uri == videoUri) {
                        val updatedComments = (video.comments ?: emptyList()) + newComment
                        video.copy(
                            comments = updatedComments,
                            comments = updatedComments.size
                        )
                    } else {
                        video
                    }
                }
                _videos.value = updatedVideos
                
                // Update repository
                videoRepository.addComment(videoUri, commentText)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error adding comment: ${e.message}")
            }
        }
    }
    
    fun likeComment(videoUri: String, commentId: String) {
        viewModelScope.launch {
            try {
                val currentLikedComments = _likedComments.value.toMutableSet()
                
                if (currentLikedComments.contains(commentId)) {
                    // Unlike comment
                    currentLikedComments.remove(commentId)
                } else {
                    // Like comment
                    currentLikedComments.add(commentId)
                }
                
                _likedComments.value = currentLikedComments
                
                // Update the videos list to reflect comment like status
                val updatedVideos = _videos.value.map { video ->
                    if (video.uri == videoUri) {
                        val updatedComments = video.comments?.map { comment ->
                            if (comment.id == commentId) {
                                comment.copy(
                                    likes = if (currentLikedComments.contains(commentId)) 
                                        comment.likes + 1 else comment.likes - 1
                                )
                            } else {
                                comment
                            }
                        }
                        video.copy(comments = updatedComments)
                    } else {
                        video
                    }
                }
                _videos.value = updatedVideos
                
                // Update repository
                videoRepository.likeComment(videoUri, commentId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error liking comment: ${e.message}")
            }
        }
    }
    
    fun replyToComment(videoUri: String, commentId: String) {
        // This would be implemented to handle replies to comments
        // For now, just log the action
        Log.d(TAG, "Reply to comment: $commentId on video: $videoUri")
    }
    
    // Helper function to determine if we're in debug mode
    fun isDebugMode(): Boolean {
        return true // This would be determined by build configuration
    }
}
