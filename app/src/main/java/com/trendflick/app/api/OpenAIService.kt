package com.trendflick.app.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAIService {
    @POST("generateAIResponse")
    suspend fun generateResponse(@Body request: AIRequest): Response<AIResponse>
}

data class AIRequest(
    val prompt: String,
    val model: String = "gpt-3.5-turbo"
)

data class AIResponse(
    val success: Boolean,
    val data: String?,
    val error: String?,
    val message: String?
) 