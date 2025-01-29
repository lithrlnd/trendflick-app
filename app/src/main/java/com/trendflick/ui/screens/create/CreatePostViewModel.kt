package com.trendflick.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.ui.model.AIEnhancementState
import com.trendflick.ui.model.AIEnhancement
import com.trendflick.ui.model.SuggestionItem
import com.trendflick.app.repository.OpenAIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val repository: AtProtocolRepository,
    private val openAIRepository: OpenAIRepository
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<SuggestionItem>>(emptyList())
    val suggestions: StateFlow<List<SuggestionItem>> = _suggestions.asStateFlow()

    private val _aiEnhancedContent = MutableStateFlow<AIEnhancementState>(AIEnhancementState.Initial)
    val aiEnhancedContent: StateFlow<AIEnhancementState> = _aiEnhancedContent.asStateFlow()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun searchMentions(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                // Add debounce to avoid too many API calls
                delay(300)
                val results = repository.searchUsers(query)
                _suggestions.value = results.actors.map { actor ->
                    SuggestionItem.Mention(
                        handle = actor.handle,
                        displayName = actor.displayName,
                        avatar = actor.avatar
                    )
                }
            } catch (e: Exception) {
                _suggestions.value = emptyList()
            }
        }
    }

    fun searchHashtags(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                // Add debounce to avoid too many API calls
                delay(300)
                if (query.isNotBlank()) {
                    val results = repository.searchHashtags(query)
                    _suggestions.value = results.map { hashtag ->
                        SuggestionItem.Hashtag(
                            tag = hashtag.tag,
                            postCount = hashtag.count
                        )
                    }.sortedByDescending { it.postCount }
                } else {
                    _suggestions.value = emptyList()
                }
            } catch (e: Exception) {
                _suggestions.value = emptyList()
            }
        }
    }

    fun createPost(text: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val timestamp = Instant.now().toString()
                
                // Parse facets for mentions and links before creating post
                val facets = repository.parseFacets(text)
                repository.createPost(
                    text = text,
                    timestamp = timestamp,
                    facets = facets
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPostSuccessful = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create post"
                )
            }
        }
    }

    fun createReply(text: String, parentUri: String, parentCid: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val timestamp = Instant.now().toString()
                repository.createReply(
                    text = text,
                    parentUri = parentUri,
                    parentCid = parentCid,
                    timestamp = timestamp
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPostSuccessful = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create reply"
                )
            }
        }
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun enhancePostWithAI(text: String) {
        viewModelScope.launch {
            _aiEnhancedContent.value = AIEnhancementState.Loading
            try {
                val enhancement = repository.enhancePostWithAI(text)
                _aiEnhancedContent.value = AIEnhancementState.Success(enhancement)
            } catch (e: Exception) {
                _aiEnhancedContent.value = AIEnhancementState.Error(e.message ?: "Failed to enhance post")
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