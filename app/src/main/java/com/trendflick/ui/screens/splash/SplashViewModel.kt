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
        val handle = credentialsManager.getHandle()
        val password = credentialsManager.getPassword()
        
        // Check if both handle and password exist
        return !handle.isNullOrBlank() && !password.isNullOrBlank()
    }
} 