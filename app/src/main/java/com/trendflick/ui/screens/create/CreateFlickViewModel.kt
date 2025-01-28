package com.trendflick.ui.screens.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.repository.VideoRepository
import com.trendflick.data.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import android.util.Log

data class CreateFlickUiState(
    val isLoading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isPostSuccessful: Boolean = false,
    val video: Video? = null,
    val error: String? = null
)

@HiltViewModel
class CreateFlickViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val atProtocolRepository: AtProtocolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateFlickUiState())
    val uiState: StateFlow<CreateFlickUiState> = _uiState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val TAG = "TF_VideoRepo"

    private fun extractFacets(text: String): List<Map<String, Any>> {
        val facets = mutableListOf<Map<String, Any>>()
        val textBytes = text.encodeToByteArray()
        
        // Find mentions (@handle.bsky.social)
        val mentionRegex = Regex("@([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)")
        mentionRegex.findAll(text).forEach { matchResult ->
            val handle = matchResult.groupValues[1]
            val byteRange = matchResult.range
            val byteStart = text.substring(0, byteRange.first).encodeToByteArray().size
            val byteEnd = text.substring(0, byteRange.last + 1).encodeToByteArray().size
            
            // Get the DID for the handle using identity.resolveHandle
            viewModelScope.launch {
                try {
                    val did = atProtocolRepository.identityResolveHandle(handle)
                    facets.add(mapOf(
                        "index" to mapOf(
                            "byteStart" to byteStart,
                            "byteEnd" to byteEnd
                        ),
                        "features" to listOf(mapOf(
                            "\$type" to "app.bsky.richtext.facet#mention",
                            "did" to did
                        ))
                    ))
                } catch (e: Exception) {
                    // Handle error - invalid handle
                }
            }
        }

        // Find hashtags (#tag)
        val hashtagRegex = Regex("#([a-zA-Z0-9]+)")
        hashtagRegex.findAll(text).forEach { matchResult ->
            val tag = matchResult.groupValues[1]
            val byteRange = matchResult.range
            val byteStart = text.substring(0, byteRange.first).encodeToByteArray().size
            val byteEnd = text.substring(0, byteRange.last + 1).encodeToByteArray().size
            
            facets.add(mapOf(
                "index" to mapOf(
                    "byteStart" to byteStart,
                    "byteEnd" to byteEnd
                ),
                "features" to listOf(mapOf(
                    "\$type" to "app.bsky.richtext.facet#tag",
                    "tag" to tag
                ))
            ))
        }

        // Find URLs
        val urlRegex = Regex("https?://[^\\s]+")
        urlRegex.findAll(text).forEach { matchResult ->
            val url = matchResult.value
            val byteRange = matchResult.range
            val byteStart = text.substring(0, byteRange.first).encodeToByteArray().size
            val byteEnd = text.substring(0, byteRange.last + 1).encodeToByteArray().size
            
            facets.add(mapOf(
                "index" to mapOf(
                    "byteStart" to byteStart,
                    "byteEnd" to byteEnd
                ),
                "features" to listOf(mapOf(
                    "\$type" to "app.bsky.richtext.facet#link",
                    "uri" to url
                ))
            ))
        }

        return facets
    }

    fun searchBlueSkyUsers(query: String) {
        viewModelScope.launch {
            try {
                // Use AtProtocolRepository to search for users
                val users = atProtocolRepository.searchUsers(query)
                _suggestions.value = users.map { "${it.handle}" } // Include full handle with domain
            } catch (e: Exception) {
                // Handle error silently - keep existing suggestions
                _suggestions.value = emptyList()
            }
        }
    }

    fun searchHashtags(query: String) {
        viewModelScope.launch {
            try {
                // Fallback to local list since BlueSky doesn't have a hashtag search API yet
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

    fun createFlick(videoUri: Uri, description: String, postToBlueSky: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, """
                    🎬 CREATE FLICK START
                    Description: $description
                    PostToBlueSky: $postToBlueSky
                """.trimIndent())
                
                _uiState.value = CreateFlickUiState(isLoading = true)
                
                // 1. Upload video to Firebase Storage
                Log.d(TAG, "📤 UPLOAD: Starting video upload to Firebase")
                val videoUrl = uploadVideo(videoUri)
                Log.d(TAG, "📤 UPLOAD: Success - URL: $videoUrl")
                
                // 2. Get user info - AT Protocol only needed if posting to BlueSky
                val currentUser = if (postToBlueSky) {
                    atProtocolRepository.getCurrentSession()
                        ?: throw IllegalStateException("AT Protocol authentication required to post to BlueSky")
                } else null
                
                Log.d(TAG, if (postToBlueSky) {
                    "👤 USER: DID=${currentUser?.did}, Handle=${currentUser?.handle}"
                } else {
                    "👤 USER: Posting to TrendFlick only (no BlueSky)"
                })
                
                // 3. Save video metadata
                val timestamp = Instant.now().toString()
                Log.d(TAG, "💾 METADATA: Saving with timestamp=$timestamp")
                val video = videoRepository.saveVideoMetadata(
                    videoUrl = videoUrl,
                    description = description,
                    timestamp = timestamp,
                    did = currentUser?.did ?: "",  // Empty string if null
                    handle = currentUser?.handle ?: "",  // Empty string if null
                    postToBlueSky = postToBlueSky
                )
                Log.d(TAG, "💾 METADATA: Saved successfully - URI=${video.uri}")

                // 4. Post to BlueSky if selected
                if (postToBlueSky) {
                    try {
                val facets = extractFacets(description)
                val record = mapOf<String, Any>(
                    "\$type" to "app.bsky.feed.post",
                            "text" to "$description\n\n🎬 Coming soon to Android! Follow us for updates.",
                    "createdAt" to timestamp,
                    "facets" to facets,
                    "embed" to mapOf<String, Any>(
                        "\$type" to "app.bsky.embed.external",
                        "external" to mapOf(
                                    "uri" to "https://trendflick.app/flick/${video.uri}",
                                    "title" to "Watch on TrendFlick (Coming Soon)",
                            "description" to description,
                                    "thumb" to "" // Optional thumbnail URL
                            )
                        )
                    )
                val postResult = atProtocolRepository.createPost(record)
                        _uiState.value = CreateFlickUiState(isPostSuccessful = true, video = video)
                    } catch (e: Exception) {
                        _uiState.value = CreateFlickUiState(error = "Failed to post to BlueSky: ${e.message}", video = video)
                    }
                } else {
                    _uiState.value = CreateFlickUiState(isPostSuccessful = true, video = video)
                }
                Log.d(TAG, "✅ CREATE FLICK: Completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR: ${e.message}")
                Log.e(TAG, "❌ STACK: ${e.stackTraceToString()}")
                _uiState.value = CreateFlickUiState(error = e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun uploadVideo(uri: Uri): String {
        return try {
            val result = videoRepository.uploadVideo(
                uri = uri,
                title = "Flick ${System.currentTimeMillis()}",
                description = "Created with TrendFlick",
                visibility = "public",
                tags = listOf("trendflick", "flick")
            )
            result.getOrThrow()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload video", e)
            throw e
        }
    }
} 