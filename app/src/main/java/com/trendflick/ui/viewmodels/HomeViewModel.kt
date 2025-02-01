package com.trendflick.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.trendflick.data.repository.AtProtocolRepository
import javax.inject.Inject
import com.trendflick.data.api.ThreadPost
import com.trendflick.data.api.FeedPost
import com.trendflick.data.model.categories
import android.util.Log

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val atProtocolRepository: AtProtocolRepository
) : ViewModel() {

    private val _selectedFeed = MutableStateFlow("Trends")
    val selectedFeed: StateFlow<String> = _selectedFeed.asStateFlow()

    private val _currentCategory = MutableStateFlow("")
    val currentCategory: StateFlow<String> = _currentCategory.asStateFlow()

    private val _currentHashtag = MutableStateFlow("")
    val currentHashtag: StateFlow<String> = _currentHashtag.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _trendingHashtags = MutableStateFlow<List<String>>(emptyList())
    val trendingHashtags: StateFlow<List<String>> = _trendingHashtags.asStateFlow()

    private val _threads = MutableStateFlow<List<ThreadPost>>(emptyList())
    val threads: StateFlow<List<ThreadPost>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _likedPosts = MutableStateFlow<Set<String>>(emptySet())
    val likedPosts: StateFlow<Set<String>> = _likedPosts.asStateFlow()

    private val _repostedPosts = MutableStateFlow<Set<String>>(emptySet())
    val repostedPosts: StateFlow<Set<String>> = _repostedPosts.asStateFlow()

    private val _showAuthorOnly = MutableStateFlow(false)
    val showAuthorOnly: StateFlow<Boolean> = _showAuthorOnly.asStateFlow()

    fun updateSelectedFeed(feed: String) {
        _selectedFeed.value = feed
        if (feed == "Trends") {
            loadPosts()
        }
    }

    fun openDrawer() {
        _isDrawerOpen.value = true
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun filterByCategory(category: String) {
        _currentCategory.value = category
        _selectedFeed.value = category // Update the selected feed to show the category name
        closeDrawer()
        loadPosts()
    }

    fun onHashtagSelected(hashtag: String) {
        _currentHashtag.value = hashtag
        closeDrawer()
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (_currentCategory.value.isNotEmpty()) {
                    // Get hashtags for the selected category
                    val categoryHashtags = categories.find { category -> 
                        category.name.equals(_currentCategory.value, ignoreCase = true) 
                    }?.hashtags
                    
                    if (!categoryHashtags.isNullOrEmpty()) {
                        // Use the first hashtag as primary search
                        val result = atProtocolRepository.getPostsByHashtag(
                            hashtag = categoryHashtags.first(),
                            limit = 100
                        )
                        
                        result.onSuccess { response ->
                            val filteredPosts = response.feed.filter { post ->
                                categoryHashtags.any { tag ->
                                    post.post.record.text.lowercase().contains(tag.lowercase())
                                }
                            }
                            _threads.value = filteredPosts.map { feedPost ->
                                ThreadPost(
                                    post = feedPost.post,
                                    parent = null,
                                    replies = emptyList()
                                )
                            }.take(50)
                        }.onFailure { error ->
                            _error.value = error.message
                        }
                    }
                } else if (_currentHashtag.value.isNotEmpty()) {
                    // Load posts for a specific hashtag
                    val result = atProtocolRepository.getPostsByHashtag(
                        hashtag = _currentHashtag.value,
                        limit = 50
                    )
                    result.onSuccess { response ->
                        _threads.value = response.feed.map { feedPost ->
                            ThreadPost(
                                post = feedPost.post,
                                parent = null,
                                replies = emptyList()
                            )
                        }
                    }.onFailure { error ->
                        _error.value = error.message
                    }
                } else {
                    // Load general timeline/trends
                    val result = atProtocolRepository.getTimeline(
                        algorithm = "whats-hot",
                        limit = 50
                    )
                    result.onSuccess { response ->
                        _threads.value = response.feed.map { feedPost ->
                            ThreadPost(
                                post = feedPost.post,
                                parent = null,
                                replies = emptyList()
                            )
                        }
                    }.onFailure { error ->
                        _error.value = error.message
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    init {
        loadTrendingHashtags()
        loadPosts()
    }

    private fun loadTrendingHashtags() {
        viewModelScope.launch {
            try {
                val hashtags = atProtocolRepository.getTrendingHashtags()
                _trendingHashtags.value = hashtags.map { it.tag }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading trending hashtags: ${e.message}")
            }
        }
    }

    fun toggleLike(postUri: String) {
        viewModelScope.launch {
            try {
                val isLiked = atProtocolRepository.likePost(postUri)
                if (isLiked) {
                    _likedPosts.value = _likedPosts.value + postUri
                } else {
                    _likedPosts.value = _likedPosts.value - postUri
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error toggling like: ${e.message}")
            }
        }
    }

    fun repost(postUri: String) {
        viewModelScope.launch {
            try {
                val threadResult = atProtocolRepository.getPostThread(postUri)
                threadResult.onSuccess { response ->
                    atProtocolRepository.repost(response.thread.post.uri, response.thread.post.cid)
                    _repostedPosts.value = _repostedPosts.value + postUri
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error reposting: ${e.message}")
            }
        }
    }

    fun loadThread(postUri: String) {
        viewModelScope.launch {
            try {
                val result = atProtocolRepository.getPostThread(postUri)
                result.onSuccess { response ->
                    _threads.value = listOf(response.thread)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading thread: ${e.message}")
            }
        }
    }
} 