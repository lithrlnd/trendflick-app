package com.trendflick.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.trendflick.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "CameraPreview"

private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).apply {
        addListener({
            try {
                continuation.resume(get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(this@getCameraProvider))
    }
}

suspend fun Context.getExtensionsManager(cameraProvider: ProcessCameraProvider): ExtensionsManager = suspendCoroutine { continuation ->
    ExtensionsManager.getInstanceAsync(this, cameraProvider).apply {
        addListener({
            try {
                continuation.resume(get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(this@getExtensionsManager))
    }
}

@Composable
fun RequestPermissions(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val requiredPermissions = remember {
        mutableListOf<String>().apply {
            addAll(PermissionUtils.getRequiredCameraPermissions())
            addAll(PermissionUtils.getRequiredMediaPermissions())
        }
    }

    val missingPermissions = remember(requiredPermissions) {
        requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    if (missingPermissions.isEmpty()) {
        onPermissionsGranted()
        return
    }

    var showRationale by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            showRationale = true
            onPermissionsDenied()
        }
    }

    LaunchedEffect(missingPermissions) {
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permissions Required") },
            text = { 
                Text(
                    "Camera and storage permissions are required to record and save videos. " +
                    "Please grant these permissions in Settings."
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        activity?.let { act ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", act.packageName, null)
                            }
                            act.startActivity(intent)
                        }
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onVideoRecorded: (List<File>) -> Unit,
    isPaused: Boolean,
    isBackCamera: Boolean,
    isRecording: Boolean,
    onSegmentUpdated: (List<Float>) -> Unit,
    enableBeautyFilter: Boolean = true
) {
    var permissionsGranted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBeautyFilterAvailable by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    RequestPermissions(
        onPermissionsGranted = { permissionsGranted = true },
        onPermissionsDenied = { permissionsGranted = false }
    )

    if (!permissionsGranted || !checkCameraPermission(context) || !checkAudioPermission(context)) {
        return
    }

    val preview = remember { Preview.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val videoCapture: MutableState<VideoCapture<Recorder>?> = remember { mutableStateOf(null) }
    val recording: MutableState<Recording?> = remember { mutableStateOf(null) }
    val recordedSegments = remember { mutableStateListOf<File>() }
    val recordingSegments = remember { mutableStateListOf<Float>() }
    var currentSegmentProgress by remember { mutableStateOf(0f) }

    // Check if beauty filter is available
    LaunchedEffect(Unit) {
        try {
            val cameraProvider = context.getCameraProvider()
            val extensionsManager = context.getExtensionsManager(cameraProvider)
            isBeautyFilterAvailable = extensionsManager.isExtensionAvailable(
                CameraSelector.DEFAULT_FRONT_CAMERA, 
                ExtensionMode.FACE_RETOUCH
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check beauty filter availability", e)
        }
    }

    // Handle recording state changes
    LaunchedEffect(isRecording) {
        if (isRecording) {
            try {
                startRecording(
                    context = context,
                    videoCapture = videoCapture.value,
                    recording = recording,
                    recordedSegments = recordedSegments,
                    recordingSegments = recordingSegments,
                    onSegmentUpdated = onSegmentUpdated,
                    onVideoRecorded = onVideoRecorded,
                    isBackCamera = isBackCamera
                )
            } catch (e: Exception) {
                errorMessage = "Failed to start recording: ${e.message}"
                Log.e(TAG, "Recording error", e)
            }
        } else {
            try {
                recording.value?.stop()
            } catch (e: Exception) {
                errorMessage = "Failed to stop recording: ${e.message}"
                Log.e(TAG, "Stop recording error", e)
            }
        }
    }

    // Handle pause/resume
    LaunchedEffect(isPaused) {
        if (isPaused && isRecording) {
            try {
                recording.value?.pause()
                if (currentSegmentProgress > 0f) {
                    recordingSegments.add(currentSegmentProgress)
                    currentSegmentProgress = 0f
                    onSegmentUpdated(recordingSegments.toList())
                }
            } catch (e: Exception) {
                errorMessage = "Failed to pause recording: ${e.message}"
                Log.e(TAG, "Pause error", e)
            }
        } else if (!isPaused && isRecording) {
            try {
                recording.value?.resume()
                recordingSegments.add(0f)
                onSegmentUpdated(recordingSegments.toList())
            } catch (e: Exception) {
                errorMessage = "Failed to resume recording: ${e.message}"
                Log.e(TAG, "Resume error", e)
            }
        }
    }

    // Handle camera setup and switching
    LaunchedEffect(isBackCamera, enableBeautyFilter) {
        try {
            val cameraProvider = context.getCameraProvider()
            val cameraSelector = if (isBackCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            // Configure preview with proper rotation
            val preview = Preview.Builder()
                .setTargetRotation(previewView.display.rotation)
                .build()

            // Configure preview view for optimal performance and scaling
            previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

            // Apply beauty filter if available and enabled for front camera
            val effectiveCameraSelector = if (enableBeautyFilter && isBeautyFilterAvailable && !isBackCamera) {
                val extensionsManager = context.getExtensionsManager(cameraProvider)
                extensionsManager.getExtensionEnabledCameraSelector(
                    cameraSelector,
                    ExtensionMode.FACE_RETOUCH
                )
            } else {
                cameraSelector
            }

            // Configure video capture with proper orientation settings
            val qualitySelector = QualitySelector.from(Quality.HIGHEST)
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            
            val videoCaptureBuilder = VideoCapture.withOutput(recorder)

            // Mirror preview for front camera only (selfie view)
            if (!isBackCamera) {
                previewView.scaleX = -1f
            } else {
                previewView.scaleX = 1f
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                effectiveCameraSelector,
                preview,
                videoCaptureBuilder
            )

            videoCapture.value = videoCaptureBuilder
            preview.setSurfaceProvider(previewView.surfaceProvider)

        } catch (e: Exception) {
            errorMessage = "Failed to setup camera: ${e.message}"
            Log.e(TAG, "Camera setup error", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recording.value?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording on dispose", e)
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Show beauty filter unavailable message if needed
        if (enableBeautyFilter && !isBeautyFilterAvailable && !isBackCamera) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = "Beauty filter not available on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Show error message if any
        errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { errorMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

private suspend fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    recording: MutableState<Recording?>,
    recordedSegments: MutableList<File>,
    recordingSegments: MutableList<Float>,
    onSegmentUpdated: (List<Float>) -> Unit,
    onVideoRecorded: (List<File>) -> Unit,
    isBackCamera: Boolean
) {
    if (!checkCameraPermission(context) || !checkAudioPermission(context)) {
        Log.e(TAG, "Missing camera or audio permissions")
        return
    }

    withContext(Dispatchers.IO) {
        try {
            val videoFile = File(
                context.cacheDir,
                "TrendFlick_${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())}.mp4"
            )

            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            recording.value = videoCapture?.output
                ?.prepareRecording(context, outputOptions)
                ?.apply {
                    if (checkAudioPermission(context)) {
                        withAudioEnabled()
                    }
                }
                ?.start(ContextCompat.getMainExecutor(context)) { event ->
                    when(event) {
                        is VideoRecordEvent.Start -> {
                            Log.d(TAG, "Recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (event.hasError()) {
                                Log.e(TAG, "Video capture failed: ${event.error}")
                                videoFile.delete()
                            } else {
                                Log.d(TAG, "Video capture succeeded: ${videoFile.absolutePath}")
                                recordedSegments.add(videoFile)
                                onVideoRecorded(recordedSegments.toList())
                            }
                        }
                        else -> { /* Ignore other events */ }
                    }
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during recording: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording: ${e.message}")
            throw e
        }
    }
}

private fun mergeVideoSegments(segments: List<File>, context: Context): File {
    // TODO: Implement video merging logic
    // For now, return the last segment
    return segments.last()
}

private fun createVideoFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(null)
    return File.createTempFile(
        "VIDEO_${timeStamp}_",
        ".mp4",
        storageDir
    )
} 