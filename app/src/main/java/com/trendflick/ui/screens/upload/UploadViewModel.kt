package com.trendflick.ui.screens.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Video
import com.trendflick.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Initial)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    fun setPermissionsGranted(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    fun uploadVideo(videoUri: Uri, title: String, description: String, hashtags: List<String>) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading
                
                val video = Video(
                    id = 0, // Will be set by SampleDataProvider
                    userId = "currentUser", // In a real app, this would come from auth
                    username = "CurrentUser", // In a real app, this would come from auth
                    videoUrl = videoUri.toString(),
                    thumbnailUrl = "", // In a real app, we'd generate this
                    title = title,
                    description = description,
                    likes = 0,
                    comments = 0,
                    shares = 0,
                    hashtags = hashtags
                )
                
                videoRepository.insertVideo(video)
                _uploadState.value = UploadState.Success
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }
}

sealed class UploadState {
    object Initial : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
} 