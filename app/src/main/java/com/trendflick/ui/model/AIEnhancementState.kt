package com.trendflick.ui.model

sealed class AIEnhancementState {
    object Initial : AIEnhancementState()
    object Loading : AIEnhancementState()
    data class Success(val enhancement: AIEnhancement) : AIEnhancementState()
    data class Error(val message: String) : AIEnhancementState()
}

data class AIEnhancement(
    val enhancedPost: String,
    val hashtags: List<String>
) 
