package com.trendflick.ui.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {
    private val _selectedFeed = MutableStateFlow("Trends")
    val selectedFeed: StateFlow<String> = _selectedFeed.asStateFlow()

    fun updateSelectedFeed(feed: String) {
        _selectedFeed.value = feed
    }
} 