package com.trendflick.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Video
import com.trendflick.data.model.VideoCategory
import com.trendflick.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    fun commentOnVideo(videoId: Int) {
        // TODO: Implement comment functionality
    }

    fun shareVideo(videoId: Int) {
        // TODO: Implement share functionality
    }
} 