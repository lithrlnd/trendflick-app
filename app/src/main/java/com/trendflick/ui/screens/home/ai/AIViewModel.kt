package com.trendflick.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.trendflick.app.repository.OpenAIRepository

@HiltViewModel
class AIViewModel @Inject constructor() : ViewModel() {
    private val repository = OpenAIRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            role = ChatRole.Assistant,
            content = "Hey! I'm your social media advisor. I can help you create engaging content and start meaningful conversations. What would you like to discuss?"
        )
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun sendMessage(userMessage: String) {
        _isLoading.value = true
        try {
            // Add user message to chat
            val newUserMessage = ChatMessage(
                role = ChatRole.User,
                content = userMessage
            )
            _messages.value = _messages.value + newUserMessage

            // Get AI response using our Firebase Function
            repository.generateAIResponse(userMessage).fold(
                onSuccess = { response ->
                    val aiMessage = ChatMessage(
                        role = ChatRole.Assistant,
                        content = response
                    )
                    _messages.value = _messages.value + aiMessage
                },
                onFailure = { error ->
                    val errorMessage = ChatMessage(
                        role = ChatRole.Assistant,
                        content = "Sorry, I encountered an error. Please try again."
                    )
                    _messages.value = _messages.value + errorMessage
                    error.printStackTrace()
                }
            )
        } catch (e: Exception) {
            // Add error message to chat
            val errorMessage = ChatMessage(
                role = ChatRole.Assistant,
                content = "Sorry, I encountered an error. Please try again."
            )
            _messages.value = _messages.value + errorMessage
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                role = ChatRole.Assistant,
                content = "Hey! I'm your social media advisor. I can help you create engaging content and start meaningful conversations. What would you like to discuss?"
            )
        )
    }
} 