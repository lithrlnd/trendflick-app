package com.trendflick.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Post
import com.trendflick.data.repository.BlueskyPostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsHotViewModel @Inject constructor(
    private val repository: BlueskyPostRepository
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val trendingPosts = repository.getTrendingPosts()
                _posts.value = trendingPosts
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun likePost(post: Post) {
        viewModelScope.launch {
            try {
                repository.likePost(post.id)
                refreshPost(post.id)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun commentPost(post: Post) {
        viewModelScope.launch {
            try {
                // Navigate to comment screen or show comment dialog
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun sharePost(post: Post) {
        viewModelScope.launch {
            try {
                // Implement share functionality
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun repostPost(post: Post) {
        viewModelScope.launch {
            try {
                repository.repostPost(post.id)
                refreshPost(post.id)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun savePost(post: Post) {
        viewModelScope.launch {
            try {
                // Implement save functionality
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun handleHashtagClick(tag: String) {
        viewModelScope.launch {
            try {
                // Implement hashtag navigation or filtering
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun refreshPost(postId: String) {
        viewModelScope.launch {
            try {
                // Refresh the specific post
                val updatedPosts = _posts.value.map { post ->
                    if (post.id == postId) {
                        // Get updated post from repository
                        post // TODO: Replace with actual updated post
                    } else {
                        post
                    }
                }
                _posts.value = updatedPosts
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
} 