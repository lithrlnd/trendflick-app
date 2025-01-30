package com.trendflick.ui.screens.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.repository.UserSearchResult
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
import com.trendflick.data.model.TrendingHashtag
import com.trendflick.domain.model.UserProfile
import com.trendflick.domain.model.Hashtag

data class CreateFlickUiState(
    val isLoading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isPostSuccessful: Boolean = false,
    val error: String? = null,
    val userSuggestions: List<UserSearchResult> = emptyList(),
    val hashtagSuggestions: List<TrendingHashtag> = emptyList()
)

@HiltViewModel
class CreateFlickViewModel @Inject constructor(
    application: Application,
    private val atProtocolRepository: AtProtocolRepository,
    private val blueskyRepository: BlueskyRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CreateFlickUiState())
    val uiState: StateFlow<CreateFlickUiState> = _uiState.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    private val _userSuggestions = MutableStateFlow<List<UserProfile>>(emptyList())
    val userSuggestions: StateFlow<List<UserProfile>> = _userSuggestions.asStateFlow()

    private val _hashtagSuggestions = MutableStateFlow<List<Hashtag>>(emptyList())
    val hashtagSuggestions: StateFlow<List<Hashtag>> = _hashtagSuggestions.asStateFlow()

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

                    // Parse facets for mentions and hashtags in description
                    val facets = try {
                        atProtocolRepository.parseFacets(description)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse facets: ${e.message}")
                        null
                    }

                    // Create BlueSky post with video embed and facets
                    val record = mutableMapOf(
                        "did" to (atProtocolRepository.getDid() ?: throw IllegalStateException("No DID found")),
                        "\$type" to "app.bsky.feed.post",
                        "text" to description,
                        "createdAt" to java.time.Instant.now().toString(),
                        "embed" to mapOf(
                            "\$type" to "app.bsky.embed.video",
                            "video" to uploadResult.blobRef
                        )
                    )
                    
                    // Add facets if available
                    if (facets != null) {
                        record["facets"] = facets
                    }

                    val postResult = atProtocolRepository.createPost(record)

                    Log.d(TAG, """
                        ✅ Post created successfully:
                        URI: ${uploadResult.postUri}
                        Description: $description
                        Facets: ${facets?.size ?: 0}
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

    fun updateQuery(newText: String) {
        viewModelScope.launch {
            _currentQuery.value = newText
            
            // Check if we're looking for mentions or hashtags
            when {
                newText.contains("@") -> {
                    val query = newText.substringAfterLast("@")
                    if (query.isNotEmpty()) {
                        searchUsers(query)
                    }
                }
                newText.contains("#") -> {
                    val query = newText.substringAfterLast("#")
                    if (query.isNotEmpty()) {
                        searchHashtags(query)
                    }
                }
            }
        }
    }

    private suspend fun searchUsers(query: String) {
        try {
            val users = atProtocolRepository.searchHandles(query)
            _userSuggestions.value = users.map { result ->
                UserProfile(
                    did = result.did,
                    handle = result.handle,
                    displayName = result.displayName,
                    avatar = result.avatar
                )
            }
            _hashtagSuggestions.value = emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: ${e.message}")
            _userSuggestions.value = emptyList()
            _hashtagSuggestions.value = emptyList()
        }
    }

    private suspend fun searchHashtags(query: String) {
        try {
            val hashtags = atProtocolRepository.searchHashtags(query)
            _userSuggestions.value = emptyList()
            _hashtagSuggestions.value = hashtags.map { trending ->
                Hashtag(
                    tag = trending.tag,
                    postCount = 0  // Default to 0 since we don't have this info from BlueSky
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching hashtags: ${e.message}")
            _userSuggestions.value = emptyList()
            _hashtagSuggestions.value = emptyList()
        }
    }

    fun createTextPost(text: String) {
        viewModelScope.launch {
            try {
                _uiState.value = CreateFlickUiState(isLoading = true)
                
                // Ensure valid session
                if (!atProtocolRepository.ensureValidSession()) {
                    throw IllegalStateException("BlueSky session is invalid. Please log in again.")
                }

                // Parse facets (mentions, hashtags, links)
                val facets = atProtocolRepository.parseFacets(text)
                
                // Create the post with facets
                atProtocolRepository.createPost(
                    text = text,
                    timestamp = Instant.now().toString(),
                    facets = facets
                )

                Log.d(TAG, """
                    ✅ Text post created successfully:
                    Text: $text
                    Facets: ${facets?.size ?: 0}
                """.trimIndent())
                
                _uiState.value = CreateFlickUiState(isPostSuccessful = true)
                
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ Error creating text post:
                    Error: ${e.message}
                    Stack: ${e.stackTraceToString()}
                """.trimIndent())
                
                _uiState.value = CreateFlickUiState(
                    error = e.message ?: "Failed to create post"
                )
            }
        }
    }

    fun insertMention(user: UserProfile): String {
        val currentText = _currentQuery.value
        val lastAtIndex = currentText.lastIndexOf('@')
        return if (lastAtIndex >= 0) {
            currentText.substring(0, lastAtIndex) + "@${user.handle} "
        } else {
            currentText
        }
    }

    fun insertHashtag(hashtag: Hashtag): String {
        val currentText = _currentQuery.value
        val lastHashIndex = currentText.lastIndexOf('#')
        return if (lastHashIndex >= 0) {
            currentText.substring(0, lastHashIndex) + "#${hashtag.tag} "
        } else {
            currentText
        }
    }

    fun clearSuggestions() {
        _userSuggestions.value = emptyList()
        _hashtagSuggestions.value = emptyList()
    }
} 