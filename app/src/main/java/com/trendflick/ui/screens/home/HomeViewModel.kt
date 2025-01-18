package com.trendflick.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Video
import com.trendflick.data.model.VideoCategory
import com.trendflick.data.model.Comment
import com.trendflick.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _likedVideos = MutableStateFlow<Set<Int>>(emptySet())
    val likedVideos: StateFlow<Set<Int>> = _likedVideos.asStateFlow()

    private val _comments = MutableStateFlow<Map<Int, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<Int, List<Comment>>> = _comments.asStateFlow()

    private val _likedComments = MutableStateFlow<Set<String>>(emptySet())
    val likedComments: StateFlow<Set<String>> = _likedComments.asStateFlow()

    // Store the default feed
    private var defaultFeed: List<Video> = emptyList()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            videoRepository.getAllVideos().collect { videos ->
                defaultFeed = videos  // Store the default feed
                _videos.value = videos
                _isLoading.value = false
            }
        }
    }

    fun filterByCategory(category: VideoCategory) {
        viewModelScope.launch {
            _isLoading.value = true
            when (category.id) {
                "fyp" -> {
                    // Return to the default feed
                    _videos.value = defaultFeed
                }
                "trending" -> {
                    videoRepository.getAllVideos().collect { videos ->
                        _videos.value = videos
                    }
                }
                else -> {
                    videoRepository.getAllVideos().collect { allVideos ->
                        _videos.value = allVideos.filter { video ->
                            video.hashtags.any { hashtag ->
                                hashtag.contains(category.id, ignoreCase = true)
                            }
                        }
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun likeVideo(videoId: Int) {
        viewModelScope.launch {
            videoRepository.likeVideo(videoId)
            _likedVideos.value = _likedVideos.value.toMutableSet().apply {
                if (contains(videoId)) remove(videoId) else add(videoId)
            }
        }
    }

    fun unlikeVideo(videoId: Int) {
        viewModelScope.launch {
            videoRepository.unlikeVideo(videoId)
            _likedVideos.value = _likedVideos.value.toMutableSet().apply {
                remove(videoId)
            }
        }
    }

    fun preloadVideo(videoUrl: String) {
        viewModelScope.launch {
            videoRepository.preloadVideo(videoUrl)
        }
    }

    fun preloadVideos(currentPage: Int, videos: List<Video>) {
        // Preload videos around the current page
        val preloadRange = (currentPage - 1)..(currentPage + 1)
        preloadRange.forEach { page ->
            if (page >= 0 && page < videos.size) {
                preloadVideo(videos[page].videoUrl)
            }
        }
    }

    fun commentOnVideo(videoId: Int, content: String) {
        viewModelScope.launch {
            val video = _videos.value.find { it.id == videoId } ?: return@launch
            val commentId = UUID.randomUUID().toString()
            val newComment = Comment(
                id = commentId,
                uri = "at://${video.userId}/app.bsky.feed.post/$commentId",
                userId = "current_user_id", // TODO: Get from auth
                username = "current_user", // TODO: Get from auth
                content = content,
                createdAt = System.currentTimeMillis(),
                indexedAt = java.time.Instant.now().toString(),
                replyCount = 0,
                likes = 0
            )
            
            val currentComments = _comments.value.getOrDefault(videoId, emptyList())
            _comments.value = _comments.value + mapOf(videoId to (currentComments + newComment))
        }
    }

    fun likeComment(commentId: String) {
        viewModelScope.launch {
            _likedComments.value = _likedComments.value.toMutableSet().apply {
                if (contains(commentId)) remove(commentId) else add(commentId)
            }
        }
    }

    fun replyToComment(videoId: Int, commentId: String) {
        viewModelScope.launch {
            val video = _videos.value.find { it.id == videoId } ?: return@launch
            val parentComment = _comments.value[videoId]?.find { it.id == commentId } ?: return@launch
            
            // Update reply count of parent comment
            val updatedParentComment = parentComment.copy(replyCount = parentComment.replyCount + 1)
            val currentComments = _comments.value.getOrDefault(videoId, emptyList())
            val updatedComments = currentComments.map { 
                if (it.id == commentId) updatedParentComment else it 
            }
            _comments.value = _comments.value + mapOf(videoId to updatedComments)
        }
    }

    fun shareVideo(videoId: Int) {
        viewModelScope.launch {
            val video = _videos.value.find { it.id == videoId } ?: return@launch
            val shareText = """
                Check out this video on TrendFlick!
                
                ${video.title}
                By @${video.username}
                
                ${video.videoUrl}
            """.trimIndent()

            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            _shareEvent.emit(intent)
        }
    }

    private val _shareEvent = MutableSharedFlow<Intent>()
    val shareEvent = _shareEvent.asSharedFlow()
} 