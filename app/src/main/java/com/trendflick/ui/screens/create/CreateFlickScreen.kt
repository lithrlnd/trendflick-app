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
    var selectedDuration by remember { mutableStateOf(60L) }
    var recordingProgress by remember { mutableStateOf(0f) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var postToBlueSky by remember { mutableStateOf(true) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var currentTag by remember { mutableStateOf("") }
    var isTagging by remember { mutableStateOf(false) }
    var isMentioning by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    
    val timeOptions = remember { listOf(
        60L to "60s",
        180L to "3m",
        600L to "10m"
    )}
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
        if (uri != null) {
            showDescriptionScreen = true
            showCameraPreview = false
        }
    }

    fun resetRecording() {
        isRecording = false
        isPaused = false
        elapsedTime = 0
        recordingProgress = 0f
        recordingSegments = listOf()
        selectedVideoUri = null
    }

    fun discardChanges() {
        showDescriptionScreen = false
        showPreviewScreen = false
        showCameraPreview = true
        textFieldValue = TextFieldValue()
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
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // Top Bar with Discard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                // Show confirmation dialog
                                showDialog = true
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Discard")
                        }
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.createFlick(
                                    selectedVideoUri!!, 
                                    textFieldValue.text, 
                                    postToBlueSky
                                )
                                navController.navigateUp()
                            },
                            enabled = textFieldValue.text.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EFF))
                        ) {
                            Text("Post")
                        }
                    }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Discard Changes?") },
                            text = { Text("Are you sure you want to discard your video and changes?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDialog = false
                                        discardChanges()
                                    }
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // BlueSky post toggle - moved to top for better visibility
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Post to BlueSky", color = Color.White)
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

                    // Video preview
                    selectedVideoUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (keyboardVisible) 150.dp else 300.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggestions now appear ABOVE the text field when active
                    if (showSuggestions) {
                        val suggestions = viewModel.suggestions.collectAsState().value
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(Color.DarkGray.copy(alpha = 0.9f))
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            items(suggestions) { suggestion ->
                                Text(
                                    text = if (isMentioning) "@$suggestion" else "#$suggestion",
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val prefix = if (isMentioning) "@" else "#"
                                            val text = textFieldValue.text
                                            val lastSpaceIndex = text.lastIndexOf(' ').coerceAtLeast(0)
                                            val newText = text.substring(0, lastSpaceIndex) + 
                                                (if (lastSpaceIndex > 0) " " else "") +
                                                prefix + suggestion + " "
                                            textFieldValue = TextFieldValue(
                                                text = newText,
                                                selection = TextRange(newText.length)
                                            )
                                            showSuggestions = false
                                            isTagging = false
                                            isMentioning = false
                                            currentTag = ""
                                        }
                                        .padding(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            val text = newValue.text
                            val lastWord = text.substring(text.lastIndexOf(' ').coerceAtLeast(0)).trim()
                            
                            when {
                                lastWord.startsWith("@") -> {
                                    currentTag = lastWord.substring(1)
                                    if (currentTag.isNotEmpty()) {
                                        showSuggestions = true
                                        isTagging = false
                                        isMentioning = true
                                        // Fetch BlueSky users matching the query
                                        viewModel.searchBlueSkyUsers(currentTag)
                                    }
                                }
                                lastWord.startsWith("#") -> {
                                    currentTag = lastWord.substring(1)
                                    if (currentTag.isNotEmpty()) {
                                        showSuggestions = true
                                        isTagging = true
                                        isMentioning = false
                                        // Fetch trending hashtags or filter local list
                                        viewModel.searchHashtags(currentTag)
                                    }
                                }
                                lastWord.isEmpty() -> {
                                    showSuggestions = false
                                    isTagging = false
                                    isMentioning = false
                                    currentTag = ""
                                }
                                else -> {
                                    if (showSuggestions && !lastWord.contains("@") && !lastWord.contains("#")) {
                                        showSuggestions = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Add a description... Use @ to mention users and # for hashtags") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF6B4EFF),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = TextStyle(fontSize = 16.sp)
                    )
                }
            }
            showPreviewScreen -> {
                // Preview Screen
                Box(modifier = Modifier.fillMaxSize()) {
                    // Video Preview
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

                    // Preview Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Retake")
                            }
                            
                            Button(
                                onClick = {
                                    showPreviewScreen = false
                                    showDescriptionScreen = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EFF))
                            ) {
                                Text("Use Video")
                            }
                        }
                    }

                    // Add back button to preview screen
                    IconButton(
                        onClick = {
                            showPreviewScreen = false
                            showCameraPreview = true
                            resetRecording()
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                                    selectedVideoUri = Uri.fromFile(videoFiles.last())
                                    showDescriptionScreen = true
                                    showCameraPreview = false
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
                                        showPreviewScreen = true 
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
    }
} 