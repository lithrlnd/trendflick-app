package com.trendflick.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.auth.BlueskyCredentialsManager
import com.trendflick.data.model.User
import com.trendflick.data.repository.UserRepository
import com.trendflick.data.repository.AtProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.delay

private const val TAG = "SettingsViewModel"

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val atProtocolRepository: AtProtocolRepository,
    private val credentialsManager: BlueskyCredentialsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { user ->
                    _uiState.update { it.copy(user = user) }
                }
        }
    }

    fun updateProfilePicture(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Upload image to BlueSky's blob store
                val blobResult = atProtocolRepository.uploadBlob(uri)
                
                // Update user profile with new avatar
                uiState.value.user?.let { currentUser ->
                    val updatedUser = currentUser.copy(
                        avatar = blobResult.blobUri
                    )
                    userRepository.updateUser(updatedUser)
                    
                    // Update profile on BlueSky
                    atProtocolRepository.updateProfile(
                        did = updatedUser.did,
                        displayName = updatedUser.displayName,
                        description = updatedUser.description,
                        avatar = blobResult.blobUri
                    )
                }
                
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to update profile picture: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            try {
                uiState.value.user?.let { currentUser ->
                    val updatedUser = currentUser.copy(displayName = newName)
                    userRepository.updateUser(updatedUser)
                    
                    // Update profile on BlueSky
                    atProtocolRepository.updateProfile(
                        did = updatedUser.did,
                        displayName = newName,
                        description = updatedUser.description,
                        avatar = updatedUser.avatar
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to update display name: ${e.message}")
                }
            }
        }
    }

    fun updateBio(newBio: String) {
        viewModelScope.launch {
            try {
                uiState.value.user?.let { currentUser ->
                    val updatedUser = currentUser.copy(description = newBio)
                    userRepository.updateUser(updatedUser)
                    
                    // Update profile on BlueSky
                    atProtocolRepository.updateProfile(
                        did = updatedUser.did,
                        displayName = updatedUser.displayName,
                        description = newBio,
                        avatar = updatedUser.avatar
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to update bio: ${e.message}")
                }
            }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            try {
                userRepository.updateUser(user)
                
                // Update profile on BlueSky if needed
                atProtocolRepository.updateProfile(
                    did = user.did,
                    displayName = user.displayName,
                    description = user.description,
                    avatar = user.avatar
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to update user settings: ${e.message}")
                }
            }
        }
    }

    suspend fun logout(): Boolean {
        return try {
            Log.d(TAG, "🔄 Starting AT Protocol logout process")
            _uiState.update { it.copy(isLoading = true) }
            
            var logoutSuccess = true
            
            // 1. Get refresh token before any cleanup
            val refreshToken = credentialsManager.getRefreshToken()
            Log.d(TAG, "✅ Got refresh token: ${refreshToken != null}")
            
            // 2. Delete BlueSky session through Entryway first
            if (!refreshToken.isNullOrEmpty()) {
                try {
                    atProtocolRepository.deleteSession(refreshToken)
                        .onSuccess {
                            Log.d(TAG, "✅ BlueSky session deleted via Entryway")
                        }
                        .onFailure { error ->
                            Log.w(TAG, "⚠️ BlueSky session deletion response: ${error.message}")
                            logoutSuccess = false
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Error during BlueSky session deletion: ${e.message}")
                    logoutSuccess = false
                }
                
                delay(500)
            }
            
            // 3. Clear all local data regardless of session deletion result
            try {
                credentialsManager.clearCredentials()
                Log.d(TAG, "✅ BlueSky credentials and session cleared")
                
                uiState.value.user?.let { user ->
                    userRepository.deleteUser(user.did)
                    Log.d(TAG, "✅ Local user data cleared for DID: ${user.did}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error clearing local data: ${e.message}")
                logoutSuccess = false
            }
            
            // 4. Verify cleanup was successful
            if (credentialsManager.hasValidCredentials()) {
                Log.e(TAG, "❌ Credentials still valid after cleanup")
                logoutSuccess = false
            }
            
            // 5. Update UI state to trigger navigation
            _uiState.update { 
                it.copy(
                    user = null, 
                    isLoading = false,
                    error = if (!logoutSuccess) "Partial logout - please try again" else null,
                    isLoggedOut = true  // This will trigger navigation
                )
            }
            
            if (logoutSuccess) {
                Log.d(TAG, "✅ AT Protocol logout completed successfully")
            } else {
                Log.w(TAG, "⚠️ AT Protocol logout completed with warnings")
            }
            
            logoutSuccess
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Critical error during logout:
                Type: ${e.javaClass.name}
                Message: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            
            try {
                credentialsManager.clearCredentials()
            } catch (e2: Exception) {
                Log.e(TAG, "❌ Emergency cleanup also failed: ${e2.message}")
            }
            
            _uiState.update { 
                it.copy(
                    user = null,
                    error = "Logout failed: ${e.message}",
                    isLoading = false,
                    isLoggedOut = true  // Still navigate away even on error
                )
            }
            false
        }
    }
}

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false
) 