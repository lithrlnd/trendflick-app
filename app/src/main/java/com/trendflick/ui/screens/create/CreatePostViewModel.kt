package com.trendflick.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.model.SuggestionItem
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

    private val _suggestions = MutableStateFlow<List<SuggestionItem>>(emptyList())
    val suggestions: StateFlow<List<SuggestionItem>> = _suggestions.asStateFlow()

    fun searchMentions(query: String) {
        viewModelScope.launch {
            try {
                if (query.length >= 2) {
                    val handles = atProtocolRepository.searchHandles(query)
                    _suggestions.value = handles.map { handle ->
                        SuggestionItem.Mention(
                            did = handle.did,
                            handle = handle.handle,
                            displayName = handle.displayName ?: handle.handle,
                            avatarUrl = handle.avatar
                        )
                    }
                } else {
                    _suggestions.value = emptyList()
                }
            } catch (e: Exception) {
                _suggestions.value = emptyList()
                _uiState.value = CreatePostUiState(error = "Failed to load suggestions")
            }
        }
    }

    fun searchHashtags(query: String) {
        viewModelScope.launch {
            try {
                if (query.length >= 2) {
                    val hashtags = atProtocolRepository.searchHashtags(query)
                    _suggestions.value = hashtags.map { tag ->
                        SuggestionItem.Hashtag(
                            tag = tag.tag,
                            postCount = tag.count
                        )
                    }
                } else {
                    _suggestions.value = emptyList()
                }
            } catch (e: Exception) {
                _suggestions.value = emptyList()
                _uiState.value = CreatePostUiState(error = "Failed to load suggestions")
            }
        }
    }

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
}

data class CreatePostUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPostSuccessful: Boolean = false
)

data class HandleSuggestion(
    val handle: String,
    val displayName: String? = null
) 