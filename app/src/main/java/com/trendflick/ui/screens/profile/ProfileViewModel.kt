package com.trendflick.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.User
import com.trendflick.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    fun loadProfile(did: String) {
        viewModelScope.launch {
            userRepository.getUserByDid(did)
                .catch { error ->
                    _profileState.value = ProfileState.Error(error.message ?: "Unknown error")
                }
                .collect { user ->
                    _profileState.value = user?.let { ProfileState.Success(it) }
                        ?: ProfileState.Error("User not found")
                }
        }
    }

    fun loadProfileByHandle(handle: String) {
        viewModelScope.launch {
            userRepository.getUserByHandle(handle)
                .catch { error ->
                    _profileState.value = ProfileState.Error(error.message ?: "Unknown error")
                }
                .collect { user ->
                    _profileState.value = user?.let { ProfileState.Success(it) }
                        ?: ProfileState.Error("User not found")
                }
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
} 