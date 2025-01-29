package com.trendflick.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.MessageRepository
import com.trendflick.data.repository.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Start watching for conversations
        viewModelScope.launch {
            messageRepository.watchConversations()
                .catch { e ->
                    _error.value = e.message ?: "Failed to load conversations"
                }
                .collect { conversations ->
                    _conversations.value = conversations.sortedByDescending { it.updatedAt }
                }
        }
        
        // Initial load
        loadConversations()
    }
    
    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                messageRepository.getConversations()
                    .onSuccess { conversations ->
                        // Update will come through watchConversations()
                    }
                    .onFailure { e ->
                        _error.value = e.message ?: "Failed to load conversations"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun markConversationAsRead(conversationDid: String) {
        viewModelScope.launch {
            try {
                messageRepository.markAsRead(conversationDid, Instant.now())
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to mark conversation as read"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
} 