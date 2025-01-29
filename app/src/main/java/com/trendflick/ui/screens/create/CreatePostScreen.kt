@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)

package com.trendflick.ui.screens.create

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.trendflick.R
import com.trendflick.ui.components.SuggestionPopup
import com.trendflick.ui.model.SuggestionItem
import com.trendflick.ui.viewmodels.SharedViewModel
import com.trendflick.ui.navigation.Screen
import com.trendflick.ui.model.AIEnhancementState
import com.trendflick.ui.model.QueryType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn

@Composable
private fun rememberMentionHashtagTransformation(highlightColor: Color): VisualTransformation {
    return remember(highlightColor) {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val annotatedString = buildAnnotatedString {
                    var startIndex = 0
                    var currentIndex = 0
                    
                    while (currentIndex < text.text.length) {
                        when {
                            // Handle hashtags with proper AT Protocol regex
                            text.text[currentIndex] == '#' && (currentIndex == 0 || text.text[currentIndex - 1].isWhitespace()) -> {
                                // Add the text before the special character
                                append(text.text.substring(startIndex, currentIndex))
                                
                                // Find valid hashtag using AT Protocol rules
                                val remaining = text.text.substring(currentIndex)
                                val tagMatch = Regex("""#([^\d\s]\S*?)(?=\s|$)""").find(remaining)
                                
                                if (tagMatch != null) {
                                    val tag = tagMatch.value
                                    // Check tag length (max 64 chars not including #)
                                    if (tag.length <= 65) {
                                        withStyle(
                                            SpanStyle(
                                                color = highlightColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        ) {
                                            append(tag)
                                        }
                                        currentIndex += tag.length
                                        startIndex = currentIndex
                                    } else {
                                        append("#")
                                        currentIndex++
                                        startIndex = currentIndex
                                    }
                                } else {
                                    append("#")
                                    currentIndex++
                                    startIndex = currentIndex
                                }
                            }
                            // Handle mentions with AT Protocol regex
                            text.text[currentIndex] == '@' && (currentIndex == 0 || text.text[currentIndex - 1].isWhitespace()) -> {
                                append(text.text.substring(startIndex, currentIndex))
                                
                                val remaining = text.text.substring(currentIndex)
                                // AT Protocol mention regex
                                val mentionMatch = Regex("""@([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?""").find(remaining)
                                
                                if (mentionMatch != null) {
                                    withStyle(
                                        SpanStyle(
                                            color = highlightColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) {
                                        append(mentionMatch.value)
                                    }
                                    currentIndex += mentionMatch.value.length
                                    startIndex = currentIndex
                                } else {
                                    append("@")
                                    currentIndex++
                                    startIndex = currentIndex
                                }
                            }
                            else -> currentIndex++
                        }
                    }
                    
                    // Add any remaining text
                    if (startIndex < text.text.length) {
                        append(text.text.substring(startIndex))
                    }
                }
                
                return TransformedText(annotatedString, OffsetMapping.Identity)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle(initialValue = emptyList())
    val aiEnhancedContent by viewModel.aiEnhancedContent.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val highlightColor = MaterialTheme.colorScheme.primary
    val mentionHashtagTransformation = rememberMentionHashtagTransformation(highlightColor)

    LaunchedEffect(postTextFieldValue.text) {
        val text = postTextFieldValue.text
        val lastAtIndex = text.lastIndexOf('@')
        val lastHashIndex = text.lastIndexOf('#')
        val lastSpaceIndex = text.lastIndexOf(' ')
        
        when {
            // Handle mentions with AT Protocol format
            lastAtIndex != -1 && lastAtIndex > lastHashIndex && 
            (lastAtIndex == 0 || text[lastAtIndex - 1].isWhitespace()) -> {
                val query = text.substring(lastAtIndex + 1).takeWhile { !it.isWhitespace() }
                if (query != currentQuery) {
                    currentQuery = query
                    queryType = QueryType.MENTION
                    if (query.isNotEmpty()) {
                        viewModel.searchMentions(query)
                        showSuggestions = true
                    } else {
                        viewModel.clearSuggestions()
                        showSuggestions = false
                    }
                }
            }
            // Handle hashtags with AT Protocol format
            lastHashIndex != -1 && lastHashIndex > lastAtIndex && 
            (lastHashIndex == 0 || text[lastHashIndex - 1].isWhitespace()) -> {
                val query = text.substring(lastHashIndex + 1).takeWhile { !it.isWhitespace() }
                if (query != currentQuery) {
                    currentQuery = query
                    queryType = QueryType.HASHTAG
                    if (query.isNotEmpty()) {
                        viewModel.searchHashtags(query)
                        showSuggestions = true
                    } else {
                        viewModel.clearSuggestions()
                        showSuggestions = false
                    }
                }
            }
            else -> {
                showSuggestions = false
                currentQuery = ""
                queryType = null
                viewModel.clearSuggestions()
            }
        }
    }

    LaunchedEffect(uiState.isPostSuccessful) {
        if (uiState.isPostSuccessful) {
            navController.navigateUp()
        }
    }

    // Handle AI enhancement state
    LaunchedEffect(aiEnhancedContent) {
        when (aiEnhancedContent) {
            is AIEnhancementState.Success -> {
                val enhancement = (aiEnhancedContent as AIEnhancementState.Success).enhancement
                postTextFieldValue = TextFieldValue(
                    text = enhancement.enhancedPost + " " + enhancement.hashtags.joinToString(" ") { "#$it" },
                    selection = TextRange(enhancement.enhancedPost.length)
                )
            }
            is AIEnhancementState.Error -> {
                // You might want to show a snackbar or toast here
            }
            else -> {}
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
                        contentDescription = "Close"
                    )
                }
                
                Row {
                    // AI Enhancement Button
                    Button(
                        onClick = { 
                            if (postTextFieldValue.text.isNotBlank()) {
                                viewModel.enhancePostWithAI(postTextFieldValue.text)
                            }
                        },
                        enabled = postTextFieldValue.text.isNotBlank() && 
                                aiEnhancedContent !is AIEnhancementState.Loading,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (aiEnhancedContent is AIEnhancementState.Loading)
                                    Icons.Default.Refresh
                                else
                                    Icons.Default.AutoAwesome,
                                contentDescription = "Enhance with AI"
                            )
                            Text("Enhance")
                        }
                    }
                    
                    // Post Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.createPost(postTextFieldValue.text)
                        },
                        enabled = postTextFieldValue.text.isNotBlank() && 
                                postTextFieldValue.text.length <= 300,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B4EFF),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF6B4EFF).copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Post")
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
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

                // Suggestions dropdown
                if (showSuggestions && suggestions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 60.dp)
                            .fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp,
                            shadowElevation = 4.dp
                        ) {
                            LazyColumn {
                                items(suggestions) { suggestion ->
                                    when (suggestion) {
                                        is SuggestionItem.Mention -> {
                                            MentionSuggestionItem(
                                                mention = suggestion,
                                                onClick = {
                                                    val text = postTextFieldValue.text
                                                    val lastAtIndex = text.lastIndexOf('@')
                                                    if (lastAtIndex != -1) {
                                                        val newText = text.substring(0, lastAtIndex) + "@${suggestion.handle} "
                                                        postTextFieldValue = TextFieldValue(
                                                            text = newText,
                                                            selection = TextRange(newText.length)
                                                        )
                                                        showSuggestions = false
                                                        viewModel.clearSuggestions()
                                                    }
                                                }
                                            )
                                        }
                                        is SuggestionItem.Hashtag -> {
                                            HashtagSuggestionItem(
                                                hashtag = suggestion,
                                                onClick = {
                                                    val text = postTextFieldValue.text
                                                    val lastHashIndex = text.lastIndexOf('#')
                                                    if (lastHashIndex != -1) {
                                                        val newText = text.substring(0, lastHashIndex) + "#${suggestion.tag} "
                                                        postTextFieldValue = TextFieldValue(
                                                            text = newText,
                                                            selection = TextRange(newText.length)
                                                        )
                                                        showSuggestions = false
                                                        viewModel.clearSuggestions()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    Divider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Character count
            Text(
                text = "${300 - postTextFieldValue.text.length} characters remaining",
                style = MaterialTheme.typography.labelSmall,
                color = if (postTextFieldValue.text.length > 280)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Loading indicator for AI enhancement
        if (aiEnhancedContent is AIEnhancementState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MentionSuggestionItem(
    mention: SuggestionItem.Mention,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mention.avatar)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                mention.displayName?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "@${mention.handle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HashtagSuggestionItem(
    hashtag: SuggestionItem.Hashtag,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tag,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "#${hashtag.tag}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (hashtag.postCount != null) {
                Text(
                    text = "${hashtag.postCount} posts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 