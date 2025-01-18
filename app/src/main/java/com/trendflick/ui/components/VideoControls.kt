package com.trendflick.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trendflick.data.model.Video
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoControls(
    video: Video,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRelatedVideosClick: () -> Unit,
    isLiked: Boolean = false,
    progress: Float = 0f,
    isPaused: Boolean = false,
    onPauseToggle: () -> Unit,
    playbackSpeed: Float = 1f,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showHeartAnimation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Reset controls visibility when orientation changes
    LaunchedEffect(isLandscape) {
        controlsVisible = true
        if (isLandscape) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Handle auto-hide in landscape mode
    LaunchedEffect(controlsVisible, isLandscape) {
        if (isLandscape && controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (isLandscape && !controlsVisible) 0f else 1f,
        animationSpec = tween(300),
        label = "fadeAlpha"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLiked) {
                            onLikeClick()
                            showHeartAnimation = true
                            coroutineScope.launch {
                                delay(1000)
                                showHeartAnimation = false
                            }
                        }
                    },
                    onTap = {
                        controlsVisible = !controlsVisible
                        if (!isLandscape) {
                            onPauseToggle()
                        }
                    }
                )
            }
    ) {
        // Heart Animation
        AnimatedVisibility(
            visible = showHeartAnimation,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(100.dp)
            )
        }

        // Top Header Container - fades in landscape
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer(alpha = fadeAlpha)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 80.dp else 120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = if (isLandscape) 0.3f else 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Bottom Footer Container - fades in landscape
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer(alpha = fadeAlpha)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 120.dp else 200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = if (isLandscape) 0.6f else 0.8f)
                            )
                        )
                    )
            )

            // Progress Bar
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            // Bottom content
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Column {
                    Text(
                        text = "@${video.userId}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right side controls - always visible
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = if (isLandscape) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 16.dp else 20.dp)
        ) {
            // Profile Button
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Speed Button
            IconButton(
                onClick = { 
                    onSpeedChange(when (playbackSpeed) {
                        1f -> 1.5f
                        1.5f -> 2f
                        else -> 1f
                    })
                },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Text(
                    text = "${playbackSpeed}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Like Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color.Red else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = formatCount(video.likes),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comment Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comment",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = formatCount(video.commentCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = formatCount(video.shares),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Related Videos Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onRelatedVideosClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = "Related Videos",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Related",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
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