package com.trendflick.ui.screens.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.trendflick.ui.components.CameraPreview
import java.io.File
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.ui.text.style.TextOverflow
import android.util.Log

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalTransitionApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun CreateFlickScreen(
    navController: NavController,
    viewModel: CreateFlickViewModel = hiltViewModel()
) {
    var showCameraPreview by remember { mutableStateOf(true) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var showDescriptionScreen by remember { mutableStateOf(false) }
    var showPreviewScreen by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(false) }
    var recordingSegments by remember { mutableStateOf(listOf<Float>()) }
    var selectedDuration by remember { mutableStateOf(60L) } // Max 60s for BlueSky
    var recordingProgress by remember { mutableStateOf(0f) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var currentTag by remember { mutableStateOf("") }
    var isTagging by remember { mutableStateOf(false) }
    var isMentioning by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    
    // Define time options
    val timeOptions = remember { listOf(60L to "60s") } // Only 60s for BlueSky
    
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Collect UI state
    val uiState by viewModel.uiState.collectAsState()

    // Add snackbarHostState
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when {
            uiState.isPostSuccessful -> {
                // Navigate back to flicks screen on successful post
                navController.navigate("flicks") {
                    popUpTo("create_flick") { inclusive = true }
                }
            }
            uiState.error != null -> {
                // Show error Snackbar
                snackbarHostState.showSnackbar(
                    message = uiState.error!!,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
        if (uri != null) {
            showCameraPreview = false
            showPreviewScreen = false
            showDescriptionScreen = true
            // Reset description when new video is selected
            textFieldValue = TextFieldValue()
        }
    }

    fun resetRecording() {
        isRecording = false
        isPaused = false
        elapsedTime = 0
        recordingProgress = 0f
        recordingSegments = listOf()
        selectedVideoUri = null
        textFieldValue = TextFieldValue()
    }

    fun discardChanges() {
        showDialog = false
        showDescriptionScreen = false
        showPreviewScreen = false
        showCameraPreview = true
        resetRecording()
    }

    // Modified recording timer effect
    LaunchedEffect(isRecording, isPaused, selectedDuration) {
        if (isRecording && !isPaused) {
            while (elapsedTime < selectedDuration * 1000) {
                delay(100)
                elapsedTime += 100
                recordingProgress = elapsedTime.toFloat() / (selectedDuration * 1000)
                
                if (elapsedTime >= selectedDuration * 1000) {
                    isRecording = false
                    isPaused = false
                    showPreviewScreen = true
                    showCameraPreview = false
                    break
                }
            }
        }
    }

    // Detect when user is typing @ or #
    LaunchedEffect(textFieldValue.text) {
        val lastChar = textFieldValue.text.lastOrNull()
        val beforeLastChar = if (textFieldValue.text.length > 1) 
            textFieldValue.text[textFieldValue.text.length - 2] else null
        
        when {
            lastChar == '@' && (beforeLastChar == null || beforeLastChar.isWhitespace()) -> {
                isMentioning = true
                isTagging = false
                currentTag = ""
                showSuggestions = true
            }
            lastChar == '#' && (beforeLastChar == null || beforeLastChar.isWhitespace()) -> {
                isTagging = true
                isMentioning = false
                currentTag = ""
                showSuggestions = true
            }
            lastChar?.isWhitespace() == true -> {
                isTagging = false
                isMentioning = false
                showSuggestions = false
            }
            isTagging || isMentioning -> {
                if (lastChar != null && !lastChar.isWhitespace()) {
                    currentTag += lastChar
                    // Here you would typically fetch suggestions based on currentTag
                }
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .navigationBarsPadding()
        .imePadding()
    ) {
        when {
            showDescriptionScreen -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Top Bar
                    TopAppBar(
                        title = { Text("New Flick", color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { showDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            Button(
                                onClick = {
                                    if (textFieldValue.text.isNotEmpty() && selectedVideoUri != null) {
                                        viewModel.createFlick(selectedVideoUri!!, textFieldValue.text)
                                    }
                                },
                                enabled = textFieldValue.text.isNotEmpty() && selectedVideoUri != null && !uiState.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6B4EFF),
                                    disabledContainerColor = Color(0xFF6B4EFF).copy(alpha = 0.5f)
                                )
                            ) {
                                if (uiState.isLoading) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White
                                        )
                                        Text("Posting...")
                                    }
                                } else {
                                    Text("Post")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black
                        )
                    )

                    // Video preview with loading overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        selectedVideoUri?.let { uri ->
                            AndroidView(
                                factory = { context ->
                                    VideoView(context).apply {
                                        setVideoURI(uri)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Loading overlay
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f))
                            ) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                    Text(
                                        text = "Uploading to BlueSky...",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (uiState.uploadProgress > 0f) {
                                        Text(
                                            text = "${(uiState.uploadProgress * 100).toInt()}%",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        LinearProgressIndicator(
                                            progress = uiState.uploadProgress,
                                            modifier = Modifier
                                                .width(200.dp)
                                                .height(4.dp),
                                            color = Color(0xFF6B4EFF)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Description input
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            if (newValue.text.length <= 300) {
                                textFieldValue = newValue
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(120.dp),
                        enabled = !uiState.isLoading,
                        placeholder = { 
                            Text(
                                "What's on your mind? Use @ to mention users and # for hashtags",
                                color = Color.Gray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            disabledContainerColor = Color.Black,
                            focusedBorderColor = Color(0xFF6B4EFF),
                            unfocusedBorderColor = Color.Gray,
                            disabledBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White.copy(alpha = 0.5f)
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    // Character count and error message
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${300 - textFieldValue.text.length} characters remaining",
                            color = if (textFieldValue.text.length > 280) Color.Red else Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (uiState.error != null) {
                            Text(
                                text = uiState.error!!,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Discard Changes?") },
                            text = { Text("Are you sure you want to discard your video and changes?") },
                            confirmButton = {
                                TextButton(
                                    onClick = { discardChanges() }
                                ) {
                                    Text("Discard", color = Color.Red)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
            showPreviewScreen -> {
                // Preview Screen
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                ) {
                    // Video Preview with error handling
                    selectedVideoUri?.let { uri ->
                        Log.d("CreateFlickScreen", "Attempting to preview video: $uri")
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    setOnErrorListener { _, what, extra ->
                                        Log.e("CreateFlickScreen", "VideoView error: what=$what, extra=$extra")
                                        true
                                    }
                                    setVideoURI(uri)
                                    setOnPreparedListener { mp ->
                                        Log.d("CreateFlickScreen", "Video prepared successfully")
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: run {
                        Log.e("CreateFlickScreen", "No video URI available in preview screen")
                    }

                    // Overlay for controls with darker background for better visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        // Top bar with back button
                        TopAppBar(
                            title = { Text("Preview", color = Color.White) },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        showPreviewScreen = false
                                        showCameraPreview = true
                                        resetRecording()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.align(Alignment.TopCenter)
                        )

                        // Preview Controls with more prominent buttons
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 120.dp) // Increased bottom padding to move above nav bar
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(vertical = 16.dp) // Add padding inside the dark background
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        showPreviewScreen = false
                                        showCameraPreview = true
                                        resetRecording()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                        .height(48.dp) // Make buttons taller
                                ) {
                                    Text("Retake", color = Color.White)
                                }
                                
                                Button(
                                    onClick = {
                                        Log.d("CreateFlickScreen", "Next button clicked, navigating to description")
                                        showPreviewScreen = false
                                        showDescriptionScreen = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EFF)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                        .height(48.dp) // Make buttons taller
                                ) {
                                    Text("Next", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Camera/Recording Screen
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showCameraPreview) {
                        CameraPreview(
                            onVideoRecorded = { videoFiles ->
                                if (videoFiles.isNotEmpty()) {
                                    val videoFile = videoFiles.last()
                                    Log.d("CreateFlickScreen", "Video recorded: ${videoFile.absolutePath}")
                                    selectedVideoUri = Uri.fromFile(videoFile)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            isPaused = isPaused,
                            isBackCamera = isBackCamera,
                            isRecording = isRecording,
                            onSegmentUpdated = { segments ->
                                recordingSegments = segments
                            }
                        )
                    }

                    // Recording segments indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp)
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val segmentWidth = size.width / recordingSegments.size.coerceAtLeast(1)
                            recordingSegments.forEachIndexed { index, progress ->
                                drawLine(
                                    color = Color(0xFF6B4EFF),
                                    start = Offset(index * segmentWidth, size.height / 2),
                                    end = Offset((index + progress) * segmentWidth, size.height / 2),
                                    strokeWidth = size.height,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    // Top controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Close button
                        IconButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }

                        // Camera switch button
                        IconButton(
                            onClick = { isBackCamera = !isBackCamera },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }

                        // Gallery picker button
                        IconButton(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Choose from gallery",
                                tint = Color.White
                            )
                        }
                    }

                    // Bottom controls container
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Time options
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            timeOptions.forEach { (duration, label) ->
                                Text(
                                    text = label,
                                    color = if (selectedDuration == duration) Color.White else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .clickable { 
                                            if (!isRecording) {
                                                selectedDuration = duration
                                            }
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }

                        // Recording controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(85.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Timer circle
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Background circle
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.3f),
                                        style = Stroke(width = 4.dp.toPx())
                                    )
                                    
                                    // Progress arc
                                    drawArc(
                                        color = Color(0xFF6B4EFF),
                                        startAngle = -90f,
                                        sweepAngle = 360f * recordingProgress,
                                        useCenter = false,
                                        style = Stroke(width = 4.dp.toPx())
                                    )

                                    // Pause indicators
                                    recordingSegments.forEachIndexed { index, progress ->
                                        rotate(degrees = 360f * progress) {
                                            drawLine(
                                                color = Color.White,
                                                start = Offset(center.x, 0f),
                                                end = Offset(center.x, 8.dp.toPx()),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }
                                    }
                                }

                                // Record button
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .border(4.dp, Color.White, CircleShape)
                                        .background(if (isRecording) Color.Red else Color.White, CircleShape)
                                        .clickable {
                                            if (!isRecording) {
                                                isRecording = true
                                                isPaused = false
                                            } else if (isPaused) {
                                                isPaused = false
                                            } else {
                                                isPaused = true
                                            }
                                        }
                                )
                            }

                            // Pause/Resume button
                            if (isRecording) {
                                IconButton(
                                    onClick = { isPaused = !isPaused },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Check button (only visible when recording has content)
                            if (elapsedTime > 0) {
                                IconButton(
                                    onClick = { 
                                        isRecording = false
                                        isPaused = false
                                        if (selectedVideoUri != null) {
                                            Log.d("CreateFlickScreen", "Navigating to preview with video: $selectedVideoUri")
                                            showCameraPreview = false
                                            showDescriptionScreen = false
                                            showPreviewScreen = true
                                        } else {
                                            Log.e("CreateFlickScreen", "No video URI available for preview")
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF6B4EFF), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Add discard button when recording is paused
                        if (isPaused) {
                            IconButton(
                                onClick = { resetRecording() },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .background(Color.Red.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Discard Recording",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Snackbar host at the end of the Box
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
} 