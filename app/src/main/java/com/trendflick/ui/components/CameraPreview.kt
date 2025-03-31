package com.trendflick.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import android.net.Uri
import java.util.concurrent.Executor
import kotlin.coroutines.suspendCoroutine
import android.content.Intent
import android.provider.Settings
import com.trendflick.utils.PermissionUtils

private const val TAG = "CameraPreview"

// Add extension function for ListenableFuture to support await()
suspend fun <T> ListenableFuture<T>.await(context: Context): T = suspendCancellableCoroutine { cont ->
    addListener({
        try {
            cont.resume(get())
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }, ContextCompat.getMainExecutor(context))
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
                        // Open app settings
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
    onSegmentUpdated: (List<Float>) -> Unit
) {
    var permissionsGranted by remember { mutableStateOf(false) }
    
    RequestPermissions(
        onPermissionsGranted = { permissionsGranted = true },
        onPermissionsDenied = { permissionsGranted = false }
    )

    if (!permissionsGranted) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val videoCapture: MutableState<VideoCapture<Recorder>?> = remember { mutableStateOf(null) }
    val recording: MutableState<Recording?> = remember { mutableStateOf(null) }
    val recordedSegments = remember { mutableStateListOf<File>() }
    val recordingSegments = remember { mutableStateListOf<Float>() }
    var currentSegmentProgress by remember { mutableStateOf(0f) }

    // Handle recording state changes
    LaunchedEffect(isRecording) {
        if (isRecording) {
            startRecording(
                context = context,
                videoCapture = videoCapture.value,
                recording = recording,
                recordedSegments = recordedSegments,
                recordingSegments = recordingSegments,
                onSegmentUpdated = onSegmentUpdated,
                onVideoRecorded = onVideoRecorded
            )
        } else {
            recording.value?.stop()
        }
    }

    // Handle pause/resume
    LaunchedEffect(isPaused) {
        if (isPaused && isRecording) {
            recording.value?.pause()
            if (currentSegmentProgress > 0f) {
                recordingSegments.add(currentSegmentProgress)
                currentSegmentProgress = 0f
                onSegmentUpdated(recordingSegments.toList())
            }
        } else if (!isPaused && isRecording) {
            recording.value?.resume()
            recordingSegments.add(0f)
            onSegmentUpdated(recordingSegments.toList())
        }
    }

    // Handle camera setup and switching
    LaunchedEffect(isBackCamera) {
        val cameraProvider = context.getCameraProvider()
        val cameraSelector = if (isBackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture.value = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture.value
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recording.value?.stop()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

private suspend fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    recording: MutableState<Recording?>,
    recordedSegments: MutableList<File>,
    recordingSegments: MutableList<Float>,
    onSegmentUpdated: (List<Float>) -> Unit,
    onVideoRecorded: (List<File>) -> Unit
) {
    val videoFile = File(
        context.filesDir,
        "TrendFlick_${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())}.mp4"
    )
    
    val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

    recording.value = videoCapture?.output
        ?.prepareRecording(context, fileOutputOptions)
        ?.apply { withAudioEnabled() }
        ?.start(ContextCompat.getMainExecutor(context)) { event ->
            when(event) {
                is VideoRecordEvent.Start -> {
                    recordingSegments.add(0f)
                    onSegmentUpdated(recordingSegments.toList())
                }
                is VideoRecordEvent.Pause -> {
                    // Handle pause event
                }
                is VideoRecordEvent.Resume -> {
                    // Handle resume event
                }
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        Log.e("CameraPreview", "Video capture failed: ${event.cause}")
                    } else {
                        recordedSegments.add(videoFile)
                        onVideoRecorded(recordedSegments.toList())
                    }
                }
            }
        }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            {
                continuation.resume(future.get())
            },
            ContextCompat.getMainExecutor(this)
        )
    }
}

private fun mergeVideoSegments(segments: List<File>): File {
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