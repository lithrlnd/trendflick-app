package com.trendflick.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Post
import com.trendflick.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * ViewModel for the profile screen
 */
@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val TAG = "TF_ProfileViewModel"

    // User profile data
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Follow state
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    /**
     * Load user profile data
     */
    fun loadUserProfile(username: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔍 Loading profile for user: ${username ?: "current user"}")
                
                // Simulate network delay
                delay(1000)
                
                // Create mock user profile
                val handle = username ?: "currentuser"
                _userProfile.value = UserProfile(
                    handle = handle,
                    displayName = if (username == null) "Current User" else "${handle.capitalize()} User",
                    bio = "This is a bio for $handle. #trendflick #bluesky",
                    avatarUrl = null,
                    followers = (100..5000).random(),
                    following = (50..1000).random(),
                    posts = (10..200).random(),
                    recentPosts = generateMockPosts(handle, 10),
                    recentVideos = generateMockVideos(handle, 6),
                    recentLikes = generateMockPosts("liked", 9),
                    recentMedia = generateMockMedia(handle, 12)
                )
                
                // Check if following
                _isFollowing.value = false
                
                Log.d(TAG, "✅ Profile loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading profile: ${e.message}", e)
                _userProfile.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Follow a user
     */
    fun followUser(handle: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👤 Following user: $handle")
                
                // Simulate network delay
                delay(500)
                
                _isFollowing.value = true
                
                // Update followers count
                _userProfile.value = _userProfile.value?.copy(
                    followers = (_userProfile.value?.followers ?: 0) + 1
                )
                
                Log.d(TAG, "✅ Successfully followed user")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error following user: ${e.message}", e)
            }
        }
    }

    /**
     * Unfollow a user
     */
    fun unfollowUser(handle: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👤 Unfollowing user: $handle")
                
                // Simulate network delay
                delay(500)
                
                _isFollowing.value = false
                
                // Update followers count
                _userProfile.value = _userProfile.value?.copy(
                    followers = (_userProfile.value?.followers ?: 1).coerceAtLeast(1) - 1
                )
                
                Log.d(TAG, "✅ Successfully unfollowed user")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error unfollowing user: ${e.message}", e)
            }
        }
    }

    /**
     * Get current user handle
     */
    fun getCurrentUserHandle(): String {
        return "currentuser"
    }

    /**
     * Generate mock posts for testing
     */
    private fun generateMockPosts(authorHandle: String, count: Int): List<Post> {
        return List(count) { index ->
            Post(
                id = "${authorHandle}_post_$index",
                authorName = "${authorHandle.capitalize()} User",
                authorHandle = authorHandle,
                content = "This is post #$index by $authorHandle. #trendflick #bluesky",
                timestamp = System.currentTimeMillis() - (index * 3600000),
                likes = (10..500).random(),
                comments = (0..50).random(),
                reposts = (0..30).random(),
                hashtags = listOf("trendflick", "bluesky"),
                mentions = emptyList(),
                mediaUrl = if (index % 3 == 0) "https://example.com/image.jpg" else null,
                isVideo = false
            )
        }
    }

    /**
     * Generate mock videos for testing
     */
    private fun generateMockVideos(authorHandle: String, count: Int): List<Post> {
        return List(count) { index ->
            Post(
                id = "${authorHandle}_video_$index",
                authorName = "${authorHandle.capitalize()} User",
                authorHandle = authorHandle,
                content = "Check out this video! #trendflick #video",
                timestamp = System.currentTimeMillis() - (index * 3600000),
                likes = (10..500).random(),
                comments = (0..50).random(),
                reposts = (0..30).random(),
                hashtags = listOf("trendflick", "video"),
                mentions = emptyList(),
                mediaUrl = "https://example.com/video.mp4",
                isVideo = true
            )
        }
    }

    /**
     * Generate mock media for testing
     */
    private fun generateMockMedia(authorHandle: String, count: Int): List<Post> {
        return List(count) { index ->
            Post(
                id = "${authorHandle}_media_$index",
                authorName = "${authorHandle.capitalize()} User",
                authorHandle = authorHandle,
                content = "Media post #$index",
                timestamp = System.currentTimeMillis() - (index * 3600000),
                likes = (10..500).random(),
                comments = (0..50).random(),
                reposts = (0..30).random(),
                hashtags = emptyList(),
                mentions = emptyList(),
                mediaUrl = "https://example.com/media.jpg",
                isVideo = index % 3 == 0
            )
        }
    }
}

/**
 * Extension function to capitalize the first letter of a string
 */
private fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this[0].uppercase() + this.substring(1)
    } else {
        this
    }
}
