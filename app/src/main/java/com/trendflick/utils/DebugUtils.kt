package com.trendflick.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class for debugging and logging in the app
 */
object DebugUtils {
    private const val TAG = "TrendFlick"
    private var isDebugMode = true

    /**
     * Set debug mode
     */
    fun setDebugMode(enabled: Boolean) {
        isDebugMode = enabled
    }

    /**
     * Log debug message
     */
    fun d(tag: String, message: String) {
        if (isDebugMode) {
            Log.d("$TAG:$tag", message)
        }
    }

    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebugMode) {
            if (throwable != null) {
                Log.e("$TAG:$tag", message, throwable)
            } else {
                Log.e("$TAG:$tag", message)
            }
        }
    }

    /**
     * Log warning message
     */
    fun w(tag: String, message: String) {
        if (isDebugMode) {
            Log.w("$TAG:$tag", message)
        }
    }

    /**
     * Log info message
     */
    fun i(tag: String, message: String) {
        if (isDebugMode) {
            Log.i("$TAG:$tag", message)
        }
    }

    /**
     * Log verbose message
     */
    fun v(tag: String, message: String) {
        if (isDebugMode) {
            Log.v("$TAG:$tag", message)
        }
    }

    /**
     * Download file from URL
     */
    suspend fun downloadFile(url: String, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    e("DownloadFile", "Server returned HTTP ${connection.responseCode}")
                    return@withContext false
                }

                val inputStream: InputStream = connection.inputStream
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                d("DownloadFile", "File downloaded successfully to ${outputFile.absolutePath}")
                return@withContext true
            } catch (e: IOException) {
                e("DownloadFile", "Error downloading file: ${e.message}", e)
                return@withContext false
            }
        }
    }

    /**
     * Check if URL is valid
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        
        return try {
            URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if file is video
     */
    fun isVideoFile(filePath: String): Boolean {
        val videoExtensions = listOf(".mp4", ".mov", ".avi", ".wmv", ".flv", ".mkv", ".webm")
        return videoExtensions.any { filePath.lowercase().endsWith(it) }
    }

    /**
     * Check if file is image
     */
    fun isImageFile(filePath: String): Boolean {
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
        return imageExtensions.any { filePath.lowercase().endsWith(it) }
    }
}
