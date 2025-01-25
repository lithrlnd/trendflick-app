package com.trendflick.ui.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onVideoRecorded: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var cameraPreview by remember { mutableStateOf<CameraPreview?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    cameraPreview = CameraPreview(
                        context = ctx,
                        lifecycleOwner = lifecycleOwner,
                        previewView = this
                    )
                    scope.launch {
                        cameraPreview?.startCamera()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Camera Controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Close Button
            IconButton(
                onClick = {
                    cameraPreview?.release()
                    onClose()
                },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Record Button
            Button(
                onClick = {
                    if (isRecording) {
                        cameraPreview?.stopRecording { videoPath ->
                            onVideoRecorded(videoPath)
                        }
                    } else {
                        cameraPreview?.startRecording()
                    }
                    isRecording = !isRecording
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) 
                        MaterialTheme.colorScheme.error 
                    else MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                // Empty content, the button itself is the visual indicator
            }
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraPreview?.release()
        }
    }
} 