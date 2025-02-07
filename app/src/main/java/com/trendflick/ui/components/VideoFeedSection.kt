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
                    color = Color(0xFF6B4EFF)
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
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B4EFF)
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
                        color = Color.White
                    )
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B4EFF)
                        )
                    ) {
                        Text("Refresh")
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

        FloatingActionButton(
            onClick = onCreateVideo,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF6B4EFF),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create new video",
                modifier = Modifier.size(24.dp)
            )
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
    val view = LocalView.current

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { 
                        showHeartAnimation = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLikeClick()
                    },
                    onTap = { if (video.videoUrl.isNotBlank()) isPaused = !isPaused }
                )
            }
    ) {
        if (video.videoUrl.isNotBlank()) {
            Log.d("VideoFeedSection", "🎥 Playing video: ${video.videoUrl}")
            VideoPlayer(
                videoUrl = video.videoUrl,
                isVisible = isVisible,
                onProgressChanged = { newProgress -> progress = newProgress },
                isPaused = isPaused,
                modifier = Modifier.fillMaxSize(),
                onError = { error -> 
                    Log.e("VideoFeedSection", "❌ Video playback error: $error")
                    loadError = error 
                }
            )
        } else {
            Log.w("VideoFeedSection", "⚠️ No video URL available for: ${video.title}")
            Text(
                text = video.title,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }

        // Author info overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
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
                        .background(Color(0xFF1A1A1A)),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        text = video.authorName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${video.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
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
                onClick = onLikeClick
            )
            EngagementAction(
                icon = Icons.Default.ChatBubble,
                count = video.comments,
                onClick = onCommentClick
            )
            EngagementAction(
                icon = Icons.Default.Share,
                count = video.shares,
                onClick = onShareClick
            )
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                }
            }
        }

        // Progress indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Color(0xFF6B4EFF))
            )
        }

        // Heart animation on double-tap
        if (showHeartAnimation) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFF6B4EFF),
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
    }
}

@Composable
private fun EngagementAction(
    icon: ImageVector,
    count: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

private fun formatCount(count: Int): String = when {
    count < 1000 -> count.toString()
    count < 1000000 -> String.format("%.1fK", count / 1000f)
    else -> String.format("%.1fM", count / 1000000f)
} 