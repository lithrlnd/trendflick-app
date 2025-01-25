package com.trendflick.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.app.repository.OpenAIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OpenAIViewModel @Inject constructor() : ViewModel() {
    private val repository = OpenAIRepository()

    private val _aiResponse = MutableStateFlow<AIResponseState>(AIResponseState.Idle)
    val aiResponse: StateFlow<AIResponseState> = _aiResponse.asStateFlow()

    fun generateResponse(prompt: String) {
        viewModelScope.launch {
            _aiResponse.value = AIResponseState.Loading
            try {
                repository.generateAIResponse(prompt).fold(
                    onSuccess = { response ->
                        _aiResponse.value = AIResponseState.Success(response)
                    },
                    onFailure = { error ->
                        _aiResponse.value = AIResponseState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _aiResponse.value = AIResponseState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class AIResponseState {
    object Idle : AIResponseState()
    object Loading : AIResponseState()
    data class Success(val response: String) : AIResponseState()
    data class Error(val message: String) : AIResponseState()
} 