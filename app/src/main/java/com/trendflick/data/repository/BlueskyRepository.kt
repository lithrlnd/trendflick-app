@file:OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)

package com.trendflick.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MultipartBody
import java.io.File
import kotlinx.coroutines.delay
import okhttp3.RequestBody
import kotlin.contracts.ExperimentalContracts
import kotlin.experimental.ExperimentalTypeInference
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaExtractor
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import java.nio.ByteBuffer
import android.content.Context
import android.net.Uri
import java.io.FileOutputStream
import android.media.MediaMetadataRetriever
import org.json.JSONArray
import dagger.hilt.android.qualifiers.ApplicationContext
import android.media.MediaCodecInfo.CodecCapabilities
import android.view.Surface
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.graphics.Matrix
import java.io.IOException
import com.trendflick.data.model.Video

data class VideoUploadResult(
    val blobRef: JSONObject?,
    val postUri: String? = null,
    val error: String? = null
)

@Singleton
class BlueskyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val atProtocolRepository: AtProtocolRepository
) {
    private val TAG = "TF_VideoRepo"
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val API_URL = "https://bsky.social"
    
    // Max file size (1MB as per Bluesky docs)
    private val MAX_FILE_SIZE = 1 * 1024 * 1024L 
    
    private suspend fun compressVideo(inputFile: File): File {
        return withContext(Dispatchers.IO) {
            val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.mp4")
            
            try {
                // Get video metadata
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(inputFile.absolutePath)
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 1920
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 1080
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                retriever.release()

                // Calculate new dimensions (maintain aspect ratio but much smaller)
                val maxDimension = 360 // Reduced from 480 for better compression
                val ratio = width.toFloat() / height.toFloat()
                val (newWidth, newHeight) = if (width > height) {
                    maxDimension to (maxDimension / ratio).toInt()
                } else {
                    (maxDimension * ratio).toInt() to maxDimension
                }

                // Create decoder
                val extractor = MediaExtractor()
                extractor.setDataSource(inputFile.absolutePath)
                
                // Find video track
                var videoTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                        videoTrackIndex = i
                        break
                    }
                }

                if (videoTrackIndex == -1) {
                    throw IllegalStateException("No video track found")
                }

                // Select video track and get format
                extractor.selectTrack(videoTrackIndex)
                val inputFormat = extractor.getTrackFormat(videoTrackIndex)
                
                // Create decoder
                val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME) ?: "video/avc")
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()

                // Create encoder format
                val outputFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, newWidth, newHeight)
                outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 250_000) // 250Kbps
                outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20)
                outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
                outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, CodecCapabilities.COLOR_FormatSurface)
                outputFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                outputFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3)

                // Create encoder
                val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                
                // Create input surface for encoder
                val encoderSurface = encoder.createInputSurface()
                
                // Create output surface for decoder using SurfaceTexture
                val surfaceTexture = SurfaceTexture(0).apply {
                    setDefaultBufferSize(newWidth, newHeight)
                }
                val decoderSurface = Surface(surfaceTexture)
                decoder.configure(inputFormat, decoderSurface, null, 0)

                // Start both codecs
                encoder.start()
                decoder.start()

                // Create muxer
                val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var muxerStarted = false
                var muxerTrackIndex = -1

                // Compression loop
                val bufferInfo = MediaCodec.BufferInfo()
                var isEOS = false
                var generatedIndex = 0

                try {
                    while (!isEOS) {
                        if (!isEOS) {
                            val inputBufferId = decoder.dequeueInputBuffer(10000L)
                            if (inputBufferId >= 0) {
                                val inputBuffer = decoder.getInputBuffer(inputBufferId)
                                val sampleSize = extractor.readSampleData(inputBuffer!!, 0)

                                if (sampleSize < 0) {
                                    decoder.queueInputBuffer(
                                        inputBufferId,
                                        0,
                                        0,
                                        0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    isEOS = true
                                } else {
                                    decoder.queueInputBuffer(
                                        inputBufferId,
                                        0,
                                        sampleSize,
                                        extractor.sampleTime,
                                        0
                                    )
                                    extractor.advance()
                                }
                            }
                        }

                        // Handle decoder output/encoder input using surface
                        var decoderOutputAvailable = true
                        var encoderOutputAvailable = true

                        while (decoderOutputAvailable || encoderOutputAvailable) {
                            // Drain encoder
                            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                            if (encoderStatus >= 0) {
                                val encodedData = encoder.getOutputBuffer(encoderStatus)
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                    bufferInfo.size = 0
                                }

                                if (bufferInfo.size > 0) {
                                    if (!muxerStarted) {
                                        val format = encoder.getOutputFormat(encoderStatus)
                                        muxerTrackIndex = muxer.addTrack(format)
                                        muxer.start()
                                        muxerStarted = true
                                    }

                                    encodedData?.position(bufferInfo.offset)
                                    encodedData?.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(muxerTrackIndex, encodedData!!, bufferInfo)
                                }

                                encoder.releaseOutputBuffer(encoderStatus, false)
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    break
                                }
                            } else if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                encoderOutputAvailable = false
                            }

                            // Drain decoder
                            val decoderStatus = decoder.dequeueOutputBuffer(bufferInfo, 10000L)
                            if (decoderStatus >= 0) {
                                val doRender = bufferInfo.size != 0
                                decoder.releaseOutputBuffer(decoderStatus, doRender)
                                if (doRender) {
                                    surfaceTexture.updateTexImage()
                                }
                            } else if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                decoderOutputAvailable = false
                            }
                        }
                    }
                } finally {
                    // Clean up
                    decoder.stop()
                    decoder.release()
                    encoder.stop()
                    encoder.release()
                    extractor.release()
                    decoderSurface.release()
                    surfaceTexture.release()
                    if (muxerStarted) {
                        muxer.stop()
                        muxer.release()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error compressing video: ${e.message}", e)
                throw e
            }
            
            outputFile
        }
    }
    
    suspend fun uploadVideo(videoFile: File, description: String): VideoUploadResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check video duration
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoFile.absolutePath)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                retriever.release()

                if (durationMs > 60000) { // 60 seconds max
                    return@withContext VideoUploadResult(
                        blobRef = null,
                        error = "Video duration exceeds 60 seconds limit (${durationMs/1000} seconds). Please trim your video to 60 seconds or less."
                    )
                }

                // Check file size (50MB limit according to Bluesky docs)
                val maxFileSize = 50 * 1024 * 1024L // 50MB
                if (videoFile.length() > maxFileSize) {
                    Log.d(TAG, "Video file too large (${videoFile.length()} bytes), compressing...")
                    try {
                        val compressedFile = compressVideo(videoFile)
                        if (compressedFile.length() > maxFileSize) {
                            return@withContext VideoUploadResult(
                                blobRef = null,
                                error = "Video file too large even after compression: ${compressedFile.length()} bytes. Maximum allowed: $maxFileSize bytes"
                            )
                        }
                        return@withContext uploadVideoInternal(compressedFile, description)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error compressing video: ${e.message}", e)
                        return@withContext VideoUploadResult(
                            blobRef = null,
                            error = "Failed to compress video: ${e.message}"
                        )
                    }
                }

                // If file is within limits, upload directly
                uploadVideoInternal(videoFile, description)
            } catch (e: Exception) {
                Log.e(TAG, "Error during video upload: ${e.message}", e)
                VideoUploadResult(
                    blobRef = null,
                    error = e.message
                )
            }
        }
    }

    private suspend fun uploadVideoInternal(videoFile: File, description: String): VideoUploadResult {
        // Get current session from AT Protocol
        val session = atProtocolRepository.getCurrentSession()
            ?: throw IllegalStateException("Not logged in to Bluesky")
        
        // Get access token from AT Protocol repository
        val accessToken = atProtocolRepository.refreshSession()
            .getOrNull()?.accessJwt
            ?: throw IllegalStateException("Failed to get access token")

        Log.d(TAG, """
            📤 Starting video upload:
            File size: ${videoFile.length()}
            File path: ${videoFile.absolutePath}
            User: ${session.handle}
        """.trimIndent())

        // Upload video as blob
        val videoBytes = videoFile.readBytes()
        val blobUploadRequest = Request.Builder()
            .url("$API_URL/xrpc/com.atproto.repo.uploadBlob")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "video/mp4")
            .post(videoBytes.toRequestBody("video/mp4".toMediaTypeOrNull()))
            .build()

        var blobRef: JSONObject? = null
        
        client.newCall(blobUploadRequest).execute().use { response ->
            val responseBody = response.body?.string()
            Log.d(TAG, """
                📤 Blob upload response:
                Code: ${response.code}
                Body: $responseBody
            """.trimIndent())

            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to upload video blob: ${response.code} - ${responseBody ?: "Unknown error"}")
            }

            blobRef = JSONObject(responseBody ?: "{}").getJSONObject("blob")
        }

        // Create post with video embed
        val now = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        
        // Get video metadata for embed
        val videoMetadata = MediaMetadataRetriever().apply {
            setDataSource(videoFile.absolutePath)
        }.use { retriever ->
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 1080
            width to height
        }
        
        val postRecord = JSONObject().apply {
            put("\$type", "app.bsky.feed.post")
            put("text", description)
            put("createdAt", now)
            put("did", session.did)
            
            // Use proper video embed type
            val embed = JSONObject().apply {
                put("\$type", "app.bsky.embed.video")
                put("video", blobRef)
                
                // Add video metadata
                val aspectRatio = JSONObject().apply {
                    put("width", videoMetadata.first)
                    put("height", videoMetadata.second)
                }
                put("aspectRatio", aspectRatio)
                
                // Add alt text for accessibility
                put("alt", "Video: $description")
            }
            put("embed", embed)
            
            // Add feed generator hints for vertical video feed
            if (videoMetadata.second > videoMetadata.first) {
                put("feedGeneratorHints", JSONObject().apply {
                    put("isVerticalVideo", true)
                })
            }
        }

        // Create the full request body with repo and record
        val createPostBody = JSONObject().apply {
            put("repo", session.did)
            put("collection", "app.bsky.feed.post")
            put("record", postRecord)
        }

        val createPostRequest = Request.Builder()
            .url("$API_URL/xrpc/com.atproto.repo.createRecord")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(createPostBody.toString().toRequestBody(JSON))
            .build()

        var postUri: String? = null

        client.newCall(createPostRequest).execute().use { response ->
            val responseBody = response.body?.string()
            Log.d(TAG, """
                📤 Create post response:
                Code: ${response.code}
                Body: $responseBody
            """.trimIndent())

            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to create post: ${response.code} - ${responseBody ?: "Unknown error"}")
            }

            postUri = JSONObject(responseBody ?: "{}").getString("uri")
        }

        return VideoUploadResult(
            blobRef = blobRef,
            postUri = postUri
        )
    }

    suspend fun getMediaPosts(cursor: String? = null): List<Video> {
        try {
            Log.d(TAG, "🔄 Starting getMediaPosts with cursor: $cursor")
            
            // Get current session and refresh to ensure we have a valid token
            val accessToken = atProtocolRepository.refreshSession()
                .getOrNull()?.accessJwt
                ?: throw IllegalStateException("Failed to get access token")
            
            Log.d(TAG, "✅ Got access token successfully")
            
            // Use "what's hot" feed generator
            val query = JSONObject().apply {
                put("feed", "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.generator/whats-hot")
                put("limit", 100)
                cursor?.let { put("cursor", it) }
            }
            
            Log.d(TAG, "📤 Sending query to Bluesky API: ${query.toString(2)}")

            val response = client.newCall(
                Request.Builder()
                    .url("$API_URL/xrpc/app.bsky.feed.getFeed")  // Changed to getFeed endpoint
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(query.toString().toRequestBody(JSON))
                    .build()
            ).execute()

            val responseBody = response.body?.string()
            Log.d(TAG, """
                📥 Bluesky API Response:
                Code: ${response.code}
                Headers: ${response.headers}
                Body: $responseBody
            """.trimIndent())

            if (!response.isSuccessful) {
                throw IOException("Failed to fetch media posts: ${response.code}")
            }

            val body = JSONObject(responseBody ?: throw IOException("Empty response"))
            val feed = body.getJSONArray("feed")
            
            Log.d(TAG, "📊 Found ${feed.length()} total items in feed")
            
            val mediaList = mutableListOf<Video>()
            
            for (index in 0 until feed.length()) {
                try {
                    val item = feed.getJSONObject(index)
                    val post = item.getJSONObject("post")
                    val record = post.getJSONObject("record")
                    
                    // First check for direct embeds
                    val embed = record.optJSONObject("embed")
                    
                    // Then check for embed in a repost
                    val repost = record.optJSONObject("embed")?.optJSONObject("record")
                    val repostEmbed = repost?.optJSONObject("embed")
                    
                    val effectiveEmbed = embed ?: repostEmbed
                    val effectiveRecord = if (repost != null) repost else record
                    val effectivePost = if (repost != null) repost else post
                    
                    Log.d(TAG, """
                        🔍 Examining post ${index + 1}/${feed.length()}:
                        URI: ${effectivePost.optString("uri", "unknown")}
                        Type: ${effectiveEmbed?.optString("\$type", "none")}
                        Has Embed: ${effectiveEmbed != null}
                        Is Repost: ${repost != null}
                        Raw Embed: ${effectiveEmbed?.toString(2)}
                    """.trimIndent())
                    
                    // Check for images first
                    if (effectiveEmbed?.getString("\$type") == "app.bsky.embed.images") {
                        val images = effectiveEmbed.getJSONArray("images")
                        if (images.length() > 0) {
                            val image = images.getJSONObject(0).getJSONObject("image")
                            val ref = image.getJSONObject("ref").getString("\$link")
                            val mediaUrl = "https://cdn.bsky.social/img/feed_fullsize/plain/$ref@jpeg"
                            
                            val aspectRatio = run {
                                val img = images.getJSONObject(0)
                                val ratio = img.optJSONObject("aspectRatio")
                                if (ratio != null) {
                                    ratio.getInt("width").toFloat() / ratio.getInt("height")
                                } else 1f
                            }

                            Log.d(TAG, """
                                ✅ Found image post:
                                URL: $mediaUrl
                                Aspect Ratio: $aspectRatio
                                Text: ${effectiveRecord.optString("text", "")}
                            """.trimIndent())

                            mediaList.add(Video(
                                uri = effectivePost.getString("uri"),
                                did = effectivePost.getJSONObject("author").getString("did"),
                                handle = effectivePost.getJSONObject("author").getString("handle"),
                                videoUrl = "",
                                imageUrl = mediaUrl,
                                isImage = true,
                                description = effectiveRecord.getString("text"),
                                createdAt = Instant.parse(effectiveRecord.getString("createdAt")),
                                indexedAt = Instant.parse(effectivePost.getString("indexedAt")),
                                sortAt = Instant.parse(effectivePost.getString("indexedAt")),
                                title = "",
                                thumbnailUrl = "",
                                username = effectivePost.getJSONObject("author").getString("displayName"),
                                userId = effectivePost.getJSONObject("author").getString("did"),
                                likes = effectivePost.optJSONObject("likeCount")?.optInt("count") ?: 0,
                                comments = effectivePost.optJSONObject("replyCount")?.optInt("count") ?: 0,
                                shares = effectivePost.optJSONObject("repostCount")?.optInt("count") ?: 0,
                                aspectRatio = aspectRatio
                            ))
                        }
                    }
                    // Also check for videos
                    else if (effectiveEmbed?.getString("\$type") == "app.bsky.embed.video") {
                        val video = effectiveEmbed.getJSONObject("video")
                        val ref = video.getJSONObject("ref").getString("\$link")
                        val mediaUrl = "https://cdn.bsky.social/video/plain/$ref"
                        
                        val aspectRatio = run {
                            val ratio = video.optJSONObject("aspectRatio")
                            if (ratio != null) {
                                ratio.getInt("width").toFloat() / ratio.getInt("height")
                            } else 16f/9f
                        }

                        Log.d(TAG, """
                            ✅ Found video post:
                            URL: $mediaUrl
                            Aspect Ratio: $aspectRatio
                            Text: ${effectiveRecord.optString("text", "")}
                        """.trimIndent())

                        mediaList.add(Video(
                            uri = effectivePost.getString("uri"),
                            did = effectivePost.getJSONObject("author").getString("did"),
                            handle = effectivePost.getJSONObject("author").getString("handle"),
                            videoUrl = mediaUrl,
                            imageUrl = "",
                            isImage = false,
                            description = effectiveRecord.getString("text"),
                            createdAt = Instant.parse(effectiveRecord.getString("createdAt")),
                            indexedAt = Instant.parse(effectivePost.getString("indexedAt")),
                            sortAt = Instant.parse(effectivePost.getString("indexedAt")),
                            title = "",
                            thumbnailUrl = "",
                            username = effectivePost.getJSONObject("author").getString("displayName"),
                            userId = effectivePost.getJSONObject("author").getString("did"),
                            likes = effectivePost.optJSONObject("likeCount")?.optInt("count") ?: 0,
                            comments = effectivePost.optJSONObject("replyCount")?.optInt("count") ?: 0,
                            shares = effectivePost.optJSONObject("repostCount")?.optInt("count") ?: 0,
                            aspectRatio = aspectRatio
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing post at index $index: ${e.message}")
                    Log.e(TAG, e.stackTraceToString())
                }
            }

            Log.d(TAG, """
                📊 Media processing complete:
                Total posts processed: ${feed.length()}
                Media posts found: ${mediaList.size}
                Images: ${mediaList.count { it.isImage }}
                Videos: ${mediaList.count { !it.isImage }}
            """.trimIndent())

            return mediaList

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch media posts: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }
} 


