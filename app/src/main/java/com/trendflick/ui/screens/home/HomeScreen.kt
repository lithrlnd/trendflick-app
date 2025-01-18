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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.HorizontalPager
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
import com.trendflick.data.model.Comment
import com.trendflick.ui.components.CommentDialog
import com.trendflick.ui.components.VideoControls
import android.content.Intent
import android.content.Context
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val likedVideos by viewModel.likedVideos.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val likedComments by viewModel.likedComments.collectAsState()
    var showDrawer by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showRelatedVideos by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<VideoCategory?>(null) }
    var currentVideoId by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (offset.x < 50.dp.toPx()) {
                            showDrawer = true
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                    },
                    onDragEnd = {},
                    onDragCancel = {}
                )
            }
    ) {
        if (isLoading) {
            LoadingAnimation()
        } else {
            val pagerState = rememberPagerState(pageCount = { videos.size })

            LaunchedEffect(pagerState.currentPage) {
                viewModel.preloadVideos(
                    currentPage = pagerState.currentPage,
                    videos = videos
                )
                currentVideoId = videos.getOrNull(pagerState.currentPage)?.id
                showRelatedVideos = false
            }

            if (isLandscape) {
                // Horizontal pager for landscape mode
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val video = videos[page]
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                                translationX = pageOffset * size.width * 0.1f
                            }
                            .fillMaxSize()
                    ) {
                        VideoItem(
                            video = video,
                            isLiked = likedVideos.contains(video.id),
                            onLikeClick = { viewModel.likeVideo(video.id) },
                            onCommentClick = { 
                                currentVideoId = video.id
                                showComments = true
                            },
                            onShareClick = { viewModel.shareVideo(video.id) },
                            onProfileClick = { navController.navigate("profile/${video.userId}") },
                            isVisible = page == pagerState.currentPage,
                            onLongPress = {
                                showRelatedVideos = !showRelatedVideos
                            }
                        )
                    }
                }
            } else {
                // Vertical pager for portrait mode
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
                    ) {
                        VideoItem(
                            video = video,
                            isLiked = likedVideos.contains(video.id),
                            onLikeClick = { viewModel.likeVideo(video.id) },
                            onCommentClick = { 
                                currentVideoId = video.id
                                showComments = true
                            },
                            onShareClick = { viewModel.shareVideo(video.id) },
                            onProfileClick = { navController.navigate("profile/${video.userId}") },
                            isVisible = page == pagerState.currentPage,
                            onLongPress = {
                                showRelatedVideos = !showRelatedVideos
                            }
                        )
                    }
                }
            }

            // Related videos panel
            AnimatedVisibility(
                visible = showRelatedVideos,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.7f)
            ) {
                RelatedVideosPanel(
                    relatedVideos = videos[pagerState.currentPage].relatedVideos,
                    onVideoClick = { relatedVideo ->
                        val index = videos.indexOfFirst { it.id == relatedVideo.id }
                        if (index != -1) {
                            showRelatedVideos = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    }
                )
            }
        }

        CategoryDrawer(
            isOpen = showDrawer,
            onCategorySelected = { category ->
                selectedCategory = category
                viewModel.filterByCategory(category)
                showDrawer = false
            },
            onDismiss = { showDrawer = false }
        )

        currentVideoId?.let { videoId ->
            CommentDialog(
                isVisible = showComments,
                onDismiss = { showComments = false },
                comments = comments[videoId] ?: emptyList(),
                onCommentSubmit = { content -> 
                    viewModel.commentOnVideo(videoId, content)
                },
                onCommentLike = { commentId ->
                    viewModel.likeComment(commentId)
                },
                onReplyClick = { commentId ->
                    viewModel.replyToComment(videoId, commentId)
                }
            )
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
    isVisible: Boolean,
    onLongPress: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var playbackSpeed by remember { mutableStateOf(1f) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        VideoPlayer(
            videoUrl = video.videoUrl,
            isVisible = isVisible,
            onProgressChanged = { newProgress -> progress = newProgress },
            playbackSpeed = playbackSpeed,
            isPaused = isPaused,
            modifier = Modifier.fillMaxSize()
        )

        VideoControls(
            video = video,
            isLiked = isLiked,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
            onProfileClick = onProfileClick,
            onRelatedVideosClick = onLongPress,
            progress = progress,
            isPaused = isPaused,
            onPauseToggle = { isPaused = !isPaused },
            playbackSpeed = playbackSpeed,
            onSpeedChange = { playbackSpeed = it },
            isLandscape = isLandscape,
            modifier = Modifier.fillMaxSize()
        )
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