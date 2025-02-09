package com.trendflick.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.User
import com.trendflick.data.model.AtSession
import com.trendflick.data.repository.UserRepository
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.auth.BlueskyCredentialsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

private const val TAG = "ProfileViewModel"

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val atProtocolRepository: AtProtocolRepository,
    private val credentialsManager: BlueskyCredentialsManager
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    init {
        loadCurrentUserProfile()
    }

    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                Log.d(TAG, "🔄 Loading current user profile")
                
                // Ensure we have a valid session first
                if (!atProtocolRepository.ensureValidSession()) {
                    Log.e(TAG, "❌ No valid session available")
                    _profileState.value = ProfileState.Error("Please log in again")
                    return@launch
                }
                
                // Try to get current user from local database first
                val currentUser = userRepository.getCurrentUser().first()
                
                if (currentUser != null) {
                    Log.d(TAG, """
                        ✅ Found current user in local database:
                        DID: ${currentUser.did}
                        Handle: ${currentUser.handle}
                        Has Avatar: ${!currentUser.avatar.isNullOrEmpty()}
                    """.trimIndent())
                    
                    try {
                        // Get fresh profile data from Bluesky
                        Log.d(TAG, "🔄 Fetching fresh profile data from Bluesky")
                        val profile = atProtocolRepository.getUserByDid(currentUser.did).first()
                        
                        if (profile != null) {
                            Log.d(TAG, """
                                ✅ Got fresh profile data:
                                Avatar URL: ${profile.avatar}
                                Display Name: ${profile.displayName}
                            """.trimIndent())
                            
                            // Update local user with fresh data
                            val updatedUser = profile.copy(
                                accessJwt = currentUser.accessJwt,
                                refreshJwt = currentUser.refreshJwt
                            )
                            userRepository.updateUser(updatedUser)
                            _profileState.value = ProfileState.Success(updatedUser)
                        } else {
                            Log.w(TAG, "⚠️ No fresh profile data available, using cached data")
                            _profileState.value = ProfileState.Success(currentUser)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error fetching fresh profile data: ${e.message}")
                        Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                        // If fetching fresh data fails, use cached data
                        _profileState.value = ProfileState.Success(currentUser)
                    }
                } else {
                    Log.d(TAG, "⚠️ No current user found, attempting to create new session")
                    // Try to create new session
                    createNewSession()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading profile: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                _profileState.value = ProfileState.Error("Failed to load profile: ${e.message}")
            }
        }
    }

    private suspend fun createNewSession() {
        val handle = credentialsManager.getHandle()
        val password = credentialsManager.getPassword()
        
        if (!handle.isNullOrEmpty() && !password.isNullOrEmpty()) {
            try {
                val sessionResult = atProtocolRepository.createSession(handle, password)
                sessionResult.onSuccess { session ->
                    // Get fresh profile data after creating session
                    val profile = atProtocolRepository.getUserByDid(session.did).first()
                    
                    val user = profile?.copy(
                        accessJwt = session.accessJwt,
                        refreshJwt = session.refreshJwt
                    ) ?: User(
                        did = session.did,
                        handle = session.handle,
                        accessJwt = session.accessJwt,
                        refreshJwt = session.refreshJwt
                    )
                    
                    userRepository.updateUser(user)
                    _profileState.value = ProfileState.Success(user)
                }.onFailure { error ->
                    Log.e(TAG, "Failed to create session: ${error.message}")
                    _profileState.value = ProfileState.Error("Failed to create session: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating session: ${e.message}")
                _profileState.value = ProfileState.Error("Failed to create session: ${e.message}")
            }
        } else {
            _profileState.value = ProfileState.Error("Please log in again")
        }
    }

    fun loadProfile(did: String) {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                atProtocolRepository.getUserByDid(did)
                    .catch { error ->
                        Log.e(TAG, "Error loading profile by DID: ${error.message}")
                        _profileState.value = ProfileState.Error(error.message ?: "Unknown error")
                    }
                    .collect { user ->
                        _profileState.value = user?.let { ProfileState.Success(it) }
                            ?: ProfileState.Error("User not found")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}")
                _profileState.value = ProfileState.Error("Failed to load profile: ${e.message}")
            }
        }
    }

    fun loadProfileByHandle(handle: String) {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                atProtocolRepository.getUserByHandle(handle)
                    .catch { error ->
                        Log.e(TAG, "Error loading profile by handle: ${error.message}")
                        _profileState.value = ProfileState.Error(error.message ?: "Unknown error")
                    }
                    .collect { user ->
                        _profileState.value = user?.let { ProfileState.Success(it) }
                            ?: ProfileState.Error("User not found")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}")
                _profileState.value = ProfileState.Error("Failed to load profile: ${e.message}")
            }
        }
    }

    fun refreshProfile() {
        loadCurrentUserProfile()
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
} 