package com.trendflick.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoggingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        println("DEBUG: API Request -> ${request.method} ${request.url}")
        request.headers.forEach { (name, value) ->
            println("DEBUG: Header -> $name: $value")
        }
        
        val response = chain.proceed(request)
        println("DEBUG: API Response <- ${response.code} for ${request.url}")
        if (!response.isSuccessful) {
            println("DEBUG: Error Response Body: ${response.peekBody(Long.MAX_VALUE).string()}")
        }
        return response
    }
} 