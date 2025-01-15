package com.trendflick.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.trendflick.data.auth.BlueskyCredentialsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val credentialsManager: BlueskyCredentialsManager
) : ViewModel() {
    
    fun hasCredentials(): Boolean {
        // Force login screen for first-time use
        val handle = credentialsManager.getHandle()
        val password = credentialsManager.getPassword()
        
        // Clear any existing credentials to ensure proper login flow
        credentialsManager.clearCredentials()
        
        return false // Always go to login screen first
    }
} 