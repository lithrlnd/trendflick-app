package com.trendflick.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Post
import com.trendflick.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val sharedViewModel: SharedViewModel
) : ViewModel() {

    // Posts in the feed
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Selected feed (Trends or Flicks)
    val selectedFeed = sharedViewModel.selectedFeed
    
    init {
        loadPosts()
    }
    
    /**
     * Load posts for the feed
     */
    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val posts = feedRepository.getFeedPosts()
                _posts.value = posts
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update selected feed
     */
    fun updateSelectedFeed(feed: String) {
        sharedViewModel.updateSelectedFeed(feed)
    }
    
    /**
     * Toggle like on a post
     */
    fun toggleLikePost(postId: String) {
        viewModelScope.launch {
            try {
                val updatedPosts = _posts.value.map { post ->
                    if (post.id == postId) {
                        val newLikeCount = if (post.isLiked) post.likes - 1 else post.likes + 1
                        post.copy(isLiked = !post.isLiked, likes = newLikeCount)
                    } else {
                        post
                    }
                }
                _posts.value = updatedPosts
                
                // Update in repository
                feedRepository.toggleLikePost(postId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Repost a post
     */
    fun repostPost(postId: String) {
        viewModelScope.launch {
            try {
                val updatedPosts = _posts.value.map { post ->
                    if (post.id == postId) {
                        val newRepostCount = if (post.isReposted) post.reposts - 1 else post.reposts + 1
                        post.copy(isReposted = !post.isReposted, reposts = newRepostCount)
                    } else {
                        post
                    }
                }
                _posts.value = updatedPosts
                
                // Update in repository
                feedRepository.repostPost(postId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Share a post
     */
    fun sharePost(postId: String) {
        viewModelScope.launch {
            try {
                feedRepository.sharePost(postId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Load more posts (pagination)
     */
    fun loadMorePosts() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentPosts = _posts.value
                val morePosts = feedRepository.getMorePosts(currentPosts.size)
                _posts.value = currentPosts + morePosts
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh feed
     */
    fun refreshFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val posts = feedRepository.refreshFeed()
                _posts.value = posts
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
