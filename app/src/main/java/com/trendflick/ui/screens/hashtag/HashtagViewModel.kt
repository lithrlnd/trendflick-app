package com.trendflick.ui.screens.hashtag

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

@HiltViewModel
class HashtagViewModel @Inject constructor(
    private val repository: AtProtocolRepository
) : ViewModel() {
    private val _posts = MutableStateFlow<List<FeedPost>>(emptyList())
    val posts: StateFlow<List<FeedPost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _relatedHashtags = MutableStateFlow<List<String>>(emptyList())
    val relatedHashtags: StateFlow<List<String>> = _relatedHashtags.asStateFlow()

    private val _postCount = MutableStateFlow(0)
    val postCount: StateFlow<Int> = _postCount.asStateFlow()

    private val _engagementRate = MutableStateFlow(0.0)
    val engagementRate: StateFlow<Double> = _engagementRate.asStateFlow()

    fun loadHashtagData(hashtag: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getPostsByHashtag(hashtag)
                result.onSuccess { response ->
                    val posts = response.feed
                    _posts.value = posts
                    _postCount.value = posts.size
                    
                    val totalEngagements = posts.sumOf { post ->
                        (post.post.likeCount ?: 0) + 
                        (post.post.repostCount ?: 0) + 
                        (post.post.replyCount ?: 0)
                    }
                    
                    _engagementRate.value = if (posts.isNotEmpty()) {
                        (totalEngagements.toDouble() / posts.size) * 100
                    } else 0.0
                }

                // Check if user is following the hashtag
                _isFollowing.value = repository.checkHashtagFollowStatus(hashtag)
                
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFollow(hashtag: String) {
        viewModelScope.launch {
            try {
                if (_isFollowing.value) {
                    repository.unfollowHashtag(hashtag)
                } else {
                    repository.followHashtag(hashtag)
                }
                _isFollowing.value = !_isFollowing.value
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
} 