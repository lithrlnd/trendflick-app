package com.trendflick.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    private var tokenType: String = "Bearer"
    private val TAG = "TF_Auth"

    fun setTokenType(type: String) {
        tokenType = type
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        
        Log.d(TAG, "🔐 Request Details:\nPath: $path")
        
        // Don't add auth header for session creation
        if (path.endsWith("/createSession")) {
            return chain.proceed(request)
        }

        val accessToken = sessionManager.getAccessToken()
        val refreshToken = sessionManager.getRefreshToken()

        Log.d(TAG, """
            Has Access Token: ${!accessToken.isNullOrEmpty()}
            Has Refresh Token: ${!refreshToken.isNullOrEmpty()}
            Is Refresh: ${path.endsWith("/refreshSession")}
            Is Timeline: ${path.contains("/getTimeline")}
            Is Create Session: ${path.endsWith("/createSession")}
        """.trimIndent())

        val requestBuilder = request.newBuilder()

        if (accessToken.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ No access token available for request")
            // Let the request proceed without token, the server will return 401 if needed
            return chain.proceed(request)
        }

        // Add authorization header
        requestBuilder.header("Authorization", "$tokenType $accessToken")

        val modifiedRequest = requestBuilder.build()
        
        return try {
            val response = chain.proceed(modifiedRequest)
            
            Log.d(TAG, """
                📡 Response Details:
                Code: ${response.code}
                Message: ${response.message}
                Headers: ${response.headers}
            """.trimIndent())
            
            when (response.code) {
                401 -> {
                    Log.w(TAG, "🔄 Token expired, clearing session")
                    sessionManager.clearSession()
                }
                429 -> {
                    Log.w(TAG, "⏳ Rate limited")
                }
            }
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "❌ Request failed: ${e.message}")
            throw e
        }
    }
} 