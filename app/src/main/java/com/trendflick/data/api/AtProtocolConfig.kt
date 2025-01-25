package com.trendflick.data.api

import com.trendflick.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AtProtocolConfig @Inject constructor() {
    val handle: String = BuildConfig.BLUESKY_HANDLE
    val appPassword: String = BuildConfig.BLUESKY_APP_PASSWORD
    
    fun isConfigured(): Boolean {
        return handle.isNotEmpty() && appPassword.isNotEmpty()
    }
} 