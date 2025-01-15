package com.trendflick.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
private fun checkAndRequestPermissions(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val requiredPermissions = remember {
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            Toast.makeText(
                context,
                "Camera and microphone permissions are required for recording",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(missingPermissions) {
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onVideoRecorded: (List<File>) -> Unit
) {
    var hasPermissions by remember { mutableStateOf(false) }
    
    checkAndRequestPermissions {
        hasPermissions = true
    }
    
    if (!hasPermissions) {
        Box(modifier = modifier.fillMaxSize()) {
            Text(
                text = "Camera and microphone permissions are required",
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var recording: Recording? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var isBackCamera by remember { mutableStateOf(true) }
    var selectedTimeLimit by remember { mutableStateOf(60L) }
    var recordedTime by remember { mutableStateOf(0L) }
    var videoSegments by remember { mutableStateOf(listOf<File>()) }
    var segmentTimes by remember { mutableStateOf(listOf<Long>()) }

    // Time limit options in seconds
    val timeLimitOptions = remember {
        listOf(
            15L to "15s",
            60L to "60s",
            180L to "3m",
            600L to "10m"
        )
    }

    // Update recorded time
    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(100)
            recordedTime += 100
            if (recordedTime >= selectedTimeLimit * 1000) {
                recording?.stop()
                isRecording = false
                isPaused = false
            }
        }
    }

    // Format time display
    fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    // Function to bind camera use cases
    fun bindCameraUseCases() {
        val cameraSelector = if (isBackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        
        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview!!,
                videoCapture!!
            )
            
            previewView?.let { view ->
                preview?.setSurfaceProvider(view.surfaceProvider)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    // Initialize camera
    LaunchedEffect(Unit) {
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).await(context)
            
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )
            
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            
            videoCapture = VideoCapture.withOutput(recorder)
            
            bindCameraUseCases()
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
        }
    }

    // Function to start recording
    fun startRecording() {
        val videoFile = File(
            context.getExternalFilesDir(null),
            "TrendFlick_${System.currentTimeMillis()}.mp4"
        )
        
        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

        videoCapture?.output?.prepareRecording(context, fileOutputOptions)
            ?.apply { 
                withAudioEnabled()
            }
            ?.start(ContextCompat.getMainExecutor(context)) { event ->
                when(event) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        Log.d(TAG, "Recording started successfully")
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            Log.d(TAG, "Recording saved: ${videoFile.absolutePath}")
                            if (videoFile.exists() && videoFile.length() > 0) {
                                videoSegments = videoSegments + videoFile
                                segmentTimes = segmentTimes + recordedTime
                            }
                        } else {
                            Log.e(TAG, "Recording failed: ${event.error}")
                            Toast.makeText(context, 
                                "Failed to save recording: ${event.error}", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        isRecording = false
                        isPaused = false
                        recording = null
                    }
                }
            }?.also { recording = it }
    }

    Box(modifier = modifier) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    preview?.setSurfaceProvider(this.surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Camera controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete last segment button
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (videoSegments.isNotEmpty()) {
                                Toast.makeText(context, "Discard the last clip?", Toast.LENGTH_SHORT).show()
                                val lastSegment = videoSegments.last()
                                videoSegments = videoSegments.dropLast(1)
                                segmentTimes = segmentTimes.dropLast(1)  // Remove the last segment time
                                if (segmentTimes.isNotEmpty()) {
                                    recordedTime = segmentTimes.last()  // Set time to last segment
                                } else {
                                    recordedTime = 0
                                }
                                lastSegment.delete()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Delete Last Segment",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Center column for time options and record button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Time options
                    Row(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        timeLimitOptions.forEach { (seconds, label) ->
                            val isSelected = selectedTimeLimit == seconds
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clickable { 
                                        if (!isRecording) {
                                            selectedTimeLimit = seconds
                                            recordedTime = 0
                                            segmentTimes = listOf()
                                            videoSegments = listOf()
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    // Record button
                    Box(
                        modifier = Modifier.size(85.dp)
                    ) {
                        // Background circle (white border)
                        Box(
                            modifier = Modifier
                                .size(85.dp)
                                .align(Alignment.Center)
                                .border(
                                    width = 4.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )

                        // Draw individual segment progress arcs
                        var lastSegmentTime = 0L
                        segmentTimes.forEach { segmentEndTime ->
                            // Add a small gap by reducing the progress arc length
                            val gapSize = 0.02f // 2% gap
                            val progress = ((segmentEndTime - lastSegmentTime).toFloat() / (selectedTimeLimit * 1000)) - gapSize
                            
                            CircularProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .size(85.dp)
                                    .align(Alignment.Center)
                                    .rotate(360f * lastSegmentTime.toFloat() / (selectedTimeLimit * 1000)),
                                color = Color(0xFFFF0000),
                                strokeWidth = 4.dp
                            )
                            lastSegmentTime = segmentEndTime
                        }

                        // Current segment progress (also with gap)
                        if (isRecording && recordedTime > lastSegmentTime) {
                            val gapSize = 0.02f // 2% gap
                            val progress = ((recordedTime - lastSegmentTime).toFloat() / (selectedTimeLimit * 1000)) - gapSize
                            
                            CircularProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .size(85.dp)
                                    .align(Alignment.Center)
                                    .rotate(360f * lastSegmentTime.toFloat() / (selectedTimeLimit * 1000)),
                                color = Color(0xFFFF0000),
                                strokeWidth = 4.dp
                            )
                        }

                        // White segment divider lines
                        segmentTimes.forEach { segmentEndTime ->
                            Box(
                                modifier = Modifier
                                    .size(85.dp)
                                    .align(Alignment.Center)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(85.dp)
                                        .background(Color.White)
                                        .align(Alignment.Center)
                                        .rotate(360f * (segmentEndTime.toFloat() / (selectedTimeLimit * 1000)))
                                )
                            }
                        }
                        
                        // Inner record button
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    recording?.stop()
                                } else {
                                    startRecording()
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .align(Alignment.Center)
                                .background(
                                    color = Color(0xFFFF0000),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 6.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        ) {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.White, RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        // Timer display
                        if (recordedTime > 0) {
                            Text(
                                text = formatTime(recordedTime),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-48).dp)
                            )
                        }
                    }
                }

                // Checkmark button
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (videoSegments.isNotEmpty()) {
                                onVideoRecorded(videoSegments)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Preview Video",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Camera switch button
        IconButton(
            onClick = {
                if (!isRecording) {
                    isBackCamera = !isBackCamera
                    bindCameraUseCases()
                } else {
                    Toast.makeText(context, "Stop recording first", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)  // Subtle border
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
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