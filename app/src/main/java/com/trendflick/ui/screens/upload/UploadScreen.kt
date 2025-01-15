package com.trendflick.ui.screens.upload

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.trendflick.ui.components.CameraPreview
import com.trendflick.ui.components.CreativeCompass
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel = hiltViewModel()
) {
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraPreview by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    val uploadState by viewModel.uploadState.collectAsState()
    val permissionsGranted by viewModel.permissionsGranted.collectAsState()
    val context = LocalContext.current
    
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA,
        onPermissionResult = { isGranted ->
            viewModel.setPermissionsGranted(isGranted)
        }
    )
    
    LaunchedEffect(cameraPermissionState.status) {
        viewModel.setPermissionsGranted(cameraPermissionState.status is PermissionStatus.Granted)
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
        showCameraPreview = false
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (!permissionsGranted) {
            Button(
                onClick = { cameraPermissionState.launchPermissionRequest() },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Grant Camera Permission")
            }
        } else if (showCameraPreview) {
            CameraPreview(
                onVideoRecorded = { videoFiles ->
                    // Use the last recorded video segment
                    if (videoFiles.isNotEmpty()) {
                        selectedVideoUri = Uri.fromFile(videoFiles.last())
                    }
                    showCameraPreview = false
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (selectedVideoUri == null) {
            CreativeCompass(
                onRecordStart = { showCameraPreview = true },
                onRecordStop = { /* Handle stop recording */ },
                onGalleryClick = { videoPickerLauncher.launch("video/*") }
            )
        }
        
        // Upload form
        if (selectedVideoUri != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = hashtags,
                    onValueChange = { hashtags = it },
                    label = { Text("Hashtags (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        selectedVideoUri?.let { uri ->
                            viewModel.uploadVideo(
                                videoUri = uri,
                                title = title,
                                description = description,
                                hashtags = hashtags.split(",").map { it.trim() }
                            )
                        }
                    },
                    enabled = title.isNotBlank() && selectedVideoUri != null
                ) {
                    Text("Upload Video")
                }
                
                when (uploadState) {
                    is UploadState.Uploading -> CircularProgressIndicator()
                    is UploadState.Success -> Text("Upload successful!", style = MaterialTheme.typography.bodyLarge)
                    is UploadState.Error -> Text(
                        (uploadState as UploadState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> Unit
                }
            }
        }
    }
} 