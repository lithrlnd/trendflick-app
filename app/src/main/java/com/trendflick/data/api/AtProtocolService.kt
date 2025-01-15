package com.trendflick.data.api

import com.trendflick.data.model.AtSession
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AtProtocolService {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("com.atproto.server.createSession")
    suspend fun createSession(
        @Body credentials: Map<String, String>
    ): AtSession
} 