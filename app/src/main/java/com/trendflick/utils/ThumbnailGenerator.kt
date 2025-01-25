package com.trendflick.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.Size
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailGenerator {
    private const val TAG = "ThumbnailGenerator"
    private const val THUMBNAIL_WIDTH = 320 // Standard thumbnail width
    private const val THUMBNAIL_HEIGHT = 568 // 16:9 aspect ratio for vertical videos
    private const val COMPRESSION_QUALITY = 85 // Good quality while maintaining small size
    private const val FRAME_POSITION_USEC = 1000000L // 1 second into the video

    suspend fun generateThumbnail(
        context: Context,
        videoUri: Uri,
        did: String,
        customSize: Size? = null
    ): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var outputStream: FileOutputStream? = null
        
        try {
            retriever.setDataSource(context, videoUri)
            
            // Extract frame from video
            val frame = retriever.getFrameAtTime(
                FRAME_POSITION_USEC,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return@withContext null
            
            // Resize frame to thumbnail size
            val width = customSize?.width ?: THUMBNAIL_WIDTH
            val height = customSize?.height ?: THUMBNAIL_HEIGHT
            val scaledBitmap = Bitmap.createScaledBitmap(frame, width, height, true)
            
            // Create thumbnail file under user's DID directory
            val thumbnailFile = createThumbnailFile(context, did)
            outputStream = FileOutputStream(thumbnailFile)
            
            // Save thumbnail with compression
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            
            // Clean up
            scaledBitmap.recycle()
            frame.recycle()
            
            return@withContext thumbnailFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail", e)
            return@withContext null
        } finally {
            try {
                outputStream?.close()
                retriever.release()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing resources", e)
            }
        }
    }

    private fun createThumbnailFile(context: Context, did: String): File {
        val timestamp = System.currentTimeMillis()
        // Create directory structure: thumbnails/did:plc:xyz/
        val userDir = File(context.getExternalFilesDir(null), "thumbnails/$did").apply {
            if (!exists()) mkdirs()
        }
        return File(userDir, "THUMB_$timestamp.jpg")
    }

    fun getThumbnailsForUser(context: Context, did: String): List<File> {
        val userDir = File(context.getExternalFilesDir(null), "thumbnails/$did")
        return if (userDir.exists()) {
            userDir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun cleanupOldThumbnails(context: Context, did: String, maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000L) {
        val userDir = File(context.getExternalFilesDir(null), "thumbnails/$did")
        if (userDir.exists()) {
            val currentTime = System.currentTimeMillis()
            userDir.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > maxAgeMillis) {
                    file.delete()
                }
            }
        }
    }
} 