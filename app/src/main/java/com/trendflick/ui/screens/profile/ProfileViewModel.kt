package com.trendflick.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.UserProfile
import com.trendflick.data.model.Video
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.repository.UserRepository
import com.trendflick.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val videoRepository: VideoRepository,
    private val atProtocolRepository: AtProtocolRepository
) : ViewModel() {

    private val TAG = "TF_ProfileViewModel"

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _userVideos = MutableStateFlow<List<Video>>(emptyList())
    val userVideos: StateFlow<List<Video>> = _userVideos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Loading user profile")
                
                // Get current user profile from repository
                val profile = userRepository.getCurrentUserProfile()
                
                // Update UI state
                _userProfile.value = profile
                
                Log.d(TAG, "User profile loaded successfully: ${profile.username}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile: ${e.message}")
                // Set default profile if loading fails
                _userProfile.value = UserProfile(
                    username = "TrendFlick User",
                    handle = "user",
                    bio = "Welcome to TrendFlick!"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserVideos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Loading user videos")
                
                // Get current user's videos from repository
                val videos = videoRepository.getUserVideos()
                
                // Filter to only include videos (not images)
                val videoOnly = videos.filter { 
                    it.videoUrl.isNotEmpty() && 
                    (it.videoUrl.endsWith(".mp4", ignoreCase = true) || 
                     it.videoUrl.endsWith(".mov", ignoreCase = true) ||
                     it.videoUrl.endsWith(".webm", ignoreCase = true) ||
                     it.videoUrl.contains("video", ignoreCase = true))
                }
                
                // Update UI state
                _userVideos.value = videoOnly
                
                Log.d(TAG, "User videos loaded successfully: ${videoOnly.size} videos")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user videos: ${e.message}")
                // Set empty list if loading fails
                _userVideos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(username: String, bio: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Updating user profile")
                
                // Update profile in repository
                val updatedProfile = userRepository.updateUserProfile(username, bio)
                
                // Update UI state
                _userProfile.value = updatedProfile
                
                Log.d(TAG, "User profile updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user profile: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfileImage(imageUri: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Updating profile image")
                
                // Update profile image in repository
                val updatedProfile = userRepository.updateProfileImage(imageUri)
                
                // Update UI state
                _userProfile.value = updatedProfile
                
                Log.d(TAG, "Profile image updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile image: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
