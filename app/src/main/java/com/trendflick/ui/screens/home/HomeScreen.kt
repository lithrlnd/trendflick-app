@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.media3.common.util.UnstableApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
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
import androidx.compose.ui.graphics.Brush
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
import com.trendflick.ui.components.SwipeRefresh
import com.trendflick.ui.components.rememberSwipeRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.style.TextAlign
import com.trendflick.ui.components.RichTextRenderer
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.trendflick.data.api.Facet
import com.trendflick.ui.components.RichTextPostOverlay
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.PagerDefaults

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    navController: NavController
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val repostedPosts by viewModel.repostedPosts.collectAsState()
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

    // Collect share events
    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { shareIntent ->
            try {
                val chooserIntent = Intent.createChooser(shareIntent, "Share via")
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to share: ${e.message}")
            }
        }
    }

    LaunchedEffect(selectedFeed) {
        viewModel.updateSelectedFeed(selectedFeed)
        if (selectedFeed == "Flicks") {
            Log.d("HomeScreen", "🎥 Switching to Flicks tab, refreshing video feed")
            viewModel.refreshVideoFeed()
        }
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
                                        text = "Trends",
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
                        if (isLoading && threads.isEmpty()) {
                            LoadingAnimation()
                        } else if (threads.isEmpty()) {
                            EmptyState(onRefresh = { viewModel.loadMoreThreads() })
                        } else {
                            val pagerState = rememberPagerState(pageCount = { threads.size })

                            LaunchedEffect(pagerState.currentPage) {
                                if (pagerState.currentPage >= threads.size - 2 && !isLoading) {
                                    viewModel.loadMoreThreads()
                                }
                            }

                            if (isLandscape) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    val thread = threads.getOrNull(page)
                                    if (thread != null) {
                                        ThreadCard(
                                            feedPost = thread,
                                            isLiked = likedPosts.contains(thread.post.uri),
                                            isReposted = repostedPosts.contains(thread.post.uri),
                                            onLikeClick = { viewModel.toggleLike(thread.post.uri) },
                                            onRepostClick = { viewModel.repost(thread.post.uri) },
                                            onShareClick = { viewModel.sharePost(thread.post.uri) },
                                            onProfileClick = { onNavigateToProfile(thread.post.author.did) },
                                            onThreadClick = { /* Handle thread click */ },
                                            onCommentClick = {
                                                viewModel.loadComments(thread.post.uri)
                                                viewModel.toggleComments(true)
                                            },
                                            onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                                            onImageClick = { image ->
                                                // Handle image click by opening in full screen or in a viewer
                                                val imageUrl = image.fullsize ?: image.image?.link?.let { link ->
                                                    "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                                                } ?: ""
                                                // You can implement your image viewing logic here
                                            },
                                            onHashtagClick = { tag -> viewModel.onHashtagSelected(tag) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                VerticalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    key = { threads[it].post.uri },
                                    pageSpacing = 1.dp,
                                    userScrollEnabled = true,
                                    beyondBoundsPageCount = 1,
                                    flingBehavior = PagerDefaults.flingBehavior(
                                        state = pagerState,
                                        snapAnimationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                ) { page ->
                                    val thread = threads.getOrNull(page)
                                    if (thread != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black)
                                        ) {
                                            ThreadCard(
                                                feedPost = thread,
                                                isLiked = likedPosts.contains(thread.post.uri),
                                                isReposted = repostedPosts.contains(thread.post.uri),
                                                onLikeClick = { viewModel.toggleLike(thread.post.uri) },
                                                onRepostClick = { viewModel.repost(thread.post.uri) },
                                                onShareClick = { viewModel.sharePost(thread.post.uri) },
                                                onProfileClick = { onNavigateToProfile(thread.post.author.did) },
                                                onThreadClick = { /* Handle thread click */ },
                                                onCommentClick = {
                                                    viewModel.loadComments(thread.post.uri)
                                                    viewModel.toggleComments(true)
                                                },
                                                onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                                                onImageClick = { image -> /* Handle image click */ },
                                                onHashtagClick = { tag -> viewModel.onHashtagSelected(tag) },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Show loading indicator when loading more threads
                        if (isLoading && threads.isNotEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                color = Color(0xFF6B4EFF)
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
                                    CommentsHeader(
                                        showAuthorOnly = viewModel.showAuthorOnly.collectAsState().value,
                                        onBackClick = { viewModel.toggleComments(false) },
                                        onAuthorOnlyChange = { viewModel.toggleAuthorOnly() },
                                        onRefreshClick = { 
                                            currentThread?.post?.uri?.let { uri ->
                                                viewModel.loadComments(uri)
                                            }
                                        }
                                    )

                                    currentThread?.let { thread ->
                                        CommentsList(
                                            thread = thread,
                                            showAuthorOnly = viewModel.showAuthorOnly.collectAsState().value,
                                            onProfileClick = { did -> /* Handle profile navigation */ }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add hashtag filter indicator if active
                    AnimatedVisibility(
                        visible = viewModel.currentHashtag.collectAsState().value != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${viewModel.currentHashtag.collectAsState().value}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                IconButton(onClick = { viewModel.clearHashtagFilter() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear hashtag filter",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
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
    val context = LocalContext.current
    val view = LocalView.current
    val viewModel: HomeViewModel = hiltViewModel()
    val repostedPosts by viewModel.repostedPosts.collectAsState()
    
    var playbackSpeed by remember { mutableStateOf(1f) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showRichText by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLikeClick()
                    },
                    onTap = { if (!video.isImage) isPaused = !isPaused },
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        showRichText = true
                        Log.d("VideoItem", """
                            Long press detected:
                            URI: ${video.uri}
                            Is Image: ${video.isImage}
                            Description: ${video.description}
                            Caption: ${video.caption}
                            Facets: ${video.facets?.size ?: 0}
                            Author: ${video.authorName}
                        """.trimIndent())
                    }
                )
            }
    ) {
        if (video.isImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = video.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentScale = when {
                        video.aspectRatio > 1f -> ContentScale.FillWidth
                        video.aspectRatio < 1f -> ContentScale.FillHeight
                        else -> ContentScale.Fit
                    },
                    onError = { loadError = "Failed to load image" }
                )
            }
        } else if (video.videoUrl.isNotBlank()) {
            VideoPlayer(
                videoUrl = video.videoUrl,
                isVisible = isVisible,
                onProgressChanged = { newProgress -> progress = newProgress },
                playbackSpeed = playbackSpeed,
                isPaused = isPaused,
                modifier = Modifier.fillMaxSize(),
                onError = { error -> loadError = "Failed to load video: $error" }
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

        // Author info
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        text = video.authorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${video.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Engagement column
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            EngagementColumn(
                isLiked = isLiked,
                isReposted = repostedPosts.contains(video.uri),
                likeCount = video.likes,
                replyCount = video.comments,
                repostCount = video.shares,
                onLikeClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLikeClick()
                },
                onCommentClick = onCommentClick,
                onRepostClick = { viewModel.repost(video.uri) },
                onShareClick = onShareClick
            )
        }

        // Rich text overlay
        RichTextPostOverlay(
            visible = showRichText,
            text = video.caption.ifBlank { video.description },
            facets = video.facets ?: emptyList(),
            onDismiss = { 
                showRichText = false
                Log.d("VideoItem", """
                    Dismissing overlay:
                    Was showing: $showRichText
                    Caption length: ${video.caption.length}
                    Description length: ${video.description.length}
                    Facets count: ${video.facets?.size ?: 0}
                """.trimIndent())
            }
        )
    }
}

@Composable
private fun EngagementColumn(
    isLiked: Boolean,
    isReposted: Boolean,
    likeCount: Int,
    replyCount: Int,
    repostCount: Int,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .height(52.dp)
                .padding(end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Like
            EngagementAction(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                count = likeCount,
                isActive = isLiked,
                onClick = onLikeClick,
                tint = if (isLiked) Color(0xFF6B4EFF) else Color(0xFFB4A5FF),
                isHorizontal = true
            )

            // Comment
            EngagementAction(
                icon = Icons.Default.ChatBubbleOutline,
                count = replyCount,
                onClick = onCommentClick,
                isHorizontal = true
            )

            // Repost
            EngagementAction(
                icon = Icons.Default.Repeat,
                count = repostCount,
                isActive = isReposted,
                onClick = onRepostClick,
                tint = if (isReposted) Color(0xFF6B4EFF) else Color(0xFFB4A5FF),
                isHorizontal = true
            )

            // Share
            EngagementAction(
                icon = Icons.Default.Share,
                onClick = onShareClick,
                isHorizontal = true
            )
        }
    } else {
        Column(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Like
            EngagementAction(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                count = likeCount,
                isActive = isLiked,
                onClick = onLikeClick,
                tint = if (isLiked) Color(0xFF6B4EFF) else Color(0xFFB4A5FF)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Comment
            EngagementAction(
                icon = Icons.Default.ChatBubbleOutline,
                count = replyCount,
                onClick = onCommentClick
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Repost
            EngagementAction(
                icon = Icons.Default.Repeat,
                count = repostCount,
                isActive = isReposted,
                onClick = onRepostClick,
                tint = if (isReposted) Color(0xFF6B4EFF) else Color(0xFFB4A5FF)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Share
            EngagementAction(
                icon = Icons.Default.Share,
                onClick = onShareClick
            )

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
private fun EngagementAction(
    icon: ImageVector,
    count: Int = 0,
    isActive: Boolean = false,
    tint: Color = Color(0xFFB4A5FF),
    isHorizontal: Boolean = false,
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

    if (isHorizontal) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    scale = 0.8f
                    onClick()
                    scale = 1f
                },
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (count > 0) {
                Text(
                    text = formatEngagementCount(count),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB4A5FF)
                )
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    scale = 0.8f
                    onClick()
                    scale = 1f
                },
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (count > 0) {
                Text(
                    text = formatEngagementCount(count),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB4A5FF)
                )
            }
        }
    }
}

private fun formatEngagementCount(count: Int): String = when {
    count < 1000 -> count.toString()
    count < 1000000 -> String.format("%.1fK", count / 1000f)
    else -> String.format("%.1fM", count / 1000000f)
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
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val viewModel: HomeViewModel = hiltViewModel()
    val isOP = originalPostAuthorDid != null && comment.post.author.did == originalPostAuthorDid
    val remainingChars = 300 - replyText.length

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp)
            .padding(vertical = 8.dp)
            .background(
                if (isOP) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        // Existing comment content
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
        
        RichTextRenderer(
            text = comment.post.record.text,
            facets = comment.post.record.facets ?: emptyList(),
            onMentionClick = { did -> onProfileClick(did) },
            onHashtagClick = { /* Handle hashtag click */ },
            onLinkClick = { /* Handle link click */ },
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
                modifier = Modifier.clickable { showReplyInput = !showReplyInput },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Reply,
                    contentDescription = "Reply",
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

        // Reply input field
        AnimatedVisibility(visible = showReplyInput) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { 
                            if (it.length <= 300) {
                                replyText = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a reply...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6B4EFF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF6B4EFF),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        supportingText = {
                            Text(
                                text = "$remainingChars",
                                color = if (remainingChars < 50) 
                                    Color(0xFFFF4B4B) 
                                else 
                                    Color.White.copy(alpha = 0.5f)
                            )
                        }
                    )
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank() && replyText.length <= 300) {
                                scope.launch {
                                    viewModel.postComment(
                                        parentUri = comment.post.uri,
                                        text = replyText
                                    )
                                    replyText = ""
                                    showReplyInput = false
                                }
                            }
                        },
                        enabled = replyText.isNotBlank() && replyText.length <= 300
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send reply",
                            tint = if (replyText.isNotBlank() && replyText.length <= 300) 
                                Color(0xFF6B4EFF)
                            else 
                                Color.White.copy(alpha = 0.5f)
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
    val viewModel: HomeViewModel = hiltViewModel()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val repostedPosts by viewModel.repostedPosts.collectAsState()
    val showComments by viewModel.showComments.collectAsState()
    val currentThread by viewModel.currentThread.collectAsState()
    val showAuthorOnly by viewModel.showAuthorOnly.collectAsState()

    LaunchedEffect(videos, isLoading, error) {
        Log.d("VideoFeedSection", """
            📱 Video Feed State:
            Loading: $isLoading
            Error: $error
            Videos count: ${videos.size}
            First video URI: ${videos.firstOrNull()?.uri}
        """.trimIndent())
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Log.d("VideoFeedSection", "🔄 Showing loading state")
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFF6B4EFF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loading media feed...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            error != null -> {
                Log.d("VideoFeedSection", "❌ Showing error state: $error")
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            Log.d("VideoFeedSection", "🔄 Retry button clicked")
                            onRefresh()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B4EFF)
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
            videos.isEmpty() -> {
                Log.d("VideoFeedSection", "⚠️ No videos found")
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "No media found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Pull down to refresh or create your first flick",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            Log.d("VideoFeedSection", "🔄 Manual refresh triggered")
                            onRefresh()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B4EFF)
                        )
                    ) {
                        Text("Refresh Feed")
                    }
                }
            }
            else -> {
                Log.d("VideoFeedSection", "✅ Showing ${videos.size} videos")
                val pagerState = rememberPagerState(pageCount = { videos.size })
                
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { videos[it].uri },
                    pageSpacing = 1.dp,
                    userScrollEnabled = true,
                    beyondBoundsPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapAnimationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) { page ->
                    val video = videos[page]
                    var itemLoadError by remember { mutableStateOf<String?>(null) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        if (itemLoadError == null) {
                            VideoItem(
                                video = video,
                                isLiked = likedPosts.contains(video.uri),
                                onLikeClick = { viewModel.toggleLike(video.uri) },
                                onCommentClick = {
                                    viewModel.loadComments(video.uri)
                                    viewModel.toggleComments(true)
                                },
                                onShareClick = { viewModel.sharePost(video.uri) },
                                onProfileClick = { /* TODO: Implement profile navigation */ },
                                isVisible = page == pagerState.currentPage,
                                onLongPress = { /* TODO: Implement long press action */ }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = itemLoadError ?: "Failed to load media",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Comments overlay
                if (showComments) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.92f)
                            .align(Alignment.BottomCenter)
                            .imePadding(),
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            CommentsHeader(
                                showAuthorOnly = showAuthorOnly,
                                onBackClick = { viewModel.toggleComments(false) },
                                onAuthorOnlyChange = { viewModel.toggleAuthorOnly() },
                                onRefreshClick = { 
                                    currentThread?.post?.uri?.let { uri ->
                                        viewModel.loadComments(uri)
                                    }
                                }
                            )

                            currentThread?.let { thread ->
                                CommentsList(
                                    thread = thread,
                                    showAuthorOnly = showAuthorOnly,
                                    onProfileClick = { did -> /* Handle profile navigation */ }
                                )
                            }
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

@Composable
private fun CommentsHeader(
    showAuthorOnly: Boolean,
    onBackClick: () -> Unit,
    onAuthorOnlyChange: (Boolean) -> Unit,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Close comments",
                tint = Color.White
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (showAuthorOnly) "Author Only" else "All Comments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Switch(
                    checked = showAuthorOnly,
                    onCheckedChange = onAuthorOnlyChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6B4EFF),
                        checkedTrackColor = Color(0xFF6B4EFF).copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }

        IconButton(onClick = onRefreshClick) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh comments",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CommentsList(
    thread: ThreadPost,
    showAuthorOnly: Boolean,
    onProfileClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val replies = if (showAuthorOnly) {
            thread.replies?.filter { it.post.author.did == thread.post.author.did }
        } else {
            thread.replies
        }

        replies?.let { filteredReplies ->
            items(
                items = filteredReplies,
                key = { it.post.uri }
            ) { reply ->
                CommentItem(
                    comment = reply,
                    onProfileClick = onProfileClick,
                    originalPostAuthorDid = thread.post.author.did
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No posts available",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
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