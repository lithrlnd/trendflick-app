package com.trendflick.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Post
import com.trendflick.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsHotViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadTrendingPosts()
    }
    
    private fun loadTrendingPosts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Use AT Protocol to fetch trending posts
                val trendingPosts = postRepository.getTrendingPosts()
                _posts.value = trendingPosts
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun onPostLike(post: Post) {
        viewModelScope.launch {
            try {
                postRepository.likePost(post.id)
                // Update local state
                _posts.value = _posts.value.map { 
                    if (it.id == post.id) it.copy(isLiked = !it.isLiked) else it 
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun onPostComment(post: Post) {
        viewModelScope.launch {
            // Navigate to comment screen or show comment sheet
        }
    }
    
    fun onPostShare(post: Post) {
        viewModelScope.launch {
            // Handle share action
        }
    }
    
    fun refresh() {
        loadTrendingPosts()
    }
} 