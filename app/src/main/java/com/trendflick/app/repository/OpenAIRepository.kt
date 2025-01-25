package com.trendflick.app.repository

import com.google.gson.Gson
import com.trendflick.app.api.AIRequest
import com.trendflick.app.api.OpenAIService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIRepository @Inject constructor() {
    private val service: OpenAIService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://us-central1-trendflick-d7188.cloudfunctions.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        service = retrofit.create(OpenAIService::class.java)
    }

    suspend fun generateAIResponse(prompt: String): Result<String> {
        return try {
            val response = service.generateResponse(AIRequest(prompt))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: "No response data")
            } else {
                Result.failure(Exception(response.body()?.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 