@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)

package com.trendflick.ui.screens.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import com.trendflick.ui.viewmodels.SharedViewModel
import com.trendflick.ui.navigation.Screen

@Composable
private fun rememberMentionHashtagTransformation(highlightColor: Color): VisualTransformation {
    return remember {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val annotatedString = buildAnnotatedString {
                    var lastIndex = 0
                    val mentionPattern = Regex("@[\\w.-]+")
                    val hashtagPattern = Regex("#[\\w-]+")
                    
                    val allMatches = (mentionPattern.findAll(text.text) + hashtagPattern.findAll(text.text))
                        .sortedBy { it.range.first }
                    
                    for (match in allMatches) {
                        // Add text before the match
                        append(text.text.substring(lastIndex, match.range.first))
                        
                        // Add the highlighted match
                        withStyle(SpanStyle(color = highlightColor)) {
                            append(match.value)
                        }
                        
                        lastIndex = match.range.last + 1
                    }
                    
                    // Add remaining text
                    if (lastIndex < text.text.length) {
                        append(text.text.substring(lastIndex))
                    }
                }
                
                return TransformedText(
                    text = annotatedString,
                    offsetMapping = OffsetMapping.Identity
                )
            }
        }
    }
}

@Composable
fun CreatePostScreen(
    navController: NavController,
    viewModel: CreatePostViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    replyToUri: String? = null,
    replyToCid: String? = null
) {
    var postTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var isPosting by remember { mutableStateOf(false) }
    var showHandleSuggestions by remember { mutableStateOf(false) }
    var currentMentionQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val uiState by viewModel.uiState.collectAsState()
    val handleSuggestions by viewModel.handleSuggestions.collectAsState()

    // Get the highlight color from the theme in the composable context
    val highlightColor = MaterialTheme.colorScheme.primary
    
    // Pass the color to the transformation
    val mentionHashtagTransformation = rememberMentionHashtagTransformation(highlightColor)

    LaunchedEffect(postTextFieldValue.text) {
        // Check if we're in the middle of typing a mention
        val lastAtSymbolIndex = postTextFieldValue.text.lastIndexOf('@')
        if (lastAtSymbolIndex != -1) {
            val textAfterAt = postTextFieldValue.text.substring(lastAtSymbolIndex + 1)
            val spaceAfterAt = textAfterAt.indexOf(' ')
            val currentWord = if (spaceAfterAt == -1) textAfterAt else textAfterAt.substring(0, spaceAfterAt)
            
            if (currentWord.isNotEmpty()) {
                currentMentionQuery = currentWord
                viewModel.searchHandles(currentWord)
                showHandleSuggestions = true
            } else {
                showHandleSuggestions = false
            }
        } else {
            showHandleSuggestions = false
        }
    }

    LaunchedEffect(uiState.isPostSuccessful) {
        if (uiState.isPostSuccessful) {
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (replyToUri != null) "Reply" else "Create Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            isPosting = true
                            keyboardController?.hide()
                            if (replyToUri != null && replyToCid != null) {
                                viewModel.createReply(postTextFieldValue.text, replyToUri, replyToCid)
                            } else {
                                viewModel.createPost(postTextFieldValue.text)
                            }
                        },
                        enabled = postTextFieldValue.text.isNotBlank() && !isPosting,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Post")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (replyToUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Replying to post",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            TextField(
                value = postTextFieldValue,
                onValueChange = { postTextFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                visualTransformation = mentionHashtagTransformation,
                placeholder = {
                    Text(
                        text = if (replyToUri != null) 
                            "Write your reply... Use @ to mention users and # for hashtags" 
                        else 
                            "What's on your mind? Use @ to mention users and # for hashtags",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    autoCorrect = true
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                maxLines = 10
            )

            // Handle suggestions
            if (showHandleSuggestions && handleSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .padding(top = 8.dp)
                ) {
                    LazyColumn {
                        items(handleSuggestions) { handle ->
                            HandleSuggestionItem(
                                handle = handle,
                                onClick = {
                                    // Replace the current mention with the selected handle
                                    val lastAtIndex = postTextFieldValue.text.lastIndexOf('@')
                                    if (lastAtIndex != -1) {
                                        postTextFieldValue = TextFieldValue(postTextFieldValue.text.substring(0, lastAtIndex) + "@${handle.handle} " + postTextFieldValue.text.substring(lastAtIndex + 1))
                                    }
                                    showHandleSuggestions = false
                                }
                            )
                        }
                    }
                }
            }

            // Error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Loading indicator
            if (isPosting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HandleSuggestionItem(
    handle: HandleSuggestion,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "@${handle.handle}",
                style = MaterialTheme.typography.bodyLarge
            )
            if (handle.displayName != null) {
                Text(
                    text = handle.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
} 