@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class
)

package com.trendflick.ui.screens.create

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import java.time.LocalTime
import kotlin.math.PI

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

@Composable
private fun AnimatedTimeRing(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val currentTime = LocalTime.now()
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val angle = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing)
        ),
        label = "ring rotation"
    )
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        
        // Draw 12 dots representing hours
        for (i in 0..11) {
            val dotAngle = i * 30f * (PI / 180f)
            val x = center.x + (radius - 8.dp.toPx()) * cos(dotAngle).toFloat()
            val y = center.y + (radius - 8.dp.toPx()) * sin(dotAngle).toFloat()
            
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
        
        // Draw animated arc
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = angle.value,
            sweepAngle = 120f,
            useCenter = false,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
        
        // Draw time markers
        val hourAngle = (currentTime.hour % 12) * 30f + currentTime.minute / 2f
        val minuteAngle = currentTime.minute * 6f
        
        // Hour marker
        val hourX = center.x + (radius - 20.dp.toPx()) * cos(hourAngle * (PI / 180f)).toFloat()
        val hourY = center.y + (radius - 20.dp.toPx()) * sin(hourAngle * (PI / 180f)).toFloat()
        drawLine(
            color = color.copy(alpha = 0.5f),
            start = center,
            end = Offset(hourX, hourY),
            strokeWidth = 2.dp.toPx()
        )
        
        // Minute marker
        val minuteX = center.x + (radius - 12.dp.toPx()) * cos(minuteAngle * (PI / 180f)).toFloat()
        val minuteY = center.y + (radius - 12.dp.toPx()) * sin(minuteAngle * (PI / 180f)).toFloat()
        drawLine(
            color = color.copy(alpha = 0.3f),
            start = center,
            end = Offset(minuteX, minuteY),
            strokeWidth = 1.5f.dp.toPx()
        )
    }
}

