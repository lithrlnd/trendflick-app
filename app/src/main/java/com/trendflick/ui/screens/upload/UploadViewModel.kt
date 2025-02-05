package com.trendflick.ui.screens.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.model.Video
import com.trendflick.data.repository.VideoRepository
import com.trendflick.data.repository.VideoRepositoryImpl
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.utils.ThumbnailGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import java.util.UUID
import java.time.Instant
import android.media.MediaExtractor
import android.media.MediaCodec
import android.media.MediaFormat
import android.provider.MediaStore
import java.io.File
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val atProtocolRepository: AtProtocolRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Initial)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    private val TAG = "TF_VideoRepo"

    init {
        // Enable Firebase Storage debug logging
        FirebaseStorage.getInstance().apply {
            Log.d(TAG, "📝 Enabling Firebase Storage debug logging")
        }
        
        // Run initial Firebase access test
        testFirebaseAccess()
    }

    fun setPermissionsGranted(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    fun uploadVideo(
        videoUri: Uri,
        description: String,
        postToBlueSky: Boolean = true,
        playbackSpeed: Float = 1f
    ) {
        viewModelScope.launch {
            try {
                // Add detailed logging for video URI
                Log.d(TAG, """
                    🎥 Video Upload Details:
                    URI: $videoUri
                    URI Path: ${videoUri.path}
                    URI Scheme: ${videoUri.scheme}
                    Content Type: ${context.contentResolver.getType(videoUri)}
                    File Size: ${context.contentResolver.openInputStream(videoUri)?.available() ?: -1} bytes
                    Storage Bucket: ${FirebaseStorage.getInstance().app.options.storageBucket}
                """.trimIndent())

                Log.d(TAG, """
                    🎬 UPLOAD START
                    Description: $description
                    PostToBlueSky: $postToBlueSky
                    PlaybackSpeed: $playbackSpeed
                """.trimIndent())
                
                _uploadState.value = UploadState.Uploading(0f)

                // 1. Process video with selected speed if needed
                val processedVideoUri = if (playbackSpeed != 1f) {
                    Log.d(TAG, "🎥 PROCESSING: Adjusting video speed to ${playbackSpeed}x")
                    processVideoSpeed(videoUri, playbackSpeed)
                } else {
                    videoUri
                }

                // 2. Upload video to Firebase Storage
                Log.d(TAG, "📤 UPLOAD: Starting video upload to Firebase")
                val result = videoRepository.uploadVideo(
                    uri = processedVideoUri,
                    title = description.take(50),
                    description = description,
                    visibility = "public",
                    tags = listOf()
                )
                
                val videoUrl = result.getOrThrow()
                Log.d(TAG, "📤 UPLOAD: Success - URL: $videoUrl")

                // 3. Save to Firestore without BlueSky metadata first
                val video = videoRepository.saveVideoMetadata(
                    videoUrl = videoUrl,
                    description = if (!postToBlueSky && description.isBlank()) "TrendFlick Video" else description,
                    timestamp = Instant.now().toString(),
                    did = "",  // Empty for non-BlueSky uploads
                    handle = "",  // Empty for non-BlueSky uploads
                    postToBlueSky = false
                )

                // 4. Only attempt BlueSky post if requested
                if (postToBlueSky) {
                    try {
                        Log.d(TAG, "🦋 BLUESKY: Posting to BlueSky")
                        val did = atProtocolRepository.getDid()
                        val handle = atProtocolRepository.getHandle()
                        
                        // Create BlueSky post with video URL
                        val record = mapOf(
                            "text" to description,
                            "createdAt" to Instant.now().toString(),
                            "embed" to mapOf(
                                "\$type" to "app.bsky.embed.video",
                                "video" to mapOf(
                                    "ref" to mapOf(
                                        "\$link" to videoUrl
                                    ),
                                    "aspectRatio" to mapOf(
                                        "width" to 16,
                                        "height" to 9
                                    ),
                                    "alt" to description
                                )
                            )
                        )
                        
                        val postResult = atProtocolRepository.createPost(record)
                        Log.d(TAG, "🦋 BLUESKY: Post created - URI: ${postResult.uri}")
                        
                        // Update video with BlueSky metadata
                        videoRepository.saveVideoMetadata(
                            videoUrl = videoUrl,
                            description = description,
                            timestamp = Instant.now().toString(),
                            did = did,
                            handle = handle,
                            postToBlueSky = true
                        )
                    } catch (e: Exception) {
                        // BlueSky post failed but video upload succeeded
                        Log.w(TAG, "⚠️ BlueSky post failed: ${e.message}")
                    }
                }

                _uploadState.value = UploadState.Success(video)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Upload failed", e)
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

    private suspend fun processVideoSpeed(videoUri: Uri, speed: Float): Uri {
        return withContext(Dispatchers.IO) {
            val mediaExtractor = MediaExtractor()
            val inputPath = getRealPathFromUri(context, videoUri)
            val outputPath = "${context.cacheDir}/processed_${System.currentTimeMillis()}.mp4"
            
            try {
                mediaExtractor.setDataSource(inputPath)
                
                val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                val format = mediaExtractor.getTrackFormat(0)
                
                // Adjust format for speed
                format.setFloat("playback-speed-rate", speed)
                
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                
                // Process video frames...
                // This is a simplified version. In production, you'd need proper video processing
                
                Uri.fromFile(File(outputPath))
            } finally {
                mediaExtractor.release()
            }
        }
    }

    private fun getRealPathFromUri(context: Context, uri: Uri): String {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        
        return cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            it.moveToFirst()
            it.getString(columnIndex)
        } ?: throw IllegalArgumentException("Invalid URI")
    }

    fun testFirebaseStorage() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔥 Testing Firebase Storage")
                val storageRef = FirebaseStorage.getInstance().reference
                Log.d(TAG, "✅ Storage Reference: ${storageRef.path}")
                
                // Try to list root contents
                storageRef.listAll()
                    .addOnSuccessListener { result ->
                        Log.d(TAG, """
                            ✅ Storage Access Successful:
                            Items: ${result.items.size}
                            Prefixes: ${result.prefixes.size}
                            Paths: ${result.prefixes.map { it.path }}
                        """.trimIndent())
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Storage Access Failed: ${e.message}")
                        Log.e(TAG, "❌ Error Type: ${e.javaClass.name}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firebase Test Failed: ${e.message}")
                Log.e(TAG, "❌ Stack: ${e.stackTraceToString()}")
            }
        }
    }

    fun testFirebaseAccess() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔥 TESTING FIREBASE ACCESS")
                
                // 1. Get storage instance and check configuration
                val storage = FirebaseStorage.getInstance()
                Log.d(TAG, """
                    📦 Storage Configuration:
                    - Bucket: ${storage.app.options.storageBucket}
                    - Project ID: ${storage.app.options.projectId}
                    - App ID: ${storage.app.options.applicationId}
                """.trimIndent())
                
                // 2. Try to access root and videos folder
                val rootRef = storage.reference
                val videosRef = storage.reference.child("videos")
                Log.d(TAG, """
                    📁 References:
                    - Root path: ${rootRef.path}
                    - Videos path: ${videosRef.path}
                """.trimIndent())
                
                // 3. List contents of root and videos folder
                rootRef.listAll()
                    .addOnSuccessListener { result ->
                        Log.d(TAG, """
                            📂 Root Contents:
                            - Items: ${result.items.joinToString { it.name }}
                            - Folders: ${result.prefixes.joinToString { it.path }}
                        """.trimIndent())
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Root listing failed: ${e.message}")
                    }
                
                videosRef.listAll()
                    .addOnSuccessListener { result ->
                        Log.d(TAG, """
                            📂 Videos Folder Contents:
                            - Items: ${result.items.joinToString { it.name }}
                            - Folders: ${result.prefixes.joinToString { it.path }}
                        """.trimIndent())
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Videos folder listing failed: ${e.message}")
                    }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firebase Test Failed: ${e.message}")
                Log.e(TAG, "❌ Stack: ${e.stackTraceToString()}")
            }
        }
    }

    fun addTestVideo(videoUrl: String, description: String = "Test Video") {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📝 Adding test video to Firestore")
                val timestamp = Instant.now().toString()
                
                val video = videoRepository.saveVideoMetadata(
                    videoUrl = videoUrl,
                    description = description,
                    timestamp = timestamp,
                    did = "test-user",
                    handle = "test.user",
                    postToBlueSky = false
                )
                
                Log.d(TAG, "✅ Test video added successfully: ${video.uri}")
                _uploadState.value = UploadState.Success(video)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to add test video: ${e.message}")
                _uploadState.value = UploadState.Error(e.message ?: "Failed to add test video")
            }
        }
    }

    fun addManualTestVideo() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📝 Starting manual test video upload")
                
                // 1. Get storage instance and reference
                val storage = FirebaseStorage.getInstance()
                val videoRef = storage.reference.child("videos/TestVideo.mp4")
                Log.d(TAG, "✅ Got video reference: ${videoRef.path}")
                
                // 2. Get the download URL
                videoRef.downloadUrl
                    .addOnSuccessListener { downloadUrl ->
                        Log.d(TAG, "✅ Got download URL: $downloadUrl")
                        
                        viewModelScope.launch {
                            try {
                                // 3. Create video metadata
                                val now = Instant.now()
                                val video = videoRepository.saveVideoMetadata(
                                    videoUrl = downloadUrl.toString(),
                                    description = "Test Video Upload",
                                    timestamp = now.toString(),
                                    did = "test-did",
                                    handle = "test.bsky.social",
                                    postToBlueSky = false
                                )
                                
                                Log.d(TAG, """
                                    ✅ SUCCESS: Video added to feed
                                    URI: ${video.uri}
                                    URL: ${video.videoUrl}
                                    Created: ${video.createdAt}
                                    Description: ${video.description}
                                    Handle: ${video.handle}
                                """.trimIndent())
                                
                                _uploadState.value = UploadState.Success(video)
                            } catch (e: Exception) {
                                val error = "Failed to save video metadata: ${e.message}"
                                Log.e(TAG, "❌ $error")
                                Log.e(TAG, "❌ Stack: ${e.stackTraceToString()}")
                                _uploadState.value = UploadState.Error(error)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        val error = "Failed to get download URL: ${e.message}"
                        Log.e(TAG, "❌ $error")
                        Log.e(TAG, "❌ Stack: ${e.stackTraceToString()}")
                        _uploadState.value = UploadState.Error(error)
                    }
                
            } catch (e: Exception) {
                val error = "Failed to start video upload: ${e.message}"
                Log.e(TAG, "❌ $error")
                Log.e(TAG, "❌ Stack: ${e.stackTraceToString()}")
                _uploadState.value = UploadState.Error(error)
            }
        }
    }

    fun testVideoInFolder() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 TEST: Starting folder test")
                
                // Get storage instance
                val storage = FirebaseStorage.getInstance()
                Log.d(TAG, "📦 Storage bucket: ${storage.app.options.storageBucket}")
                
                // List contents of videos folder
                val videosRef = storage.reference.child("videos")
                videosRef.listAll()
                    .addOnSuccessListener { result ->
                        Log.d(TAG, """
                            📁 Videos folder contents:
                            Files: ${result.items.size}
                            Items: ${result.items.map { it.name }}
                        """.trimIndent())
                        
                        // If we found any videos, try to get URL of first one
                        result.items.firstOrNull()?.let { videoRef ->
                            Log.d(TAG, "🎥 Found video: ${videoRef.name}")
                            
                            // Get download URL
                            videoRef.downloadUrl
                                .addOnSuccessListener { downloadUrl ->
                                    Log.d(TAG, "✅ Got download URL: $downloadUrl")
                                    
                                    // Add to feed
                                    viewModelScope.launch {
                                        try {
                                            val video = videoRepository.saveVideoMetadata(
                                                videoUrl = downloadUrl.toString(),
                                                description = "Video from folder: ${videoRef.name}",
                                                timestamp = Instant.now().toString(),
                                                did = "test-did",
                                                handle = "test.bsky.social",
                                                postToBlueSky = false
                                            )
                                            
                                            Log.d(TAG, """
                                                ✅ Added to feed:
                                                Name: ${videoRef.name}
                                                URL: ${video.videoUrl}
                                                URI: ${video.uri}
                                            """.trimIndent())
                                            
                                            _uploadState.value = UploadState.Success(video)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ Failed to save to feed: ${e.message}")
                                            _uploadState.value = UploadState.Error("Failed to save to feed: ${e.message}")
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "❌ Failed to get URL: ${e.message}")
                                }
                        } ?: run {
                            Log.e(TAG, "❌ No videos found in folder")
                            _uploadState.value = UploadState.Error("No videos found in folder")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Failed to list folder: ${e.message}")
                        _uploadState.value = UploadState.Error("Failed to list folder: ${e.message}")
                    }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Test failed: ${e.message}")
                _uploadState.value = UploadState.Error("Test failed: ${e.message}")
            }
        }
    }

    fun testRootUpload(videoUri: Uri) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading(0f)
                
                Log.d(TAG, "🧪 Testing root upload")
                val downloadUrl = (videoRepository as VideoRepositoryImpl).testRootUpload(videoUri)
                
                // Save metadata as usual
                val video = videoRepository.saveVideoMetadata(
                    videoUrl = downloadUrl,
                    description = "Test Root Upload",
                    timestamp = Instant.now().toString(),
                    did = "test-did",
                    handle = "test.bsky.social",
                    postToBlueSky = false
                )
                
                _uploadState.value = UploadState.Success(video)
                Log.d(TAG, "✅ Root upload test complete")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Root upload test failed: ${e.message}")
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun testSmallFileUpload() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 Starting small file upload test")
                (videoRepository as VideoRepositoryImpl).testSmallFileUpload()
                Log.d(TAG, "✅ Small file test complete")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Small file test failed: ${e.message}")
            }
        }
    }

    fun testDirectUpload(uri: Uri) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading(0f)
                
                Log.d(TAG, """
                    🎥 DIRECT UPLOAD TEST
                    ===================
                    URI: $uri
                    URI Path: ${uri.path}
                    URI Scheme: ${uri.scheme}
                    Content Type: ${context.contentResolver.getType(uri)}
                    File Size: ${context.contentResolver.openInputStream(uri)?.available() ?: -1} bytes
                """.trimIndent())
                
                // Upload directly to root
                val timestamp = System.currentTimeMillis()
                val filename = "direct_test_${timestamp}.mp4"
                val storage = FirebaseStorage.getInstance()
                val videoRef = storage.reference.child(filename)
                
                Log.d(TAG, """
                    📁 Storage Details:
                    Bucket: ${storage.app.options.storageBucket}
                    Full Path: ${videoRef.path}
                    Storage URL: gs://${storage.app.options.storageBucket}/${videoRef.path}
                """.trimIndent())
                
                // Create metadata
                val metadata = StorageMetadata.Builder()
                    .setContentType("video/mp4")
                    .setCustomMetadata("test", "true")
                    .setCustomMetadata("timestamp", timestamp.toString())
                    .build()
                
                // Upload with progress monitoring
                val uploadTask = videoRef.putFile(uri, metadata)
                
                uploadTask.addOnProgressListener { snapshot ->
                    val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
                    _uploadState.value = UploadState.Uploading(progress.toFloat() / 100f)
                    Log.d(TAG, "📤 Upload progress: $progress%")
                }
                
                val downloadUrl = uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        Log.e(TAG, "❌ Upload failed: ${task.exception?.message}")
                        throw task.exception!!
                    }
                    videoRef.downloadUrl
                }.await().toString()
                
                Log.d(TAG, """
                    ✅ UPLOAD SUCCESS
                    ===================
                    Download URL: $downloadUrl
                    File: $filename
                    Size: ${context.contentResolver.openInputStream(uri)?.available() ?: -1} bytes
                """.trimIndent())
                
                // Save metadata
                val video = videoRepository.saveVideoMetadata(
                    videoUrl = downloadUrl,
                    description = "Direct upload test",
                    timestamp = System.currentTimeMillis().toString(),
                    did = "test-did",
                    handle = "test.bsky.social",
                    postToBlueSky = false
                )
                
                _uploadState.value = UploadState.Success(video)
                
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ DIRECT UPLOAD FAILED
                    ===================
                    Error: ${e.message}
                    Type: ${e.javaClass.simpleName}
                    Stack: ${e.stackTraceToString()}
                """.trimIndent())
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

    private suspend fun uploadVideo(uri: Uri): String {
        return try {
            val result = videoRepository.uploadVideo(
                uri = uri,
                title = "Upload ${System.currentTimeMillis()}",
                description = "Uploaded with TrendFlick",
                visibility = "public",
                tags = listOf("trendflick", "upload")
            )
            result.getOrThrow()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload video", e)
            throw e
        }
    }
}

sealed class UploadState {
    object Initial : UploadState()
    data class Uploading(val progress: Float = 0f) : UploadState()
    data class Success(
        val video: Video,
        val postUri: String? = null
    ) : UploadState()
    data class Error(val message: String) : UploadState()
} 