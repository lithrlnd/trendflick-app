package com.trendflick.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.auth.BlueskyCredentialsManager
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SharedViewModel"

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val atProtocolRepository: AtProtocolRepository,
    private val credentialsManager: BlueskyCredentialsManager
) : ViewModel() {
    private val _selectedFeed = MutableStateFlow("Trends")
    val selectedFeed: StateFlow<String> = _selectedFeed.asStateFlow()

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

    private val _followingUsers = MutableStateFlow<Set<String>>(emptySet())
    val followingUsers: StateFlow<Set<String>> = _followingUsers.asStateFlow()

    init {
        loadFollowingUsers()
    }

    private fun loadFollowingUsers() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val userId = credentialsManager.getDid() ?: return@withContext
                    val followsResult = atProtocolRepository.getFollows(userId)
                    followsResult.onSuccess { response ->
                        val followingDids = response.follows.map { profile -> profile.did }.toSet()
                        _followingUsers.value = followingDids
                        Log.d(TAG, "✅ Successfully loaded following users. Count: ${followingDids.size}")
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Failed to load following users: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in loadFollowingUsers: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    fun toggleFollow(did: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val isCurrentlyFollowing = _followingUsers.value.contains(did)
                    
                    val success = if (isCurrentlyFollowing) {
                        // Unfollow user
                        Log.d("SharedViewModel", "Unfollowing user: $did")
                        atProtocolRepository.unfollowUser(did)
                    } else {
                        // Follow user
                        Log.d("SharedViewModel", "Following user: $did")
                        atProtocolRepository.followUser(did)
                    }
                    
                    if (success) {
                        // Update local state
                        Log.d("SharedViewModel", "Follow action successful, updating state")
                        if (isCurrentlyFollowing) {
                            _followingUsers.value = _followingUsers.value - did
                        } else {
                            _followingUsers.value = _followingUsers.value + did
                        }
                    } else {
                        Log.e("SharedViewModel", "Follow action failed")
                    }
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error toggling follow: ${e.message}", e)
            }
        }
    }

    fun updateSelectedFeed(feed: String) {
        _selectedFeed.value = feed
    }

    fun toggleBottomSheet(show: Boolean) {
        _isBottomSheetVisible.value = show
    }
} 