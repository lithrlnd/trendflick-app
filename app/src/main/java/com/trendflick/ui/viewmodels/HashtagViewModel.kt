package com.trendflick.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.UserSuggestion
import com.trendflick.data.repository.HashtagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * ViewModel for handling hashtag and mention suggestions
 */
@HiltViewModel
class HashtagViewModel @Inject constructor(
    private val hashtagRepository: HashtagRepository
) : ViewModel() {

    private val TAG = "TF_HashtagViewModel"

    // Hashtag suggestions
    private val _hashtagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val hashtagSuggestions: StateFlow<List<String>> = _hashtagSuggestions.asStateFlow()

    // User mention suggestions
    private val _userSuggestions = MutableStateFlow<List<UserSuggestion>>(emptyList())
    val userSuggestions: StateFlow<List<UserSuggestion>> = _userSuggestions.asStateFlow()

    // Loading states
    private val _isLoadingHashtags = MutableStateFlow(false)
    val isLoadingHashtags: StateFlow<Boolean> = _isLoadingHashtags.asStateFlow()

    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    /**
     * Get hashtag suggestions based on query
     */
    fun getHashtagSuggestions(query: String) {
        if (query.isEmpty()) {
            _hashtagSuggestions.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _isLoadingHashtags.value = true
                Log.d(TAG, "🔍 Fetching hashtag suggestions for: $query")
                
                val suggestions = hashtagRepository.getHashtagSuggestions(query)
                _hashtagSuggestions.value = suggestions
                
                Log.d(TAG, "✅ Received ${suggestions.size} hashtag suggestions")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching hashtag suggestions: ${e.message}", e)
                _hashtagSuggestions.value = emptyList()
            } finally {
                _isLoadingHashtags.value = false
            }
        }
    }

    /**
     * Get user mention suggestions based on query
     */
    fun getUserSuggestions(query: String) {
        if (query.isEmpty()) {
            _userSuggestions.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _isLoadingUsers.value = true
                Log.d(TAG, "🔍 Fetching user suggestions for: $query")
                
                val suggestions = hashtagRepository.getUserSuggestions(query)
                _userSuggestions.value = suggestions
                
                Log.d(TAG, "✅ Received ${suggestions.size} user suggestions")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching user suggestions: ${e.message}", e)
                _userSuggestions.value = emptyList()
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }

    /**
     * Track hashtag usage for analytics
     */
    fun trackHashtagUsage(hashtag: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📊 Tracking hashtag usage: #$hashtag")
                hashtagRepository.trackHashtagUsage(hashtag)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error tracking hashtag usage: ${e.message}", e)
            }
        }
    }

    /**
     * Get trending hashtags
     */
    fun getTrendingHashtags() {
        viewModelScope.launch {
            try {
                _isLoadingHashtags.value = true
                Log.d(TAG, "🔍 Fetching trending hashtags")
                
                val trending = hashtagRepository.getTrendingHashtags()
                _hashtagSuggestions.value = trending
                
                Log.d(TAG, "✅ Received ${trending.size} trending hashtags")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching trending hashtags: ${e.message}", e)
                _hashtagSuggestions.value = emptyList()
            } finally {
                _isLoadingHashtags.value = false
            }
        }
    }

    /**
     * Get recently used hashtags
     */
    fun getRecentHashtags() {
        viewModelScope.launch {
            try {
                _isLoadingHashtags.value = true
                Log.d(TAG, "🔍 Fetching recent hashtags")
                
                val recent = hashtagRepository.getRecentHashtags()
                _hashtagSuggestions.value = recent
                
                Log.d(TAG, "✅ Received ${recent.size} recent hashtags")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching recent hashtags: ${e.message}", e)
                _hashtagSuggestions.value = emptyList()
            } finally {
                _isLoadingHashtags.value = false
            }
        }
    }

    /**
     * Get frequently mentioned users
     */
    fun getFrequentlyMentionedUsers() {
        viewModelScope.launch {
            try {
                _isLoadingUsers.value = true
                Log.d(TAG, "🔍 Fetching frequently mentioned users")
                
                val frequent = hashtagRepository.getFrequentlyMentionedUsers()
                _userSuggestions.value = frequent
                
                Log.d(TAG, "✅ Received ${frequent.size} frequently mentioned users")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching frequently mentioned users: ${e.message}", e)
                _userSuggestions.value = emptyList()
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }
}