@Composable
private fun AnimatedBorder(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6B4EFF)  // Using the consistent purple theme color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val pulseAlpha = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val dotPulse = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot pulse"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cornerRadius = 16.dp.toPx()
        
        // Draw main border
        drawRoundRect(
            color = color.copy(alpha = pulseAlpha.value),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Calculate positions for evenly spaced dots
        val dotSpacing = 20.dp.toPx()
        val topDots = (width / dotSpacing).toInt()
        val sideDots = (height / dotSpacing).toInt()
        
        // Draw dots on all sides
        val dotRadius = 3.dp.toPx()
        
        // Top and bottom dots
        for (i in 0..topDots) {
            val x = (i * dotSpacing).coerceAtMost(width)
            // Top dots
            drawCircle(
                color = color.copy(alpha = (dotPulse.value * (1 - i.toFloat() / topDots))),
                radius = dotRadius,
                center = Offset(x, 0f)
            )
            // Bottom dots
            drawCircle(
                color = color.copy(alpha = (dotPulse.value * (i.toFloat() / topDots))),
                radius = dotRadius,
                center = Offset(x, height)
            )
        }
        
        // Left and right dots
        for (i in 0..sideDots) {
            val y = (i * dotSpacing).coerceAtMost(height)
            // Left dots
            drawCircle(
                color = color.copy(alpha = (dotPulse.value * (1 - i.toFloat() / sideDots))),
                radius = dotRadius,
                center = Offset(0f, y)
            )
            // Right dots
            drawCircle(
                color = color.copy(alpha = (dotPulse.value * (i.toFloat() / sideDots))),
                radius = dotRadius,
                center = Offset(width, y)
            )
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
    var swipeProgress by remember { mutableStateOf(0f) }
    var isEnhancementExpanded by remember { mutableStateOf(false) }
    
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar with Close Button and Character Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigateUp() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Text(
                    text = "${300 - postTextFieldValue.text.length}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (postTextFieldValue.text.length > 280)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground
                )
            }

            // Enhance Button above rectangle
            FloatingActionButton(
                onClick = {
                    isEnhancementExpanded = !isEnhancementExpanded
                    if (postTextFieldValue.text.isNotBlank()) {
                        viewModel.enhancePostWithAI(postTextFieldValue.text)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = if (aiEnhancedContent is AIEnhancementState.Loading)
                        Icons.Default.Refresh
                    else
                        Icons.Default.AutoAwesome,
                    contentDescription = "Enhance"
                )
            }

            // Rectangular Composition Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    // Animated border
                    AnimatedBorder(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Text field
                    OutlinedTextField(
                        value = postTextFieldValue,
                        onValueChange = { newValue ->
                            if (newValue.text.length <= 300) {
                                postTextFieldValue = newValue
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
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = {
                            Text(
                                "What's on your mind?",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        visualTransformation = mentionHashtagTransformation
                    )

                    // Post Button at bottom right with progress
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = postTextFieldValue.text.length / 300f,
                            modifier = Modifier.size(56.dp),
                            color = if (postTextFieldValue.text.length > 280)
                                MaterialTheme.colorScheme.error
                            else Color(0xFF6B4EFF),
                            strokeWidth = 2.dp
                        )
                        
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.createPost(postTextFieldValue.text)
                            },
                            modifier = Modifier.size(48.dp),
                            enabled = !isPosting && postTextFieldValue.text.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isPosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Post",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Suggestions popup (appearing above the cursor)
                    if (showSuggestions && suggestions.isNotEmpty()) {
                        val cursorPosition = postTextFieldValue.selection.start
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(bottom = 8.dp)
                                .heightIn(max = 200.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(suggestions) { suggestion ->
                                    when (suggestion) {
                                        is SuggestionItem.Hashtag -> {
                                            ListItem(
                                                headlineContent = { 
                                                    Text("#${suggestion.tag}") 
                                                },
                                                supportingContent = suggestion.postCount?.let { count ->
                                                    { Text("${formatCount(count)} posts") }
                                                },
                                                modifier = Modifier.clickable {
                                                    val text = postTextFieldValue.text
                                                    val lastHashIndex = text.lastIndexOf('#')
                                                    if (lastHashIndex != -1) {
                                                        val newText = text.substring(0, lastHashIndex) + "#${suggestion.tag} "
                                                        postTextFieldValue = TextFieldValue(
                                                            text = newText,
                                                            selection = TextRange(newText.length)
                                                        )
                                                    }
                                                    showSuggestions = false
                                                }
                                            )
                                        }
                                        is SuggestionItem.Mention -> {
                                            ListItem(
                                                headlineContent = { 
                                                    Text("@${suggestion.handle}") 
                                                },
                                                supportingContent = suggestion.displayName?.let { name ->
                                                    { Text(name) }
                                                },
                                                leadingContent = {
                                                    AsyncImage(
                                                        model = suggestion.avatar,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                },
                                                modifier = Modifier.clickable {
                                                    val text = postTextFieldValue.text
                                                    val lastAtIndex = text.lastIndexOf('@')
                                                    if (lastAtIndex != -1) {
                                                        val newText = text.substring(0, lastAtIndex) + "@${suggestion.handle} "
                                                        postTextFieldValue = TextFieldValue(
                                                            text = newText,
                                                            selection = TextRange(newText.length)
                                                        )
                                                    }
                                                    showSuggestions = false
                                                }
                                            )
                                        }
                                    }
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }

            // Enhancement Panel
            AnimatedVisibility(
                visible = isEnhancementExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        "Enhancement Options",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { /* enhance for engagement */ },
                            label = { Text("✨ Engagement") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { /* enhance for clarity */ },
                            label = { Text("📝 Clarity") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { /* enhance for reach */ },
                            label = { Text("🚀 Reach") }
                        )
                    }
                }
            }
        }

        // Loading Overlay
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
private fun HashtagBubble(
    hashtag: String,
    postCount: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer {
                translationY = sin(System.currentTimeMillis() / 1000f) * 4f
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "#$hashtag",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            postCount?.let {
                Text(
                    text = formatCount(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MentionBubble(
    mention: SuggestionItem.Mention,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer {
                translationY = cos(System.currentTimeMillis() / 1000f) * 4f
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = mention.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Column {
                mention.displayName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = "@${mention.handle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fK", count / 1_000f)
        else -> count.toString()
    }
} 