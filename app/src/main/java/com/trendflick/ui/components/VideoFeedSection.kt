package com.trendflick.ui.components

import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trendflick.data.model.Video
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.delay
import com.trendflick.ui.components.RichTextPostOverlay

@UnstableApi
@Composable
fun VideoFeedSection(
    videos: List<Video>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onCreateVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
            videos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No videos available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Refresh",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(videos) { video ->
                        val isVisible = remember {
                            derivedStateOf {
                                val itemInfo = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == video.uri }
                                itemInfo != null
                            }
                        }

                        VideoItem(
                            video = video,
                            isVisible = isVisible.value,
                            onLikeClick = { /* TODO: Implement like action */ },
                            onCommentClick = { /* TODO: Implement comment action */ },
                            onShareClick = { /* TODO: Implement share action */ },
                            onProfileClick = { /* TODO: Implement profile navigation */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f/16f)
                        )
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun VideoItem(
    video: Video,
    isVisible: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showHeartAnimation by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    var showRichText by remember { mutableStateOf(false) }
    val view = LocalView.current

    // Check if the video URL is an oEmbed URL
    val isOEmbedVideo = video.videoUrl.contains("embed.bsky.app/oembed") || 
                        video.videoUrl.contains("youtube.com/embed") ||
                        video.videoUrl.contains("player.vimeo.com")
                        
    // Auto-retry logic for videos
    LaunchedEffect(video.videoUrl, retryCount) {
        if (loadError != null && retryCount > 0 && retryCount <= 3) {
            delay(1000) // Wait a second before retrying
            loadError = null // Clear the error to trigger a reload
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { 
                        showHeartAnimation = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLikeClick()
                    },
                    onTap = { if (video.videoUrl.isNotBlank() && !isOEmbedVideo) isPaused = !isPaused },
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        showRichText = true
                    }
                )
            }
    ) {
        if (video.videoUrl.isNotBlank()) {
            Log.d("VideoFeedSection", """
                🎥 Playing video: 
                URL: ${video.videoUrl}
                Is oEmbed: $isOEmbedVideo
                Thumbnail: ${video.thumbnailUrl}
                Retry Count: $retryCount
            """.trimIndent())
            
            // Get thumbnail URL if needed
            val thumbnailUrl = when {
                video.thumbnailUrl.isNotBlank() -> video.thumbnailUrl
                video.videoUrl.contains("youtube.com") || video.videoUrl.contains("youtu.be") -> {
                    val videoId = extractYouTubeVideoId(video.videoUrl)
                    if (videoId.isNotBlank()) {
                        "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                    } else null
                }
                video.videoUrl.contains("vimeo.com") -> {
                    val videoId = extractVimeoVideoId(video.videoUrl)
                    if (videoId.isNotBlank()) {
                        "https://vumbnail.com/$videoId.jpg"
                    } else null
                }
                else -> null
            }
            
            // Try to fix common URL issues
            val fixedVideoUrl = when {
                // Ensure HTTPS for all URLs
                video.videoUrl.startsWith("http://") -> 
                    video.videoUrl.replace("http://", "https://")
                // Add proper protocol if missing
                !video.videoUrl.startsWith("http") && !video.videoUrl.startsWith("content://") ->
                    "https://${video.videoUrl}"
                else -> video.videoUrl
            }
            
            VideoPlayer(
                videoUrl = fixedVideoUrl,
                isVisible = isVisible,
                onProgressChanged = { newProgress -> progress = newProgress },
                isPaused = isPaused,
                modifier = Modifier.fillMaxSize(),
                onError = { error -> 
                    Log.e("VideoFeedSection", "❌ Video playback error: $error")
                    loadError = error
                    // Auto-retry once for common errors
                    if (retryCount == 0 && (error.contains("Source error") || error.contains("Failed to load"))) {
                        retryCount++
                    }
                },
                thumbnailUrl = thumbnailUrl,
                isOEmbedVideo = isOEmbedVideo
            )
        } else {
            Log.w("VideoFeedSection", "⚠️ No video URL available for: ${video.title}")
            
            // Fallback content display
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Text(
                        text = video.title.ifBlank { "Media content unavailable" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Long press to view post details",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Error state
        loadError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = error,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        // Add a retry button
                        Button(
                            onClick = { 
                                loadError = null
                                retryCount++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Retry")
                        }
                        
                        // Add a "View Post Text" button
                        Button(
                            onClick = { showRichText = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("View Post Text")
                        }
                    }
                }
            }
        }

        // Progress indicator (only for non-oEmbed videos)
        if (!isOEmbedVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Author info overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = video.authorAvatar,
                    contentDescription = "Author avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        text = video.authorName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${video.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Engagement actions
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EngagementAction(
                icon = Icons.Default.Favorite,
                count = video.likes,
                isActive = video.likes > 0,
                onClick = onLikeClick
            )
            EngagementAction(
                icon = Icons.Default.ChatBubble,
                count = video.comments,
                isActive = video.comments > 0,
                onClick = onCommentClick
            )
            EngagementAction(
                icon = Icons.Default.Share,
                count = video.shares,
                isActive = video.shares > 0,
                onClick = onShareClick
            )
        }

        // Heart animation on double-tap
        if (showHeartAnimation) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp)
                    .graphicsLayer {
                        scaleX = 1.2f
                        scaleY = 1.2f
                        alpha = 0.8f
                    }
            )

            LaunchedEffect(showHeartAnimation) {
                delay(800)
                showHeartAnimation = false
            }
        }
        
        // Rich text overlay
        if (showRichText) {
            RichTextPostOverlay(
                visible = true,
                text = video.caption.ifBlank { video.description },
                facets = video.facets ?: emptyList(),
                onDismiss = { showRichText = false }
            )
        }
    }
}

@Composable
private fun EngagementAction(
    icon: ImageVector,
    count: Int = 0,
    isActive: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    onClick: () -> Unit
) {
    val view = LocalView.current
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .background(
                    color = if (isActive) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                    else 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    scale = 0.8f
                    onClick()
                    scale = 1f
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else tint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

private fun formatCount(count: Int): String = when {
    count < 1000 -> count.toString()
    count < 1000000 -> String.format("%.1fK", count / 1000f)
    else -> String.format("%.1fM", count / 1000000f)
}

// Helper functions for video ID extraction
private fun extractYouTubeVideoId(url: String): String {
    val pattern = """(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})"""
    val regex = Regex(pattern)
    return regex.find(url)?.groupValues?.get(1) ?: ""
}

private fun extractVimeoVideoId(url: String): String {
    val pattern = """vimeo\.com\/(?:.*#|.*/videos/)?([0-9]+)"""
    val regex = Regex(pattern)
    return regex.find(url)?.groupValues?.get(1) ?: ""
} 