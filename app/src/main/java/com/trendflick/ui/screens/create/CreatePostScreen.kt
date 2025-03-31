package com.trendflick.ui.screens.create

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.trendflick.ui.components.EnhancedRichTextEditor
import com.trendflick.ui.components.HashtagSuggestionList
import com.trendflick.ui.components.MentionSuggestionList
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Enhanced CreatePostScreen with improved tagging and rich text functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    
    // State for text input with rich text support
    var textFieldValue by remember { 
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0)
            )
        ) 
    }
    
    // State for media attachment
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    
    // State for hashtag and mention suggestions
    var showHashtagSuggestions by remember { mutableStateOf(false) }
    var showMentionSuggestions by remember { mutableStateOf(false) }
    var currentWord by remember { mutableStateOf("") }
    
    // State for post visibility
    var isPublic by remember { mutableStateOf(true) }
    
    // State for posting status
    val isPosting by viewModel.isPosting.collectAsState()
    val postSuccess by viewModel.postSuccess.collectAsState()
    
    // Effect to handle post success
    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            navController.popBackStack()
        }
    }
    
    // Media picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            mediaUri = it
            // Check if it's a video
            val mimeType = context.contentResolver.getType(uri)
            isVideo = mimeType?.startsWith("video/") == true
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            mediaPickerLauncher.launch("image/* video/*")
        }
    }
    
    // Function to check and request storage permission
    val pickMedia = {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                mediaPickerLauncher.launch("image/* video/*")
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    
    // Function to handle text changes and detect hashtags/mentions
    val onTextChanged = { newValue: TextFieldValue ->
        textFieldValue = newValue
        
        // Get current word being typed
        val text = newValue.text
        val selection = newValue.selection.start
        if (selection > 0) {
            // Find the start of the current word
            var wordStart = selection - 1
            while (wordStart >= 0 && !text[wordStart].isWhitespace()) {
                wordStart--
            }
            wordStart++
            
            // Extract the current word
            if (wordStart < selection) {
                val word = text.substring(wordStart, selection)
                currentWord = word
                
                // Check if it's a hashtag or mention
                showHashtagSuggestions = word.startsWith("#") && word.length > 1
                showMentionSuggestions = word.startsWith("@") && word.length > 1
            }
        } else {
            showHashtagSuggestions = false
            showMentionSuggestions = false
        }
    }
    
    // Function to insert a hashtag or mention
    val insertSuggestion = { suggestion: String, isHashtag: Boolean ->
        val text = textFieldValue.text
        val selection = textFieldValue.selection.start
        
        // Find the start of the current word
        var wordStart = selection - 1
        while (wordStart >= 0 && !text[wordStart].isWhitespace()) {
            wordStart--
        }
        wordStart++
        
        // Replace the current word with the suggestion
        val prefix = if (isHashtag) "#" else "@"
        val newText = text.substring(0, wordStart) + prefix + suggestion + " " + text.substring(selection)
        val newSelection = wordStart + prefix.length + suggestion.length + 1
        
        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newSelection)
        )
        
        showHashtagSuggestions = false
        showMentionSuggestions = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.createPost(
                                text = textFieldValue.text,
                                mediaUri = mediaUri,
                                isPublic = isPublic,
                                isVideo = isVideo
                            )
                        },
                        enabled = !isPosting && (textFieldValue.text.isNotEmpty() || mediaUri != null)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Post",
                                tint = if (textFieldValue.text.isNotEmpty() || mediaUri != null) 
                                    Color(0xFF6B4EFF) else Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        keyboardController?.hide()
                        showHashtagSuggestions = false
                        showMentionSuggestions = false
                    })
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Rich Text Editor with enhanced hashtag and mention support
                EnhancedRichTextEditor(
                    value = textFieldValue,
                    onValueChange = onTextChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = "What's happening?",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    onHashtagClick = { hashtag ->
                        navController.navigate("hashtag/$hashtag")
                    },
                    onMentionClick = { username ->
                        navController.navigate("profile/$username")
                    }
                )
                
                // Media Preview (if selected)
                mediaUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Media preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Video indicator
                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        // Remove button
                        IconButton(
                            onClick = { mediaUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove media",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Media button
                    IconButton(
                        onClick = pickMedia,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Add media",
                            tint = Color(0xFF6B4EFF)
                        )
                    }
                    
                    // Visibility toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable { isPublic = !isPublic }
                    ) {
                        Icon(
                            imageVector = if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = if (isPublic) "Public" else "Private",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = if (isPublic) "Public" else "Private",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    
                    // Character count
                    Text(
                        text = "${textFieldValue.text.length}/300",
                        color = if (textFieldValue.text.length > 280) Color.Red else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Hashtag suggestions
            if (showHashtagSuggestions) {
                val hashtagQuery = currentWord.removePrefix("#")
                HashtagSuggestionList(
                    query = hashtagQuery,
                    onSuggestionClick = { suggestion ->
                        insertSuggestion(suggestion, true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 120.dp, start = 16.dp, end = 16.dp)
                )
            }
            
            // Mention suggestions
            if (showMentionSuggestions) {
                val mentionQuery = currentWord.removePrefix("@")
                MentionSuggestionList(
                    query = mentionQuery,
                    onSuggestionClick = { suggestion ->
                        insertSuggestion(suggestion, false)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 120.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}
