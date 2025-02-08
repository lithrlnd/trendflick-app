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
                
                // Add TrendFlick signature
                val signedText = "$text\n\nPosted from TrendFlick ✨"
                
                // Make facets optional
                val facets = try {
                    repository.parseFacets(signedText)
                } catch (e: Exception) {
                    null // If facet parsing fails, continue without facets
                }
                
                repository.createPost(
                    text = signedText,
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
                // Use OpenAI repository to get AI-enhanced content
                val aiResponse = openAIRepository.generateAIResponse(
                    "Enhance this social media post and suggest relevant hashtags. " +
                    "Format the response as JSON with 'enhancedPost' and 'hashtags' fields. " +
                    "Keep the enhanced post under 300 characters. Original post: $text"
                )
                
                aiResponse.fold(
                    onSuccess = { jsonResponse ->
                        try {
                            // Parse hashtags from response
                            val hashtags = jsonResponse
                                .substringAfter("\"hashtags\":")
                                .substringBefore("]")
                                .substringAfter("[")
                                .split(",")
                                .map { it.trim().trim('"') }
                                .filter { it.isNotEmpty() }
                            
                            // Parse enhanced post
                            val enhancedPost = jsonResponse
                                .substringAfter("\"enhancedPost\":")
                                .substringAfter("\"")
                                .substringBefore("\"")
                                .trim()
                            
                            _aiEnhancedContent.value = AIEnhancementState.Success(
                                AIEnhancement(
                                    enhancedPost = enhancedPost,
                                    hashtags = hashtags
                                )
                            )
                        } catch (e: Exception) {
                            _aiEnhancedContent.value = AIEnhancementState.Error(
                                "Failed to parse AI response: ${e.message}"
                            )
                        }
                    },
                    onFailure = { error ->
                        _aiEnhancedContent.value = AIEnhancementState.Error(
                            error.message ?: "Failed to enhance post"
                        )
                    }
                )
            } catch (e: Exception) {
                _aiEnhancedContent.value = AIEnhancementState.Error(
                    e.message ?: "Failed to enhance post"
                )
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