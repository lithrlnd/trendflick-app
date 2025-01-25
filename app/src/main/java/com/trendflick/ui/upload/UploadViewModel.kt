package com.trendflick.ui.upload

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val lastUploadedUrl: String? = null,
    val error: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(UploadUiState())
    val uiState: State<UploadUiState> = _uiState

    fun uploadVideo(
        uri: Uri,
        title: String,
        description: String,
        visibility: String = "public",
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isUploading = true)
                
                Log.d("UploadViewModel", """
                    Starting video upload:
                    Title: $title
                    Description: $description
                    Visibility: $visibility
                    Tags: ${tags.joinToString()}
                """.trimIndent())
                
                videoRepository.uploadVideo(uri, title, description, visibility, tags)
                    .onSuccess { url ->
                        Log.d("UploadViewModel", "Upload successful: $url")
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            uploadProgress = 0f,
                            lastUploadedUrl = url,
                            error = null
                        )
                    }
                    .onFailure { error ->
                        Log.e("UploadViewModel", "Upload failed", error)
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            uploadProgress = 0f,
                            error = error.message ?: "Upload failed"
                        )
                    }
                
            } catch (e: Exception) {
                Log.e("UploadViewModel", "Error during upload", e)
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadProgress = 0f,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
} 