package com.trendflick.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.AtProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val atProtocolRepository: AtProtocolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _handleSuggestions = MutableStateFlow<List<HandleSuggestion>>(emptyList())
    val handleSuggestions: StateFlow<List<HandleSuggestion>> = _handleSuggestions.asStateFlow()

    fun createPost(text: String) {
        viewModelScope.launch {
            try {
                _uiState.value = CreatePostUiState(isLoading = true)
                
                val timestamp = Instant.now().toString()
                val result = atProtocolRepository.createPost(text, timestamp)
                
                _uiState.value = CreatePostUiState(isPostSuccessful = true)
            } catch (e: Exception) {
                _uiState.value = CreatePostUiState(error = e.message ?: "Failed to create post")
            }
        }
    }

    fun createReply(text: String, parentUri: String, parentCid: String) {
        viewModelScope.launch {
            try {
                _uiState.value = CreatePostUiState(isLoading = true)
                
                val timestamp = Instant.now().toString()
                val result = atProtocolRepository.createReply(
                    text = text,
                    parentUri = parentUri,
                    parentCid = parentCid,
                    timestamp = timestamp
                )
                
                _uiState.value = CreatePostUiState(isPostSuccessful = true)
            } catch (e: Exception) {
                _uiState.value = CreatePostUiState(error = e.message ?: "Failed to create reply")
            }
        }
    }

    fun searchHandles(query: String) {
        viewModelScope.launch {
            try {
                // Call the AT Protocol search endpoint
                val results = atProtocolRepository.searchUsers(query)
                _handleSuggestions.value = results.map { user ->
                    HandleSuggestion(
                        handle = user.handle,
                        displayName = user.displayName ?: ""  // Provide default empty string
                    )
                }
            } catch (e: Exception) {
                // Handle error silently for suggestions
                _handleSuggestions.value = emptyList()
            }
        }
    }
}

data class CreatePostUiState(
    val isLoading: Boolean = false,
    val isPostSuccessful: Boolean = false,
    val error: String? = null
)

data class HandleSuggestion(
    val handle: String,
    val displayName: String? = null
) 