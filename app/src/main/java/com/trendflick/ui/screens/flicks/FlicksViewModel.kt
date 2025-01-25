package com.trendflick.ui.screens.flicks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Video
import com.trendflick.data.repository.VideoRepository
import com.trendflick.data.repository.VideoRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlicksViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val TAG = "TF_FlicksViewModel"
    
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadVideos() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔄 Starting video load...")
                
                // Get videos from repository
                val loadedVideos = videoRepository.getVideos()
                
                Log.d(TAG, """
                    📊 Video Load Results:
                    Total Videos: ${loadedVideos.size}
                    Has Videos: ${loadedVideos.isNotEmpty()}
                    First Video URL: ${loadedVideos.firstOrNull()?.videoUrl}
                """.trimIndent())
                
                // Update UI state
                _videos.value = loadedVideos
                
                if (loadedVideos.isEmpty()) {
                    Log.w(TAG, "⚠️ No videos found in feed")
                } else {
                    Log.d(TAG, """
                        ✅ Videos loaded successfully:
                        ${loadedVideos.joinToString("\n") { 
                            "🎥 ${it.uri}: ${it.videoUrl}"
                        }}
                    """.trimIndent())
                }
                
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ Error loading videos:
                    Error: ${e.message}
                    Stack: ${e.stackTraceToString()}
                """.trimIndent())
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun testVideoInFolder() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                (videoRepository as VideoRepositoryImpl).testVideoInFolder()
                loadVideos() // Refresh the feed after test
            } catch (e: Exception) {
                Log.e(TAG, "Test video error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun testFolderAccess() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                (videoRepository as VideoRepositoryImpl).testFolderAccess()
            } catch (e: Exception) {
                Log.e(TAG, "Test folder error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun testSmallFileUpload() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🧪 Starting small file upload test")
                (videoRepository as VideoRepositoryImpl).testSmallFileUpload()
                Log.d(TAG, "✅ Small file test complete")
                loadVideos() // Refresh the feed after test
            } catch (e: Exception) {
                Log.e(TAG, "❌ Small file test failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
} 