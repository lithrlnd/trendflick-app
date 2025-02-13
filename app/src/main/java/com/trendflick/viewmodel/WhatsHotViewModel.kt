package com.trendflick.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Video
import com.trendflick.data.api.FeedPost
import com.trendflick.data.repository.AtProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class WhatsHotViewModel @Inject constructor(
    private val repository: AtProtocolRepository
) : ViewModel() {

    private val _threads = MutableStateFlow<List<FeedPost>>(emptyList())
    val threads: StateFlow<List<FeedPost>> = _threads

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _likedPosts = MutableStateFlow<Set<String>>(emptySet())
    val likedPosts: StateFlow<Set<String>> = _likedPosts

    private val _repostedPosts = MutableStateFlow<Set<String>>(emptySet())
    val repostedPosts: StateFlow<Set<String>> = _repostedPosts

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos

    private val _videoLoadError = MutableStateFlow<String?>(null)
    val videoLoadError: StateFlow<String?> = _videoLoadError

    private val _showComments = MutableStateFlow(false)
    val showComments: StateFlow<Boolean> = _showComments

    init {
        filterByCategory("whats-hot")
    }

    fun filterByCategory(category: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.getTimeline(algorithm = category, limit = 50)
                result.onSuccess { response ->
                    _threads.value = response.feed
                    // Update liked and reposted states
                    updateEngagementStates(response.feed)
                }.onFailure { error ->
                    Log.e("WhatsHotVM", "Failed to load timeline: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("WhatsHotVM", "Error loading timeline: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshVideoFeed() {
        viewModelScope.launch {
            try {
                _isLoadingVideos.value = true
                _videoLoadError.value = null
                val mediaVideos = repository.getMediaPosts()
                _videos.value = mediaVideos
            } catch (e: Exception) {
                Log.e("WhatsHotVM", "Failed to load videos: ${e.message}")
                _videoLoadError.value = "Failed to load videos: ${e.message}"
            } finally {
                _isLoadingVideos.value = false
            }
        }
    }

    private fun updateEngagementStates(posts: List<FeedPost>) {
        viewModelScope.launch {
            val likedUris = mutableSetOf<String>()
            val repostedUris = mutableSetOf<String>()
            
            posts.forEach { feedPost ->
                try {
                    if (repository.isPostLikedByUser(feedPost.post.uri)) {
                        likedUris.add(feedPost.post.uri)
                    }
                    if (repository.isPostRepostedByUser(feedPost.post.uri)) {
                        repostedUris.add(feedPost.post.uri)
                    }
                } catch (e: Exception) {
                    Log.e("WhatsHotVM", "Error checking engagement state: ${e.message}")
                }
            }
            
            _likedPosts.value = likedUris
            _repostedPosts.value = repostedUris
        }
    }

    fun toggleLike(uri: String) {
        viewModelScope.launch {
            try {
                val isLiked = repository.likePost(uri)
                _likedPosts.value = if (isLiked) {
                    _likedPosts.value + uri
                } else {
                    _likedPosts.value - uri
                }
            } catch (e: Exception) {
                Log.e("WhatsHotVM", "Error toggling like: ${e.message}")
            }
        }
    }

    fun repost(uri: String) {
        viewModelScope.launch {
            try {
                val post = _threads.value.find { it.post.uri == uri }?.post
                if (post != null) {
                    repository.repost(post.uri, post.cid)
                    _repostedPosts.value = _repostedPosts.value + uri
                }
            } catch (e: Exception) {
                Log.e("WhatsHotVM", "Error reposting: ${e.message}")
            }
        }
    }

    fun sharePost(uri: String) {
        // TODO: Implement share functionality using Android's share intent
    }

    fun loadThread(uri: String) {
        viewModelScope.launch {
            try {
                val result = repository.getPostThread(uri)
                // TODO: Update UI state with thread result
            } catch (e: Exception) {
                Log.e("WhatsHotVM", "Error loading thread: ${e.message}")
            }
        }
    }

    fun loadComments(uri: String) {
        viewModelScope.launch {
            try {
                val result = repository.getPostThread(uri)
                // TODO: Update UI state with comments
                _showComments.value = true
            } catch (e: Exception) {
                Log.e("WhatsHotVM", "Error loading comments: ${e.message}")
            }
        }
    }

    fun toggleComments(show: Boolean) {
        _showComments.value = show
    }

    fun onHashtagSelected(tag: String) {
        // TODO: Implement hashtag navigation
    }
} 
