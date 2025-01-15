@file:OptIn(UnstableApi::class)

package com.trendflick.ui.screens.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.media3.common.util.UnstableApi
import com.trendflick.ui.components.VideoPlayer
import com.trendflick.data.model.Video
import com.trendflick.data.model.VideoCategory
import com.trendflick.ui.components.CategoryWheel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import com.trendflick.ui.animation.slideInFromRight
import com.trendflick.ui.animation.slideOutToRight
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.trendflick.ui.components.CategoryDrawer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val likedVideos by viewModel.likedVideos.collectAsState()
    var showDrawer by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<VideoCategory?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { },
                    onDragCancel = { },
                    onDragStart = { offset ->
                        // If the drag starts from the left edge (within 50dp), show drawer
                        if (offset.x < 50.dp.toPx()) {
                            showDrawer = true
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                    }
                )
            }
    ) {
        // Debug indicator - temporary to verify drawer state
        Text(
            text = if (showDrawer) "Drawer Open" else "Drawer Closed",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
                .clickable { showDrawer = !showDrawer }
        )

        // Main content
        if (isLoading) {
            LoadingAnimation()
        } else {
            val pagerState = rememberPagerState(pageCount = { videos.size })
            val coroutineScope = rememberCoroutineScope()

            // Preload next and previous videos
            LaunchedEffect(pagerState.currentPage) {
                viewModel.preloadVideos(
                    currentPage = pagerState.currentPage,
                    videos = videos
                )
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { videos[it].id }
            ) { page ->
                val video = videos[page]
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                            translationY = pageOffset * size.height * 0.1f
                        }
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (!likedVideos.contains(video.id)) {
                                        viewModel.likeVideo(video.id)
                                    }
                                }
                            )
                        }
                ) {
                    VideoItem(
                        video = video,
                        isLiked = likedVideos.contains(video.id),
                        onLikeClick = { viewModel.likeVideo(video.id) },
                        onCommentClick = { viewModel.commentOnVideo(video.id) },
                        onShareClick = { viewModel.shareVideo(video.id) },
                        onProfileClick = { navController.navigate("profile/${video.userId}") },
                        isVisible = page == pagerState.currentPage
                    )
                }
            }
        }

        // Category Drawer
        CategoryDrawer(
            isOpen = showDrawer,
            onCategorySelected = { category ->
                selectedCategory = category
                viewModel.filterByCategory(category)
                showDrawer = false
            },
            onDismiss = { showDrawer = false }
        )

        // Selected category chip
        selectedCategory?.let { category ->
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .clickable { showDrawer = true },
                color = category.color.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category.icon)
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .scale(scale)
                .size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun VideoItem(
    video: Video,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: () -> Unit,
    isVisible: Boolean
) {
    var progress by remember { mutableStateOf(0f) }
    var showLikeAnimation by remember { mutableStateOf(false) }
    var showVideoInfo by remember { mutableStateOf(false) }
    var showRelatedVideos by remember { mutableStateOf(false) }
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLiked) {
                            onLikeClick()
                            showLikeAnimation = true
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            coroutineScope.launch {
                                delay(1000)
                                showLikeAnimation = false
                            }
                        }
                    },
                    onLongPress = {
                        showVideoInfo = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        coroutineScope.launch {
                            delay(2000)
                            showVideoInfo = false
                        }
                    }
                )
            }
    ) {
        VideoPlayer(
            videoUrl = video.videoUrl,
            modifier = Modifier.fillMaxSize(),
            isVisible = isVisible,
            onProgressChanged = { progress = it }
        )

        // Like animation overlay
        AnimatedVisibility(
            visible = showLikeAnimation,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Video info overlay
        AnimatedVisibility(
            visible = showVideoInfo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem(Icons.Default.Favorite, video.likes.toString())
                    StatItem(Icons.Default.ChatBubbleOutline, video.comments.toString())
                    StatItem(Icons.Default.Share, video.shares.toString())
                }
            }
        }

        // Custom video controls with animations
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Video progress
            CustomVideoProgress(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
            )

            // Action buttons
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedActionButton(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    onClick = {
                        onLikeClick()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    isActive = isLiked,
                    count = video.likes
                )
                
                AnimatedActionButton(
                    icon = Icons.Filled.ChatBubbleOutline,
                    onClick = {
                        onCommentClick()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    count = video.comments
                )
                
                AnimatedActionButton(
                    icon = Icons.Filled.Share,
                    onClick = {
                        onShareClick()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                )

                // Add Related Videos Button
                AnimatedActionButton(
                    icon = Icons.Default.PlayCircle,
                    onClick = {
                        showRelatedVideos = !showRelatedVideos
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    isActive = showRelatedVideos
                )
            }

            // User info and hashtags
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 80.dp)
                    .fillMaxWidth(0.8f)
            ) {
                Text(
                    text = "@${video.username}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onProfileClick()
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        )
                    }
                )
                
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Hashtags
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    video.hashtags.forEach { hashtag ->
                        HashtagChip(
                            hashtag = hashtag,
                            onClick = { /* Handle hashtag click */ }
                        )
                    }
                }
            }

            // Related Videos Panel
            AnimatedVisibility(
                visible = showRelatedVideos,
                enter = slideInFromRight,
                exit = slideOutToRight,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(200.dp)
            ) {
                RelatedVideosPanel(
                    relatedVideos = video.relatedVideos,
                    onVideoClick = { /* Handle related video click */ }
                )
            }
        }
    }
}

@Composable
private fun RelatedVideosPanel(
    relatedVideos: List<Video>,
    onVideoClick: (Video) -> Unit
) {
    Column(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
            .padding(8.dp)
    ) {
        Text(
            text = "Related Videos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        relatedVideos.forEach { video ->
            RelatedVideoItem(
                video = video,
                onClick = { onVideoClick(video) }
            )
        }
    }
}

@Composable
private fun RelatedVideoItem(
    video: Video,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
        ) {
            // You can load actual thumbnail here using Coil or other image loading library
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Video info
        Column {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${video.username}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    count: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = count,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CustomVideoProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun AnimatedActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isActive: Boolean = false,
    count: Int = 0
) {
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                scale = 0.8f
                onClick()
                scale = 1f
            },
            modifier = Modifier
                .scale(animatedScale)
                .size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
        
        if (count > 0) {
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> String.format("%.1fK", count / 1000f)
        else -> String.format("%.1fM", count / 1000000f)
    }
}

@Composable
fun HashtagChip(
    hashtag: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text("#$hashtag") },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            labelColor = MaterialTheme.colorScheme.primary
        ),
        border = null,
        modifier = Modifier.height(28.dp)
    )
} 