@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.media3.common.util.UnstableApi::class,
    ExperimentalMaterialApi::class
)

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.trendflick.ui.navigation.Screen
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.trendflick.data.api.FeedPost
import com.trendflick.ui.components.ThreadCard
import com.trendflick.utils.DateUtils
import androidx.compose.foundation.shape.CircleShape
import com.trendflick.data.api.ThreadPost
import com.trendflick.ui.viewmodels.SharedViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import com.trendflick.ui.screens.flicks.FlicksScreen
import com.trendflick.ui.components.SwipeRefresh
import com.trendflick.ui.components.rememberSwipeRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    navController: NavController
) {
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val currentThread by viewModel.currentThread.collectAsState()
    val showComments by viewModel.showComments.collectAsState()
    val currentPostComments by viewModel.currentPostComments.collectAsState()
    val isLoadingComments by viewModel.isLoadingComments.collectAsState()
    val scope = rememberCoroutineScope()
    var commentText by remember { mutableStateOf("") }
    val selectedFeed by sharedViewModel.selectedFeed.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsState()
    val videoLoadError by viewModel.videoLoadError.collectAsState()
    val selectedCategory = remember { mutableStateOf("Trends") }

    LaunchedEffect(selectedFeed) {
        viewModel.updateSelectedFeed(selectedFeed)
    }

    LaunchedEffect(Unit) {
        viewModel.loadMoreThreads()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            refreshing = isRefreshing,
            onRefresh = { viewModel.refresh() }
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Navigation buttons
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFF1A1A1A),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        sharedViewModel.updateSelectedFeed("Trends")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedFeed == "Trends") Color(0xFF6B4EFF) else Color.Transparent,
                                        contentColor = if (selectedFeed == "Trends") Color.White else Color.White.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = selectedCategory.value,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Button(
                                    onClick = { 
                                        sharedViewModel.updateSelectedFeed("Flicks")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedFeed == "Flicks") Color(0xFF6B4EFF) else Color.Transparent,
                                        contentColor = if (selectedFeed == "Flicks") Color.White else Color.White.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "Flicks",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (selectedFeed == "Trends") {
                        Box(modifier = Modifier.fillMaxSize()) {
                            var isDrawerOpen by remember { mutableStateOf(false) }
                            var offsetX by remember { mutableStateOf(0f) }
                            val drawerWidth = 250.dp

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { offset ->
                                                // Increased edge detection area (80dp from edge)
                                                if (offset.x < 80.dp.toPx()) {
                                                    isDrawerOpen = true
                                                }
                                            },
                                            onDragEnd = {
                                                if (offsetX < drawerWidth.toPx() / 2) {
                                                    isDrawerOpen = false
                                                }
                                            },
                                            onDragCancel = {
                                                if (offsetX < drawerWidth.toPx() / 2) {
                                                    isDrawerOpen = false
                                                }
                                            },
                                            onHorizontalDrag = { change, dragAmount ->
                                                change.consume()
                                                offsetX = (offsetX + dragAmount).coerceIn(0f, drawerWidth.toPx())
                                            }
                                        )
                                    }
                            ) {
                                // Add tap area on the left edge
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .fillMaxHeight()
                                        .clickable { isDrawerOpen = true }
                                )

                                // Main content
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (isLoading && threads.isEmpty()) {
                                        LoadingAnimation()
                                    } else if (threads.isEmpty()) {
                                        // Empty state
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "No threads available",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(onClick = { viewModel.loadMoreThreads() }) {
                                                Text("Refresh")
                                            }
                                        }
                                    } else {
                                        val threadCount = threads.size
                                        val pagerState = rememberPagerState(pageCount = { threadCount })

                                        // Load more threads when reaching the end
                                        LaunchedEffect(pagerState.currentPage) {
                                            if (pagerState.currentPage >= threadCount - 2 && !isLoading) {
                                                viewModel.loadMoreThreads()
                                            }
                                        }

                                        VerticalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize(),
                                            key = { index -> threads.getOrNull(index)?.post?.uri ?: index.toString() }
                                        ) { page ->
                                            val thread = threads.getOrNull(page)
                                            if (thread != null) {
                                                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer {
                                                            alpha = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                                                            translationY = pageOffset * size.height * 0.1f
                                                        }
                                                ) {
                                                    ThreadCard(
                                                        feedPost = thread,
                                                        isLiked = likedPosts.contains(thread.post.uri),
                                                        onLikeClick = { viewModel.toggleLike(thread.post.uri) },
                                                        onRepostClick = { viewModel.repost(thread.post.uri) },
                                                        onShareClick = { viewModel.sharePost(thread.post.uri) },
                                                        onProfileClick = { onNavigateToProfile(thread.post.author.did) },
                                                        onThreadClick = { /* TODO */ },
                                                        onCommentClick = {
                                                            viewModel.loadComments(thread.post.uri)
                                                            viewModel.toggleComments(true)
                                                        },
                                                        onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            } else {
                                                // Fallback UI for null thread
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Content unavailable")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Drawer overlay
                                if (isDrawerOpen) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { isDrawerOpen = false }
                                    )
                                }

                                // Category Drawer
                                AnimatedVisibility(
                                    visible = isDrawerOpen,
                                    enter = slideInHorizontally(initialOffsetX = { -it }),
                                    exit = slideOutHorizontally(targetOffsetX = { -it }),
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    CategoryDrawer(
                                        isOpen = isDrawerOpen,
                                        onCategorySelected = { category ->
                                            selectedCategory.value = category
                                            viewModel.filterByCategory(category.lowercase())
                                        },
                                        onHashtagSelected = { hashtag ->
                                            viewModel.setCurrentHashtag(hashtag)
                                        },
                                        trendingHashtags = viewModel.trendingHashtags.value,
                                        currentHashtag = viewModel.currentHashtag.value,
                                        currentCategory = selectedCategory.value,
                                        onDismiss = { isDrawerOpen = false },
                                        modifier = Modifier
                                            .width(300.dp)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }

                        // Show loading indicator when loading more threads
                        if (isLoading && threads.isNotEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            )
                        }

                        // Add FloatingActionButton for creating posts
                        FloatingActionButton(
                            onClick = { navController.navigate(Screen.CreatePost.route) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .navigationBarsPadding(),
                            containerColor = Color(0xFF6B4EFF),
                            contentColor = Color.White
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create new post",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        // Only show VideoFeedSection when on Flicks tab
                        VideoFeedSection(
                            videos = videos,
                            isLoading = isLoadingVideos,
                            error = videoLoadError,
                            onRefresh = { viewModel.refreshVideoFeed() },
                            onCreateVideo = { navController.navigate(Screen.CreateFlick.route) }
                        )
                    }

                    // Comments overlay
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            visible = showComments,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.92f)
                                    .align(Alignment.BottomCenter)
                                    .imePadding(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
                                tonalElevation = 2.dp,
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF1A1A1A))
                                        .padding(horizontal = 16.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.toggleComments(false) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowBack,
                                                    contentDescription = "Back to thread",
                                                    tint = Color.White
                                                )
                                            }
                                            Text(
                                                text = "${currentThread?.replies?.size ?: 0} comments",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White
                                            )
                                        }
                                        
                                        // Add refresh button
                                        IconButton(
                                            onClick = { 
                                                currentThread?.post?.uri?.let { uri ->
                                                    viewModel.loadComments(uri)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh comments",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    // Comments list
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        val thread = currentThread
                                        if (thread != null) {
                                            thread.replies?.forEach { reply ->
                                                item {
                                                    CommentItem(
                                                        comment = reply,
                                                        onProfileClick = { did -> onNavigateToProfile(did) },
                                                        originalPostAuthorDid = thread.post.author.did
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Comment input
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .navigationBarsPadding(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = commentText,
                                            onValueChange = { commentText = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            placeholder = { 
                                                Text(
                                                    "Add a comment...",
                                                    color = Color.White.copy(alpha = 0.5f)
                                                )
                                            },
                                            maxLines = 3,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF6B4EFF),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                                cursorColor = Color(0xFF6B4EFF),
                                                unfocusedTextColor = Color.White,
                                                focusedTextColor = Color.White,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedContainerColor = Color.Transparent
                                            )
                                        )
                                        IconButton(
                                            onClick = {
                                                if (commentText.isNotBlank()) {
                                                    scope.launch {
                                                        viewModel.postComment(currentThread?.post?.uri ?: "", commentText)
                                                        commentText = ""
                                                    }
                                                }
                                            },
                                            enabled = commentText.isNotBlank()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "Post comment",
                                                tint = if (commentText.isNotBlank()) 
                                                    Color(0xFF6B4EFF)
                                                else 
                                                    Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
            if (video.thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = "Video thumbnail",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

@Composable
private fun CommentItem(
    comment: ThreadPost,
    level: Int = 0,
    onProfileClick: (String) -> Unit,
    isOriginalPoster: Boolean = false,
    originalPostAuthorDid: String? = null,
    modifier: Modifier = Modifier
) {
    val isOP = originalPostAuthorDid != null && comment.post.author.did == originalPostAuthorDid

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp)
            .padding(vertical = 8.dp)
            .background(
                if (isOP) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thread line indicator
        if (level > 0) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(
                        if (isOP) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture
                AsyncImage(
                    model = comment.post.author.avatar,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onProfileClick(comment.post.author.did) },
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = comment.post.author.displayName ?: comment.post.author.handle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isOP) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "OP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "@${comment.post.author.handle} · ${DateUtils.formatTimestamp(comment.post.record.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = comment.post.record.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 40.dp)
            )
            
            // Comment actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "Replies",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${comment.replies?.size ?: 0}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // Recursively render replies
    comment.replies?.forEach { reply ->
        CommentItem(
            comment = reply,
            level = level + 1,
            onProfileClick = onProfileClick,
            originalPostAuthorDid = originalPostAuthorDid,
            modifier = modifier
        )
    }
}

@Composable
fun VideoFeedSection(
    videos: List<Video>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onCreateVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center),
                    color = Color(0xFF6B4EFF)
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = onRefresh,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF6B4EFF)
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
            videos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "No videos found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Pull down to refresh or create your first flick",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(videos) { index, video ->
                        VideoItem(
                            video = video,
                            isLiked = false,
                            onLikeClick = { /* TODO: Implement like action */ },
                            onCommentClick = { /* TODO: Implement comment action */ },
                            onShareClick = { /* TODO: Implement share action */ },
                            onProfileClick = { /* TODO: Implement profile navigation */ },
                            isVisible = true,
                            onLongPress = { /* TODO: Implement long press action */ }
                        )
                        if (index < videos.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Update FloatingActionButton
        FloatingActionButton(
            onClick = onCreateVideo,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding(),
            containerColor = Color(0xFF6B4EFF),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create new flick",
                modifier = Modifier.size(24.dp)
            )
        }
    }
} 