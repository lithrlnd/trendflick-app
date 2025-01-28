package com.trendflick.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.auth.FirebaseAuth
import com.trendflick.data.model.Video
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.trendflick.data.repository.AtProtocolRepository
import com.google.firebase.storage.StorageException
import java.io.InputStream
import java.io.IOException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.UploadTask
import java.io.File
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
    private val contentResolver: ContentResolver,
    private val context: Context,
    private val atProtocolRepository: AtProtocolRepository
) : VideoRepository {

    private val TAG = "TF_VideoRepo"
    private val videosCollection = firestore.collection("videos")
    
    private suspend fun ensureAuthenticated(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                Log.d(TAG, "🔐 No user found, attempting anonymous auth...")
                val result = auth.signInAnonymously().await()
                Log.d(TAG, """
                    ✅ Anonymous auth successful
                    User ID: ${result.user?.uid}
                    Is Anonymous: ${result.user?.isAnonymous}
                """.trimIndent())
                true
            } else {
                Log.d(TAG, """
                    ✅ Using existing auth
                    User ID: ${auth.currentUser?.uid}
                    Is Anonymous: ${auth.currentUser?.isAnonymous}
                """.trimIndent())
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Auth failed: ${e.message}")
            false
        }
    }
    
    override suspend fun uploadVideo(
        uri: Uri,
        title: String,
        description: String,
        visibility: String,
        tags: List<String>
    ): Result<String> = runCatching {
        Log.d(TAG, "🎬 Starting video upload in background\nDescription: $description")
        
        // First ensure we're authenticated
        if (!ensureAuthenticated()) {
            throw IllegalStateException("Authentication required for upload")
        }

        // Create storage reference
        val timestamp = System.currentTimeMillis()
        val filename = "video_$timestamp.mp4"
        val videoRef = storage.reference.child("videos").child(filename)

        // First upload the file and get the URL
        val downloadUrl = suspendCancellableCoroutine { continuation ->
            try {
                // Use ContentResolver to get input stream from URI
                Log.d(TAG, """
                    🔍 DEBUG Upload Details:
                    URI: $uri
                    Auth Status: ${FirebaseAuth.getInstance().currentUser != null}
                    Storage Path: ${videoRef.path}
                    Storage Bucket: ${storage.app.options.storageBucket}
                """.trimIndent())

                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IOException("Failed to open input stream for URI: $uri")
                
                // Read the bytes and verify we have data
                val bytes = inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) {
                    throw IOException("No data read from URI: $uri")
                }
                
                Log.d(TAG, """
                    📤 Starting upload to Firebase Storage
                    - File size: ${bytes.size} bytes
                    - URI scheme: ${uri.scheme}
                    - URI path: ${uri.path}
                    - Target: ${videoRef.path}
                    - Data read: ${if (bytes.isNotEmpty()) "✅" else "❌"}
                """.trimIndent())
                
                val metadata = StorageMetadata.Builder()
                    .setContentType("video/mp4")
                    .build()
                
                val uploadTask = videoRef.putBytes(bytes, metadata)

                uploadTask
                    .addOnProgressListener { snapshot ->
                        val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
                        Log.d(TAG, """
                            📊 Upload Progress Update:
                            - Progress: $progress%
                            - Bytes Transferred: ${snapshot.bytesTransferred}
                            - Total Bytes: ${snapshot.totalByteCount}
                        """.trimIndent())
                        showUploadNotification(progress.toInt())
                    }
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            val e = task.exception!!
                            val errorMsg = when (e) {
                                is StorageException -> """
                                    ❌ Storage Exception:
                                    - Error Code: ${e.errorCode}
                                    - HTTP Result: ${e.httpResultCode}
                                    - Message: ${e.message}
                                """.trimIndent()
                                else -> """
                                    ❌ Generic Exception:
                                    - Type: ${e.javaClass.name}
                                    - Message: ${e.message}
                                """.trimIndent()
                            }
                            Log.e(TAG, errorMsg)
                            throw e
                        }

                        // First wait for upload to complete and verify
                        val uploadResult = task.result!!
                        if (uploadResult.bytesTransferred != uploadResult.totalByteCount) {
                            throw IOException("Upload incomplete: ${uploadResult.bytesTransferred}/${uploadResult.totalByteCount} bytes")
                        }
                        
                        Log.d(TAG, """
                            ✅ Upload Complete:
                            - Total Bytes: ${uploadResult.totalByteCount}
                            - Storage Path: ${uploadResult.storage.path}
                        """.trimIndent())
                        
                        // Now check metadata exists
                        uploadResult.storage.metadata
                    }
                    .continueWithTask { metadataTask ->
                        if (!metadataTask.isSuccessful) {
                            throw metadataTask.exception!!
                        }
                        
                        val metadata = metadataTask.result
                        Log.d(TAG, """
                            📝 Metadata Verified:
                            - Size: ${metadata.sizeBytes} bytes
                            - Type: ${metadata.contentType}
                            - Created: ${metadata.creationTimeMillis}
                        """.trimIndent())
                        
                        // Only after metadata is verified, get download URL
                        videoRef.downloadUrl
                    }
                    .addOnSuccessListener { uri ->
                        Log.d(TAG, "✅ File upload complete, URL: $uri")
                        continuation.resume(uri.toString())
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }

                continuation.invokeOnCancellation {
                    Log.d(TAG, "Upload cancelled")
                    uploadTask.cancel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start upload: ${e.message}")
                continuation.resumeWithException(e)
            }
        }

        // Now that we have the download URL, save the metadata
        Log.d(TAG, "📝 Upload successful, saving metadata...")
        val video = saveVideoMetadata(
            videoUrl = downloadUrl,
            description = description,
            timestamp = Instant.now().toString(),
            did = timestamp.toString(),
            handle = "lunchbox2001.bsky.social",
            postToBlueSky = false
        )

        // Clear the upload notification
        showUploadNotification(100)
        
        Log.d(TAG, """
            ✨ Upload complete:
            URL: $downloadUrl
            Video ID: ${video.uri}
        """.trimIndent())
        
        return@runCatching downloadUrl
    }

    // Extension function to calculate MD5 hash of ByteArray
    private fun ByteArray.md5(): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(this)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private suspend fun verifyStorageReady() {
        try {
            Log.d(TAG, "🔍 STORAGE: Checking if videos directory exists")
            val videosRef = storage.reference.child("videos")
            
            try {
                // Try to create an empty file in the directory
                val markerRef = videosRef.child(".keep")
                val emptyData = ByteArray(0)
                val metadata = StorageMetadata.Builder()
                    .setContentType("application/x-empty")
                    .build()
                
                markerRef.putBytes(emptyData, metadata).await()
                Log.d(TAG, "✅ STORAGE: Videos directory verified/created")
                
            } catch (e: StorageException) {
                // If we get a "not found" error, the directory doesn't exist
                if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    Log.d(TAG, "📝 STORAGE: Directory doesn't exist, attempting direct upload...")
                    
                    // Try direct upload without checking directory
                    return
                }
                
                // For other errors, log and rethrow
                Log.e(TAG, "❌ STORAGE ERROR: ${e.message} (Code: ${e.errorCode})")
                throw e
            }
            
        } catch (e: Exception) {
            // If we get here, something is fundamentally wrong with storage access
            Log.e(TAG, "❌ STORAGE ERROR: ${e.message}")
            Log.e(TAG, "❌ STORAGE STACK: ${e.stackTraceToString()}")
            throw e
        }
    }

    override suspend fun saveVideoMetadata(
        videoUrl: String,
        description: String,
        timestamp: String,
        did: String,
        handle: String,
        postToBlueSky: Boolean
    ): Video {
        Log.d(TAG, """
            💾 SAVING TO FIREBASE:
            Video URL: $videoUrl
            Description: $description
            Created At: $timestamp
            Handle: $handle
        """.trimIndent())

        try {
            // First verify the file exists in Storage
            val storageRef = storage.getReferenceFromUrl(videoUrl)
            try {
                // This will throw if file doesn't exist
                val metadata = storageRef.metadata.await()
                Log.d(TAG, """
                    ✅ Verified file exists:
                    Size: ${metadata.sizeBytes} bytes
                    Type: ${metadata.contentType}
                    Created: ${metadata.creationTimeMillis}
                """.trimIndent())
            } catch (e: Exception) {
                Log.e(TAG, "❌ File does not exist in Storage: $videoUrl")
                throw IllegalStateException("Video file must exist in Storage before creating metadata")
            }

            val now = Instant.now()
            val videoId = now.toEpochMilli().toString()
            val uri = "video_${videoId}"

            // Create video object with Firebase-specific data
            val video = Video(
                uri = uri,
                videoUrl = videoUrl,
                description = description,
                createdAt = now,
                indexedAt = now,
                sortAt = now,
                did = videoId,
                handle = "lunchbox2001.bsky.social",
                title = if (description.isBlank()) "TrendFlick Video" else description.take(50),
                thumbnailUrl = "",
                username = "lunchbox2001",
                userId = videoId,
                likes = 0,
                comments = 0,
                shares = 0
            )

            // Save to Firestore only if file exists
            videosCollection.document(uri).set(video.toMap()).await()
            
            Log.d(TAG, """
                ✅ SAVED TO FIREBASE:
                🆔 Document ID: $uri
                🎥 Firebase URL: $videoUrl
                📅 Timestamp: $now
                👤 User: ${video.username} (${video.handle})
                📝 Description: $description
            """.trimIndent())
            
            return video
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ FIREBASE SAVE FAILED:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            throw e
        }
    }

    // Helper extension function to convert Video to Map
    private fun Video.toMap(): Map<String, Any> {
        val map = mapOf(
            "uri" to uri,
            "videoUrl" to videoUrl,
            "description" to description,
            "createdAt" to createdAt.toString(),
            "indexedAt" to indexedAt.toString(),
            "sortAt" to sortAt.toEpochMilli(),  // Store as timestamp for sorting
            "did" to did,
            "handle" to handle,
            "title" to title,
            "thumbnailUrl" to thumbnailUrl,
            "username" to username,
            "userId" to userId,
            "likes" to (likes ?: 0),
            "comments" to (comments ?: 0),
            "shares" to (shares ?: 0)
        )
        
        Log.d(TAG, """
            📝 Saving to Firestore:
            Document ID: $uri
            Video URL: $videoUrl
            Created: ${createdAt.toString()}
            Sort Time: ${sortAt.toEpochMilli()}
        """.trimIndent())
        
        return map
    }

    override fun getVideoFeed(): Flow<List<Video>> = callbackFlow {
        Log.d(TAG, "🔄 Setting up video feed listener")
        
        try {
            // First check if we have any videos at all
            val totalCount = videosCollection.get().await().size()
            Log.d(TAG, "📊 Total documents in collection: $totalCount")
            
            // Create a query that gets all videos ordered by creation time
            val registration = videosCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)  // Implement pagination
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "❌ Error in video feed: ${error.message}")
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w(TAG, "⚠️ Snapshot is null")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                try {
                        val videos = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data
                                if (data == null) {
                                    Log.w(TAG, "⚠️ Document ${doc.id} has no data")
                                    return@mapNotNull null
                                }

                            Video(
                                    uri = doc.id,
                                    videoUrl = data["videoUrl"] as? String ?: run {
                                        Log.e(TAG, "❌ Missing videoUrl in doc ${doc.id}")
                                        return@mapNotNull null
                                    },
                                    description = data["description"] as? String ?: "",
                                    createdAt = try {
                                        val createdAtStr = data["createdAt"] as? String
                                        if (createdAtStr == null || createdAtStr == "[select current time]") {
                                            Log.w(TAG, "⚠️ Invalid createdAt in doc ${doc.id}, using current time")
                                            Instant.now()
                                        } else {
                                            Instant.parse(createdAtStr)
                            }
                                } catch (e: Exception) {
                                        Log.e(TAG, "❌ Error parsing createdAt in doc ${doc.id}: ${e.message}")
                                        Instant.now()
                                    },
                                    indexedAt = try {
                                        val indexedAtStr = data["indexedAt"] as? String
                                        if (indexedAtStr == null) {
                                            Log.w(TAG, "⚠️ Invalid indexedAt in doc ${doc.id}, using current time")
                                            Instant.now()
                                        } else {
                                            Instant.parse(indexedAtStr)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Error parsing indexedAt in doc ${doc.id}: ${e.message}")
                                        Instant.now()
                                    },
                                    sortAt = try {
                                        val sortAtStr = data["sortAt"] as? String
                                        if (sortAtStr == null) {
                                            Log.w(TAG, "⚠️ Invalid sortAt in doc ${doc.id}, using current time")
                                            Instant.now()
                                        } else {
                                            Instant.parse(sortAtStr)
                                }
                        } catch (e: Exception) {
                                        Log.e(TAG, "❌ Error parsing sortAt in doc ${doc.id}: ${e.message}")
                                        Instant.now()
                                    },
                                    did = data["did"] as? String ?: "",
                                    handle = data["handle"] as? String ?: "",
                                    title = data["title"] as? String ?: "",
                                    thumbnailUrl = data["thumbnailUrl"] as? String ?: "",
                                    username = data["username"] as? String ?: "",
                                    userId = data["userId"] as? String ?: "",
                                    likes = (data["likes"] as? Number)?.toInt() ?: 0,
                                    comments = (data["comments"] as? Number)?.toInt() ?: 0,
                                    shares = (data["shares"] as? Number)?.toInt() ?: 0
                                ).also { video ->
                                    Log.d(TAG, """
                                        ✅ Parsed video:
                                        🆔 URI: ${video.uri}
                                        🎥 URL: ${video.videoUrl}
                                        📅 Created: ${video.createdAt}
                                        👤 User: ${video.username}
                                    """.trimIndent())
                                }
                                } catch (e: Exception) {
                                Log.e(TAG, """
                                    ❌ Error parsing doc ${doc.id}:
                                    Error: ${e.message}
                                    Stack: ${e.stackTraceToString()}
                                """.trimIndent())
                                    null
                    }
                    }
                        
                        Log.d(TAG, """
                            📊 Feed Update:
                            Total Videos: ${videos.size}
                            Latest Video: ${videos.firstOrNull()?.videoUrl}
                        """.trimIndent())

                    trySend(videos)
                        
                } catch (e: Exception) {
                        Log.e(TAG, """
                            ❌ Error processing feed:
                            Error: ${e.message}
                            Stack: ${e.stackTraceToString()}
                        """.trimIndent())
                        close(e)
                }
            }
            
            awaitClose { 
                Log.d(TAG, "👋 Closing video feed listener")
                registration.remove()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Error setting up feed:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            close(e)
        }
    }

    // Helper extension function to convert Firebase Timestamp to Instant
    private fun com.google.firebase.Timestamp.toInstant(): Instant {
        return Instant.ofEpochSecond(seconds, nanoseconds.toLong())
    }

    override suspend fun likeVideo(videoUri: String) {
        try {
            videosCollection.document(videoUri)
                .update("likes", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to like video: ${e.message}")
        }
    }

    override suspend fun unlikeVideo(videoUri: String) {
        try {
            videosCollection.document(videoUri)
                .update("likes", FieldValue.increment(-1))
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to unlike video: ${e.message}")
        }
    }

    override suspend fun addComment(videoUri: String, comment: String) {
        try {
            videosCollection.document(videoUri)
                .update("comments", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to add comment: ${e.message}")
        }
    }

    override suspend fun shareVideo(videoUri: String) {
        try {
            videosCollection.document(videoUri)
                .update("shares", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to share video: ${e.message}")
        }
    }

    override suspend fun getRelatedVideos(videoUri: String): List<Video> {
        return try {
            videosCollection
                .whereNotEqualTo("uri", videoUri)
                .orderBy("uri")
                .orderBy("sortAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Video::class.java) }
        } catch (e: Exception) {
            throw Exception("Failed to get related videos: ${e.message}")
        }
    }

    override suspend fun getVideos(): List<Video> {
        try {
            Log.d(TAG, "🔄 Getting videos from Firestore...")
            val snapshot = videosCollection
                .orderBy("sortAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Log.d(TAG, """
                📊 Query Results:
                Total Documents: ${snapshot.documents.size}
                Empty?: ${snapshot.isEmpty}
                Metadata: ${snapshot.metadata}
            """.trimIndent())
            
            val videos = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: run {
                        Log.w(TAG, "⚠️ Document ${doc.id} has no data")
                        return@mapNotNull null
                    }
                    
                    // Log raw data for debugging
                    Log.d(TAG, """
                        📄 Raw Document Data for ${doc.id}:
                        ${data.entries.joinToString("\n") { "${it.key}: ${it.value}" }}
                    """.trimIndent())
                    
                    Video(
                        uri = doc.id,
                        videoUrl = data["videoUrl"] as? String ?: run {
                            Log.e(TAG, "❌ Missing videoUrl in doc ${doc.id}")
                            return@mapNotNull null
                        },
                        description = data["description"] as? String ?: "",
                        createdAt = try {
                            val createdAtStr = data["createdAt"] as? String
                            if (createdAtStr == null || createdAtStr == "[select current time]") {
                                Log.w(TAG, "⚠️ Invalid createdAt in doc ${doc.id}, using current time")
                                Instant.now()
                            } else {
                                Instant.parse(createdAtStr)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing createdAt in doc ${doc.id}: ${e.message}")
                            Instant.now()
                        },
                        indexedAt = try {
                            val indexedAtStr = data["indexedAt"] as? String
                            if (indexedAtStr == null) {
                                Log.w(TAG, "⚠️ Invalid indexedAt in doc ${doc.id}, using current time")
                                Instant.now()
                            } else {
                                Instant.parse(indexedAtStr)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing indexedAt in doc ${doc.id}: ${e.message}")
                            Instant.now()
                        },
                        sortAt = try {
                            val sortAtStr = data["sortAt"] as? String
                            if (sortAtStr == null) {
                                Log.w(TAG, "⚠️ Invalid sortAt in doc ${doc.id}, using current time")
                                Instant.now()
                            } else {
                                Instant.parse(sortAtStr)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing sortAt in doc ${doc.id}: ${e.message}")
                            Instant.now()
                        },
                        did = data["did"] as? String ?: "",
                        handle = data["handle"] as? String ?: "",
                        title = data["title"] as? String ?: "",
                        thumbnailUrl = data["thumbnailUrl"] as? String ?: "",
                        username = data["username"] as? String ?: "",
                        userId = data["userId"] as? String ?: "",
                        likes = (data["likes"] as? Number)?.toInt() ?: 0,
                        comments = (data["comments"] as? Number)?.toInt() ?: 0,
                        shares = (data["shares"] as? Number)?.toInt() ?: 0
                    ).also { video ->
                        Log.d(TAG, """
                            ✅ Successfully parsed video:
                            🆔 URI: ${video.uri}
                            🎥 Video URL: ${video.videoUrl}
                            📅 Created: ${video.createdAt}
                            📝 Description: ${video.description}
                            👤 User: ${video.username} (${video.handle})
                        """.trimIndent())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, """
                        ❌ Error parsing doc ${doc.id}:
                        Error: ${e.message}
                        Stack: ${e.stackTraceToString()}
                    """.trimIndent())
                    null
                }
            }
            
            Log.d(TAG, """
                📊 Final Results:
                Total Videos: ${videos.size}
                Video URLs: ${videos.map { it.videoUrl }}
            """.trimIndent())
            
            return videos
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Failed to get videos:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            throw e
        }
    }

    override suspend fun insertVideo(video: Video) {
        videosCollection.document(video.uri).set(video.toMap()).await()
    }

    override suspend fun testVideoInFolder() {
        Log.d(TAG, "🧪 TEST: Starting folder test")
        
        try {
            // Get storage instance
            val storage = FirebaseStorage.getInstance()
            Log.d(TAG, """
                📦 Storage Details:
                Bucket: ${storage.app.options.storageBucket}
                App Name: ${storage.app.name}
            """.trimIndent())
            
            // Use exact path from Firebase Storage
            val videoRef = storage.reference.child("videos/TestVideo.mp4")
            Log.d(TAG, """
                🎥 Video Reference Details:
                Full Path: ${videoRef.path}
                Name: ${videoRef.name}
                Parent: ${videoRef.parent?.path}
                Storage Path: gs://${storage.app.options.storageBucket}/${videoRef.path}
            """.trimIndent())
            
            try {
                // Verify file exists first
                val metadata = videoRef.metadata.await()
                Log.d(TAG, """
                    📄 File Metadata:
                    Size: ${metadata.sizeBytes} bytes
                    Type: ${metadata.contentType}
                    Created: ${metadata.creationTimeMillis}
                    Updated: ${metadata.updatedTimeMillis}
                """.trimIndent())
                
                // Get download URL
                val downloadUrl = videoRef.downloadUrl.await()
                Log.d(TAG, "✅ Got download URL: $downloadUrl")
                
                // Add to feed with test metadata
                val now = Instant.now()
                val video = Video(
                    uri = "test_${now.toEpochMilli()}",
                    videoUrl = downloadUrl.toString(),
                    description = "Test video from Firebase Storage",
                    createdAt = now,
                    indexedAt = now,
                    sortAt = now,
                    did = "test_did",
                    handle = "test.user",
                    title = "Test Video",
                    thumbnailUrl = "",
                    username = "test_user",
                    userId = "test_user_id",
                    likes = 0,
                    comments = 0,
                    shares = 0
                )
                
                // Save to Firestore
                insertVideo(video)
                
                Log.d(TAG, """
                    ✅ TEST SUCCESS:
                    🆔 URI: ${video.uri}
                    🎥 URL: ${video.videoUrl}
                    📅 Created: ${video.createdAt}
                    👤 User: ${video.username}
                """.trimIndent())
                
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ TEST FAILED:
                    Error: ${e.message}
                    Stack: ${e.stackTraceToString()}
                """.trimIndent())
                throw e
            }
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ TEST ERROR:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            throw e
        }
    }

    // Test function to verify Firestore saving
    suspend fun testSaveVideo() {
        try {
            val timestamp = Instant.now()
            val videoId = timestamp.toEpochMilli().toString()
            
            val testVideo = Video(
                uri = "video_$videoId",  // Firebase-style URI
                videoUrl = "https://firebasestorage.googleapis.com/v0/b/trendflick-d7188.appspot.com/o/videos%2FTestVideo.mp4",  // Actual Firebase URL
                description = "Test video from Firebase",
                createdAt = timestamp,
                indexedAt = timestamp,
                sortAt = timestamp,
                did = videoId,  // Use videoId for consistency
                handle = "lunchbox2001.bsky.social",  // Your actual handle
                title = "TrendFlick Test Video",
                thumbnailUrl = "",
                username = "lunchbox2001",  // Just the username part
                userId = videoId,  // Use videoId for consistency
                likes = 0,
                comments = 0,
                shares = 0
            )
            
            val docRef = videosCollection.document(testVideo.uri)
            
            // Create map with server timestamp
            val videoData = mapOf(
                "uri" to testVideo.uri,
                "videoUrl" to testVideo.videoUrl,
                "description" to testVideo.description,
                "createdAt" to testVideo.createdAt.toString(),
                "indexedAt" to FieldValue.serverTimestamp(),
                "sortAt" to testVideo.sortAt.toEpochMilli(),
                "did" to testVideo.did,
                "handle" to testVideo.handle,
                "title" to testVideo.title,
                "thumbnailUrl" to testVideo.thumbnailUrl,
                "username" to testVideo.username,
                "userId" to testVideo.userId,
                "likes" to testVideo.likes,
                "comments" to testVideo.comments,
                "shares" to testVideo.shares
            )
            
            docRef.set(videoData).await()
            
            // Verify save with enhanced logging
            val savedDoc = docRef.get().await()
            Log.d(TAG, """
                ✅ TEST SAVE:
                Document: ${savedDoc.id}
                Exists: ${savedDoc.exists()}
                Video ID: $videoId
                User ID: ${savedDoc.data?.get("userId")}
                DID: ${savedDoc.data?.get("did")}
                Handle: ${savedDoc.data?.get("handle")}
                Username: ${savedDoc.data?.get("username")}
                Data: ${savedDoc.data}
            """.trimIndent())
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ TEST SAVE FAILED:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
        }
    }

    // Test function to simulate a real upload
    suspend fun testRealUpload() {
        Log.d(TAG, """
            🧪 SIMULATING REAL UPLOAD
            ========================
            📱 Starting upload simulation
            🎯 Target: TestVideo.mp4
            ========================
        """.trimIndent())
        
        try {
            // 1. Get reference to test video
            val testVideoRef = storage.reference.child("videos/TestVideo.mp4")
            
            // 2. Get metadata to verify file exists
            val metadata = testVideoRef.metadata.await()
            Log.d(TAG, """
                ✅ TEST VIDEO FOUND
                ========================
                📊 Size: ${metadata.sizeBytes} bytes
                📁 Type: ${metadata.contentType}
                ⏰ Created: ${metadata.creationTimeMillis}
                ========================
            """.trimIndent())
            
            // 3. Get download URL
            val downloadUrl = testVideoRef.downloadUrl.await()
            Log.d(TAG, """
                🔗 GOT DOWNLOAD URL
                ========================
                🌐 URL: $downloadUrl
                ========================
            """.trimIndent())
            
            // 4. Save to Firestore exactly like a real upload
            val video = saveVideoMetadata(
                videoUrl = downloadUrl.toString(),
                description = "Test Upload Simulation",
                timestamp = Instant.now().toString(),
                did = "test-did",
                handle = "lunchbox2001.bsky.social",  // Updated to your handle
                postToBlueSky = false
            )
            
            Log.d(TAG, """
                ✨ UPLOAD SIMULATION COMPLETE
                ========================
                📝 Video Details:
                - URI: ${video.uri}
                - URL: ${video.videoUrl}
                - Created: ${video.createdAt}
                ========================
            """.trimIndent())
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ SIMULATION FAILED
                ========================
                💥 Error: ${e.message}
                📍 Stack Trace:
                ${e.stackTraceToString()}
                ========================
            """.trimIndent())
            throw e
        }
    }

    override suspend fun testFolderAccess() {
        Log.d(TAG, """
            🧪 TESTING FOLDER ACCESS
            ========================
            📁 Testing videos/ folder
        """.trimIndent())
        
        try {
            // 1. Get reference to videos folder
            val videosRef = storage.reference.child("videos")
            
            // 2. Try to list contents
            val listResult = videosRef.listAll().await()
            Log.d(TAG, """
                📂 FOLDER CONTENTS:
                - Files: ${listResult.items.size}
                - Prefixes: ${listResult.prefixes.size}
                ========================
            """.trimIndent())
            
            // 3. Try to create a test file IN the videos folder
            val testFileRef = videosRef.child("folder_test.txt")
            val testData = "Testing folder access".toByteArray()
            
            Log.d(TAG, "📝 Attempting to write test file to videos/folder_test.txt")
            testFileRef.putBytes(testData).await()
            
            Log.d(TAG, "✅ Successfully wrote test file to videos folder!")
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ FOLDER TEST FAILED
                ========================
                💥 Error: ${e.message}
                📍 Stack Trace:
                ${e.stackTraceToString()}
                ========================
            """.trimIndent())
            throw e
        }
    }

    // Add this function after uploadVideo
    suspend fun testRootUpload(videoUri: Uri): String {
        Log.d(TAG, "🧪 Testing upload to root directory")

        try {
            // Create storage reference to root
            val filename = "test_${System.currentTimeMillis()}.mp4"
            val storageRef = storage.reference.child(filename)

            // Simple metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("video/mp4")
                .build()

            Log.d(TAG, "📁 Uploading to: /$filename")

            // Upload directly to root
            val uploadTask = storageRef.putFile(videoUri, metadata)
            
            // Wait for completion
            val downloadUrl = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "❌ Root upload failed: ${task.exception?.message}")
                    throw task.exception!!
                }
                storageRef.downloadUrl
            }.await().toString()

            Log.d(TAG, "✅ Root upload success: $downloadUrl")
            return downloadUrl

        } catch (e: Exception) {
            Log.e(TAG, "❌ Root upload error: ${e.message}")
            throw e
        }
    }

    // Add after your existing test functions
    suspend fun testSmallFileUpload() {
        Log.d(TAG, """
            🧪 STARTING FIREBASE STORAGE TEST
            ===============================
            1️⃣ Verifying Storage Connection:
            Bucket: ${storage.app.options.storageBucket}
            App Name: ${storage.app.name}
            Project ID: ${storage.app.options.projectId}
            ===============================
        """.trimIndent())

        try {
            // Step 1: List root contents
            Log.d(TAG, "2️⃣ Listing root directory contents...")
            val rootList = storage.reference.listAll().await()
            Log.d(TAG, """
                📂 Root Contents:
                Files: ${rootList.items.joinToString(", ") { it.name }}
                Folders: ${rootList.prefixes.joinToString(", ") { it.name }}
                ===============================
            """.trimIndent())

            // Step 2: Create a tiny test file
            val testData = "test".toByteArray()
            val timestamp = System.currentTimeMillis()
            val filename = "test_$timestamp.txt"
            
            // Step 3: Get reference and log details
            val storageRef = storage.reference.child(filename)
            Log.d(TAG, """
                3️⃣ Preparing Upload:
                Path: ${storageRef.path}
                Full Path: ${storageRef.bucket}/${storageRef.path}
                Size: ${testData.size} bytes
                ===============================
            """.trimIndent())

            // Step 4: Set minimal metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("text/plain")
                .setCustomMetadata("test", "true")
                .build()

            // Step 5: Upload with detailed error handling
            try {
                Log.d(TAG, "4️⃣ Starting upload...")
                val uploadTask = storageRef.putBytes(testData, metadata)
                
                uploadTask.addOnProgressListener { snapshot ->
                    val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
                    Log.d(TAG, "📤 Progress: ${progress.toInt()}%")
                }

                val downloadUrl = uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        val exception = task.exception
                        if (exception is StorageException) {
                            Log.e(TAG, """
                                ❌ Storage Exception:
                                Code: ${exception.errorCode}
                                Message: ${exception.message}
                                ===============================
                            """.trimIndent())
                        }
                        throw task.exception!!
                    }
                    storageRef.downloadUrl
                }.await().toString()

                Log.d(TAG, """
                    ✅ Upload Success!
                    URL: $downloadUrl
                    ===============================
                """.trimIndent())

            } catch (e: StorageException) {
                Log.e(TAG, """
                    ❌ Upload Failed (StorageException):
                    Error Code: ${e.errorCode}
                    Message: ${e.message}
                    ===============================
                """.trimIndent())
                throw e
            }

        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Test Failed:
                Type: ${e.javaClass.simpleName}
                Message: ${e.message}
                Stack: ${e.stackTraceToString()}
                ===============================
            """.trimIndent())
            throw e
        }
    }

    // Add function to clean up test videos
    suspend fun cleanupTestVideos() {
        Log.d(TAG, "🧹 Starting cleanup of test videos")
        try {
            // Query all test videos or videos with TestVideo.mp4 URL
            val testVideos = videosCollection
                .whereEqualTo("isTest", true)
                .get()
                .await()

            // Also get videos with TestVideo.mp4 in URL
            val oldTestVideos = videosCollection
                .whereGreaterThanOrEqualTo("url", "TestVideo.mp4")
                .get()
                .await()

            val toDelete = (testVideos.documents + oldTestVideos.documents).distinctBy { it.id }
            
            Log.d(TAG, "🗑️ Found ${toDelete.size} test videos to delete")
            
            toDelete.forEach { doc ->
                try {
                    doc.reference.delete().await()
                    Log.d(TAG, "✅ Deleted test video: ${doc.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to delete test video ${doc.id}: ${e.message}")
                }
            }
            
            Log.d(TAG, "✨ Cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup failed: ${e.message}")
        }
    }

    // Add cleanup function for ghost entries
    suspend fun cleanupGhostEntries() {
        Log.d(TAG, "🧹 Starting ghost entry cleanup")
        
        try {
            val snapshot = videosCollection.get().await()
            var cleanedCount = 0
            
            for (doc in snapshot.documents) {
                val videoUrl = doc.getString("videoUrl") ?: continue
                
                try {
                    // Try to get reference and metadata
                    val ref = storage.getReferenceFromUrl(videoUrl)
                    ref.metadata.await()
                    
                    Log.d(TAG, "✅ Verified: ${doc.id} -> $videoUrl")
                    
                } catch (e: Exception) {
                    // If we can't get metadata, file doesn't exist
                    Log.d(TAG, "🗑️ Cleaning ghost entry: ${doc.id} -> $videoUrl")
                    doc.reference.delete().await()
                    cleanedCount++
                }
            }
            
            Log.d(TAG, """
                ✨ Ghost cleanup complete:
                Checked: ${snapshot.size()} entries
                Cleaned: $cleanedCount entries
            """.trimIndent())
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Ghost cleanup failed:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
        }
    }

    // Add function to ensure videos directory exists
    private suspend fun ensureVideosDirectoryExists() {
        // Firebase Storage creates directories automatically when uploading files
        // No need to explicitly create the videos directory
        Log.d(TAG, """
            📁 DEBUG: Videos path will be created automatically during upload
            - Storage Bucket: ${storage.app.options.storageBucket}
            - Target Path: /videos
        """.trimIndent())
    }

    suspend fun testRootTextUpload() {
        Log.d(TAG, "🧪 Testing root upload with text file")
        
        try {
            // Create a small text file
            val testData = "Test upload ${System.currentTimeMillis()}".toByteArray()
            val filename = "test_${System.currentTimeMillis()}.txt"
            
            // Create reference directly in root
            val fileRef = storage.reference.child(filename)
            
            // Upload with minimal metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("text/plain")
                .build()
                
            val uploadTask = fileRef.putBytes(testData, metadata)
            
            // Wait for result
            val downloadUrl = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                fileRef.downloadUrl
            }.await().toString()
            
            Log.d(TAG, "✅ Test upload successful: $downloadUrl")
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Test upload failed:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            throw e
        }
    }

    suspend fun testVideosTextUpload() {
        Log.d(TAG, "🧪 Testing upload to videos directory")
        
        try {
            // Create a small text file
            val testData = "Test upload ${System.currentTimeMillis()}".toByteArray()
            val filename = "test_${System.currentTimeMillis()}.txt"
            
            // Create reference in videos directory
            val fileRef = storage.reference.child("videos/$filename")
            
            Log.d(TAG, """
                📁 Upload Details:
                - Full path: ${fileRef.path}
                - Parent: ${fileRef.parent?.path}
                - Name: ${fileRef.name}
            """.trimIndent())
            
            // Upload with minimal metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("text/plain")
                .build()
                
            val uploadTask = fileRef.putBytes(testData, metadata)
            
            // Wait for result
            val downloadUrl = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                fileRef.downloadUrl
            }.await().toString()
            
            Log.d(TAG, "✅ Test upload successful: $downloadUrl")
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Test upload failed:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            throw e
        }
    }

    // Add this function to register manually uploaded videos
    suspend fun registerManualUpload(filename: String, description: String = "Manually uploaded video") {
        try {
            Log.d(TAG, "📝 Registering manual upload: $filename")
            
            // Get the storage reference
            val videoRef = storage.reference.child("videos/$filename")
            
            // Get the download URL
            val downloadUrl = videoRef.downloadUrl.await().toString()
            
            // Create video metadata
            val now = Instant.now()
            val videoId = now.toEpochMilli().toString()
            
            val video = Video(
                uri = "manual_${videoId}",
                videoUrl = downloadUrl,
                description = description,
                createdAt = now,
                indexedAt = now,
                sortAt = now,
                did = videoId,
                handle = "lunchbox2001.bsky.social",
                title = description.take(50),
                thumbnailUrl = "",
                username = "lunchbox2001",
                userId = videoId,
                likes = 0,
                comments = 0,
                shares = 0
            )
            
            // Save to Firestore
            insertVideo(video)
            
            Log.d(TAG, """
                ✅ Manual upload registered:
                URI: ${video.uri}
                URL: $downloadUrl
                Created: $now
            """.trimIndent())
            
        } catch (e: Exception) {
            Log.e(TAG, """
                ❌ Failed to register manual upload:
                Error: ${e.message}
                Stack: ${e.stackTraceToString()}
            """.trimIndent())
            throw e
        }
    }

    private fun showUploadNotification(progress: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "video_upload_channel"
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Video Upload Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows video upload progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Uploading Video")
            .setContentText("Upload in progress")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
        
        if (progress < 100) {
            // Show determinate progress
            builder.setProgress(100, progress, false)
        } else {
            // Upload complete
            builder.setContentText("Upload complete")
                .setProgress(0, 0, false)
                .setOngoing(false)
        }
        
        notificationManager.notify(UPLOAD_NOTIFICATION_ID, builder.build())
    }

    companion object {
        private const val UPLOAD_NOTIFICATION_ID = 1001
    }
} 