package com.trendflick.ui.screens.upload

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionStatus

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun UploadScreen(
    navController: NavController,
    viewModel: UploadViewModel = hiltViewModel()
) {
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var selectedDuration by remember { mutableStateOf(60) } // Default 60 seconds
    var showPreview by remember { mutableStateOf(false) }
    var postToBlueSky by remember { mutableStateOf(true) }
    
    val uploadState by viewModel.uploadState.collectAsState()
    val context = LocalContext.current

    val notificationPermissionState = rememberPermissionState(
        Manifest.permission.POST_NOTIFICATIONS
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(
            onClose = { navController.navigateUp() },
            onPost = {
                selectedVideoUri?.let { uri ->
                    viewModel.uploadVideo(
                        videoUri = uri,
                        description = description,
                        postToBlueSky = postToBlueSky,
                        playbackSpeed = playbackSpeed
                    )
                }
            },
            isPosting = uploadState is UploadState.Uploading
        )

        if (selectedVideoUri == null) {
            RecordOptions(
                selectedDuration = selectedDuration,
                onDurationSelected = { selectedDuration = it },
                onStartRecording = { /* Launch camera with duration limit */ }
            )

            Button(
                onClick = { videoPickerLauncher.launch("video/*") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B4EFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Choose video",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Video")
            }
        } else {
            VideoPreview(
                videoUri = selectedVideoUri!!,
                playbackSpeed = playbackSpeed,
                onSpeedChange = { playbackSpeed = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { 
                    Text(
                        if (postToBlueSky) "Add description... (required for BlueSky)" 
                        else "Add description... (optional)"
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray,
                    focusedBorderColor = Color(0xFF6B4EFF),
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Post to BlueSky",
                    color = Color.White
                )
                Switch(
                    checked = postToBlueSky,
                    onCheckedChange = { postToBlueSky = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6B4EFF),
                        checkedTrackColor = Color(0xFF6B4EFF).copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedVideoUri?.let { uri ->
                        viewModel.uploadVideo(
                            videoUri = uri,
                            description = description,
                            postToBlueSky = postToBlueSky,
                            playbackSpeed = playbackSpeed
                        )
                    }
                },
                enabled = (postToBlueSky && description.isNotBlank() || !postToBlueSky) && uploadState !is UploadState.Uploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B4EFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uploadState is UploadState.Uploading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Upload")
                }
            }
        }

        when (uploadState) {
            is UploadState.Success -> {
                LaunchedEffect(Unit) {
                    navController.navigateUp()
                }
            }
            is UploadState.Error -> {
                Text(
                    text = (uploadState as UploadState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun TopBar(
    onClose: () -> Unit,
    onPost: () -> Unit,
    isPosting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
        Text(
            "New Flick",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        TextButton(
            onClick = onPost,
            enabled = !isPosting
        ) {
            Text(
                "Post",
                color = if (isPosting) Color.Gray else Color(0xFF6B4EFF)
            )
        }
    }
}

@Composable
private fun RecordOptions(
    selectedDuration: Int,
    onDurationSelected: (Int) -> Unit,
    onStartRecording: () -> Unit
) {
    val durations = listOf(
        60 to "60s",
        180 to "3min",
        600 to "10min"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            durations.forEach { (duration, label) ->
                DurationChip(
                    duration = duration,
                    label = label,
                    selected = duration == selectedDuration,
                    onSelected = { onDurationSelected(duration) }
                )
            }
        }

        Button(
            onClick = onStartRecording,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6B4EFF)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Record video",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Record Video")
        }
    }
}

@Composable
private fun DurationChip(
    duration: Int,
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF6B4EFF) else Color.DarkGray,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable(onClick = onSelected)
    ) {
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun VideoPreview(
    videoUri: Uri,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Video preview player implementation
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(videoUri)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
                        start()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            update = { view ->
                view.setVideoURI(videoUri)
            }
        )

        // Playback speed control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speed:", color = Color.White)
            listOf(0.5f, 1f, 1.5f, 2f).forEach { speed ->
                SpeedChip(
                    speed = speed,
                    selected = speed == playbackSpeed,
                    onSelected = { onSpeedChange(speed) }
                )
            }
        }
    }
}

@Composable
private fun SpeedChip(
    speed: Float,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF6B4EFF) else Color.DarkGray,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable(onClick = onSelected)
    ) {
        Text(
            text = "${speed}x",
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
} 