package com.trendflick.ui.screens.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import android.util.Log
import java.io.File
import kotlinx.coroutines.delay
import com.trendflick.data.repository.BlueskyRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.json.JSONObject

data class CreateFlickUiState(
    val isLoading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isPostSuccessful: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateFlickViewModel @Inject constructor(
    application: Application,
    private val atProtocolRepository: AtProtocolRepository,
    private val blueskyRepository: BlueskyRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CreateFlickUiState())
    val uiState: StateFlow<CreateFlickUiState> = _uiState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val TAG = "TF_CreateFlickViewModel"

    fun createFlick(videoUri: Uri, description: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, """
                    🎬 CREATE FLICK START
                    Description: $description
                    VideoUri: $videoUri
                """.trimIndent())
                
                _uiState.value = CreateFlickUiState(isLoading = true)
                
                // Get the actual file path from Uri
                val context = getApplication<Application>()
                val originalVideoFile = when {
                    videoUri.scheme == "file" -> File(videoUri.path!!)
                    videoUri.scheme == "content" -> {
                        val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                        context.contentResolver.openInputStream(videoUri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }
                    else -> throw IllegalStateException("Unsupported URI scheme: ${videoUri.scheme}")
                }

                try {
                    // Check video duration
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(originalVideoFile.absolutePath)
                    val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                    
                    if (durationMs > 60000) { // 60 seconds in milliseconds
                        throw IllegalStateException("Video is too long. Maximum duration is 60 seconds.")
                    }

                    // First ensure we have a valid session
                    val sessionValid = atProtocolRepository.ensureValidSession()
                    if (!sessionValid) {
                        throw IllegalStateException("BlueSky session is invalid. Please log in again.")
                    }

                    // Upload video to BlueSky
                    val uploadResult = blueskyRepository.uploadVideo(originalVideoFile, description)
                    
                    if (uploadResult.error != null) {
                        throw IllegalStateException(uploadResult.error)
                    }

                    if (uploadResult.blobRef == null) {
                        throw IllegalStateException("Failed to upload video: No blob reference returned")
                    }

                    // Create BlueSky post with video embed
                    val postResult = atProtocolRepository.createPost(
                        record = mapOf(
                            "did" to (atProtocolRepository.getDid() ?: throw IllegalStateException("No DID found")),
                            "\$type" to "app.bsky.feed.post",
                            "text" to description,
                            "createdAt" to java.time.Instant.now().toString(),
                            "embed" to mapOf(
                                "\$type" to "app.bsky.embed.video",
                                "video" to uploadResult.blobRef
                            )
                        )
                    )

                    Log.d(TAG, """
                        ✅ Post created successfully:
                        URI: ${uploadResult.postUri}
                        Description: $description
                    """.trimIndent())
                    _uiState.value = CreateFlickUiState(isPostSuccessful = true)

                } finally {
                    // Clean up temp files
                    try {
                        if (videoUri.scheme == "content") {
                            originalVideoFile.delete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete temp files: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ Error creating flick:
                    Error: ${e.message}
                    Stack: ${e.stackTraceToString()}
                """.trimIndent())
                _uiState.value = CreateFlickUiState(error = when {
                    e.message?.contains("too long") == true -> "Video is too long. Maximum duration is 60 seconds."
                    e.message?.contains("session is invalid") == true -> "Please log in to BlueSky again."
                    else -> e.message ?: "Failed to create post"
                })
            }
        }
    }

    fun searchBlueSkyUsers(query: String) {
        viewModelScope.launch {
            try {
                val users = atProtocolRepository.searchUsers(query)
                _suggestions.value = users.map { "${it.handle}" }
            } catch (e: Exception) {
                _suggestions.value = emptyList()
            }
        }
    }

    fun searchHashtags(query: String) {
        viewModelScope.launch {
            try {
                _suggestions.value = listOf(
                    "trending",
                    "viral",
                    "fyp",
                    "foryou",
                    "trendflick",
                    "video",
                    "reels"
                ).filter { it.contains(query, ignoreCase = true) }
            } catch (e: Exception) {
                _suggestions.value = emptyList()
            }
        }
    }
} 