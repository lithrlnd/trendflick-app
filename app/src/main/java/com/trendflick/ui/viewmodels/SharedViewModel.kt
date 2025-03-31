package com.trendflick.ui.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Shared ViewModel to manage state that needs to be accessed across multiple screens
 */
@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {
    
    // Selected feed (Trends or Flicks)
    private val _selectedFeed = MutableStateFlow("Trends")
    val selectedFeed: StateFlow<String> = _selectedFeed.asStateFlow()
    
    // Update selected feed
    fun updateSelectedFeed(feed: String) {
        _selectedFeed.value = feed
    }
    
    // Current user DID
    private val _currentUserDid = MutableStateFlow<String?>(null)
    val currentUserDid: StateFlow<String?> = _currentUserDid.asStateFlow()
    
    // Update current user DID
    fun updateCurrentUserDid(did: String?) {
        _currentUserDid.value = did
    }
    
    // Cast button visibility state
    private val _showCastButton = MutableStateFlow(false)
    val showCastButton: StateFlow<Boolean> = _showCastButton.asStateFlow()
    
    // Update cast button visibility
    fun updateCastButtonVisibility(show: Boolean) {
        _showCastButton.value = show
    }
}
