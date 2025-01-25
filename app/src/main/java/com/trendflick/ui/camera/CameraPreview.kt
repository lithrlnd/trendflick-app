package com.trendflick.ui.camera

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraPreview(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private val TAG = "CameraPreview"
    
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var currentRecording: Recording? = null
    private var isRecording by mutableStateOf(false)
    private var recordingStartTime: Long = 0
    private var mediaRecorder: MediaRecorder? = null
    private var cameraProvider: ProcessCameraProvider? = null

    data class Recording(
        val outputFile: File,
        val mediaRecorder: MediaRecorder
    )

    suspend fun startCamera() = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Setup preview use case
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Bind preview use case
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )

                // Initialize MediaRecorder
                mediaRecorder = MediaRecorder(context)

                continuation.resume(Unit)
                
                Log.d(TAG, "✅ Camera setup complete")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start camera: ${e.message}")
                continuation.resume(Unit)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun startRecording() {
        if (isRecording) return

        val recording = Recording(
            outputFile = createVideoFile(),
            mediaRecorder = setupVideoRecorder(context)
        )
        currentRecording = recording
        
        try {
            recording.mediaRecorder.start()
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            
            Log.d(TAG, """
                🎥 RECORDING STARTED:
                Output: ${recording.outputFile.absolutePath}
                Format: MP4 (H264/AAC)
                Resolution: 1080x1920
                Framerate: 30fps
                Video Bitrate: 10Mbps
                Audio Bitrate: 128kbps
            """.trimIndent())
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start recording: ${e.message}")
            stopRecording()
        }
    }

    fun stopRecording(onVideoReady: ((String) -> Unit)? = null) {
        if (!isRecording) return

        currentRecording?.let { recording ->
            try {
                recording.mediaRecorder.stop()
                recording.mediaRecorder.release()
                
                // Process the recorded video
                processVideo(recording.outputFile) { finalPath ->
                    onVideoReady?.invoke(finalPath)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error stopping recording: ${e.message}")
                recording.outputFile.delete()
            } finally {
                currentRecording = null
                isRecording = false
            }
        }
    }

    private fun setupVideoRecorder(context: Context): MediaRecorder {
        return MediaRecorder(context).apply {
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            
            // TikTok-style output format and encoding
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            
            // Video settings optimized for social sharing
            setVideoEncodingBitRate(10_000_000) // 10Mbps
            setVideoFrameRate(30)
            setVideoSize(1080, 1920) // Portrait mode, standard social media size
            
            // Audio settings
            setAudioEncodingBitRate(128_000) // 128kbps
            setAudioSamplingRate(44100) // 44.1kHz
            
            // Generate temp file in cache directory
            val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
            val cacheFile = File(context.cacheDir, "TrendFlick_TEMP_$timestamp.mp4")
            setOutputFile(cacheFile.absolutePath)
            
            // Prepare the recorder
            prepare()
        }
    }

    private fun processVideo(tempFile: File, onProcessed: (String) -> Unit) {
        try {
            // Verify the video meets minimum requirements
            val retriever = MediaMetadataRetriever().apply {
                setDataSource(tempFile.absolutePath)
            }

            // Get duration in milliseconds
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            
            // Get dimensions
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            
            // Get rotation
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0

            retriever.release()

            // Log video details
            Log.d(TAG, """
                📹 VIDEO PROCESSED:
                Duration: ${duration}ms
                Resolution: ${width}x${height}
                Rotation: $rotation°
                Size: ${tempFile.length() / 1024}KB
                Path: ${tempFile.absolutePath}
            """.trimIndent())

            // Verify minimum duration (3 seconds)
            if (duration < 3000) {
                Log.e(TAG, "❌ Video too short (${duration}ms), minimum 3 seconds required")
                tempFile.delete()
                return
            }

            // Move to final location
            val finalFile = File(context.getExternalFilesDir(null), tempFile.name)
            if (tempFile.renameTo(finalFile)) {
                Log.d(TAG, "✅ Video saved to: ${finalFile.absolutePath}")
                onProcessed(finalFile.absolutePath)
            } else {
                Log.e(TAG, "❌ Failed to move video to final location")
                tempFile.delete()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing video: ${e.message}")
            tempFile.delete()
        }
    }

    private fun createVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        return File(context.cacheDir, "TrendFlick_$timestamp.mp4")
    }

    fun release() {
        try {
            // Release camera resources
            camera?.let { cam ->
                if (cam.cameraInfo.hasFlashUnit()) {
                    cam.cameraControl.enableTorch(false)
                }
            }
            
            // Unbind all use cases
            cameraProvider?.unbindAll()
            
            // Release MediaRecorder
            mediaRecorder?.release()
            currentRecording?.mediaRecorder?.release()
            
            // Clear references
            camera = null
            preview = null
            mediaRecorder = null
            cameraProvider = null
            currentRecording = null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error releasing camera: ${e.message}")
        }
    }
} 