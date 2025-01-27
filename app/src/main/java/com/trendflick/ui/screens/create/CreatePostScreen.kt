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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import com.trendflick.ui.viewmodels.SharedViewModel
import com.trendflick.ui.navigation.Screen
import com.trendflick.ui.components.SuggestionPopup
import com.trendflick.data.model.SuggestionItem

@Composable
private fun rememberMentionHashtagTransformation(highlightColor: Color): VisualTransformation {
    return remember(highlightColor) {
        VisualTransformation { text ->
            val annotatedString = buildAnnotatedString {
                var startIndex = 0
                var currentIndex = 0
                
                while (currentIndex < text.text.length) {
                    when (text.text[currentIndex]) {
                        '@', '#' -> {
                            // Add the text before the special character
                            append(text.text.substring(startIndex, currentIndex))
                            
                            // Find the end of the mention/hashtag
                            val endIndex = text.text.indexOf(' ', currentIndex + 1)
                                .takeIf { it != -1 } ?: text.text.length
                            
                            // Add the colored mention/hashtag
                            withStyle(SpanStyle(color = highlightColor)) {
                                append(text.text.substring(currentIndex, endIndex))
                            }
                            
                            currentIndex = endIndex
                            startIndex = endIndex
                        }
                        else -> currentIndex++
                    }
                }
                
                // Add any remaining text
                if (startIndex < text.text.length) {
                    append(text.text.substring(startIndex))
                }
            }
            
            TransformedText(annotatedString, OffsetMapping.Identity)
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
    var showSuggestions by remember { mutableStateOf(false) }
    var currentQuery by remember { mutableStateOf("") }
    var queryType by remember { mutableStateOf<QueryType?>(null) }
    val suggestions by viewModel.suggestions.collectAsState()
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()

    // Get the highlight color from the theme
    val highlightColor = MaterialTheme.colorScheme.primary
    val mentionHashtagTransformation = rememberMentionHashtagTransformation(highlightColor)

    LaunchedEffect(postTextFieldValue.text) {
        // Check if we're typing a mention or hashtag
        val text = postTextFieldValue.text
        val lastAtIndex = text.lastIndexOf('@')
        val lastHashIndex = text.lastIndexOf('#')
        
        when {
            lastAtIndex != -1 && lastAtIndex > lastHashIndex && 
            (lastAtIndex == 0 || text[lastAtIndex - 1].isWhitespace()) -> {
                val query = text.substring(lastAtIndex + 1).takeWhile { !it.isWhitespace() }
                if (query != currentQuery) {
                    currentQuery = query
                    queryType = QueryType.MENTION
                    viewModel.searchMentions(query)
                    showSuggestions = true
                }
            }
            lastHashIndex != -1 && lastHashIndex > lastAtIndex && 
            (lastHashIndex == 0 || text[lastHashIndex - 1].isWhitespace()) -> {
                val query = text.substring(lastHashIndex + 1).takeWhile { !it.isWhitespace() }
                if (query != currentQuery) {
                    currentQuery = query
                    queryType = QueryType.HASHTAG
                    viewModel.searchHashtags(query)
                    showSuggestions = true
                }
            }
            else -> {
                showSuggestions = false
                currentQuery = ""
                queryType = null
            }
        }
    }

    LaunchedEffect(uiState.isPostSuccessful) {
        if (uiState.isPostSuccessful) {
            navController.navigateUp()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar with post button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.createPost(postTextFieldValue.text)
                    },
                    enabled = postTextFieldValue.text.isNotBlank() && postTextFieldValue.text.length <= 300,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B4EFF),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF6B4EFF).copy(alpha = 0.5f)
                    )
                ) {
                    Text("Post")
                }
            }

            OutlinedTextField(
                value = postTextFieldValue,
                onValueChange = { newValue ->
                    if (newValue.text.length <= 300) {
                        postTextFieldValue = newValue
                        
                        // Check for @ or # triggers
                        val lastChar = newValue.text.lastOrNull()
                        when (lastChar) {
                            '@' -> {
                                queryType = QueryType.MENTION
                                currentQuery = ""
                                showSuggestions = true
                                viewModel.searchMentions("")
                            }
                            '#' -> {
                                queryType = QueryType.HASHTAG
                                currentQuery = ""
                                showSuggestions = true
                                viewModel.searchHashtags("")
                            }
                            ' ' -> {
                                showSuggestions = false
                                queryType = null
                            }
                            else -> {
                                // Check if we're in the middle of a mention/hashtag
                                val text = newValue.text
                                val lastMentionIndex = text.lastIndexOf('@')
                                val lastHashtagIndex = text.lastIndexOf('#')
                                val lastSpaceIndex = text.lastIndexOf(' ')
                                
                                when {
                                    lastMentionIndex > lastSpaceIndex -> {
                                        queryType = QueryType.MENTION
                                        currentQuery = text.substring(lastMentionIndex + 1)
                                        showSuggestions = true
                                        viewModel.searchMentions(currentQuery)
                                    }
                                    lastHashtagIndex > lastSpaceIndex -> {
                                        queryType = QueryType.HASHTAG
                                        currentQuery = text.substring(lastHashtagIndex + 1)
                                        showSuggestions = true
                                        viewModel.searchHashtags(currentQuery)
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                placeholder = { Text("What's on your mind?") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                visualTransformation = mentionHashtagTransformation
            )

            // Character count
            Text(
                text = "${300 - postTextFieldValue.text.length} characters remaining",
                style = MaterialTheme.typography.labelSmall,
                color = if (postTextFieldValue.text.length > 280)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Suggestions dropdown
            if (showSuggestions && suggestions.isNotEmpty()) {
                SuggestionPopup(
                    suggestions = suggestions,
                    onSuggestionSelected = { suggestion ->
                        val text = postTextFieldValue.text
                        val newText = when (queryType) {
                            QueryType.MENTION -> {
                                val startIndex = text.lastIndexOf('@')
                                text.substring(0, startIndex) + "@${(suggestion as SuggestionItem.Mention).handle} "
                            }
                            QueryType.HASHTAG -> {
                                val startIndex = text.lastIndexOf('#')
                                text.substring(0, startIndex) + "#${(suggestion as SuggestionItem.Hashtag).tag} "
                            }
                            null -> text
                        }
                        postTextFieldValue = TextFieldValue(newText, TextRange(newText.length))
                        showSuggestions = false
                    },
                    onDismiss = { showSuggestions = false }
                )
            }
        }
    }
}

private enum class QueryType {
    MENTION,
    HASHTAG
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