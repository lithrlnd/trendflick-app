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
import com.trendflick.ui.components.RichTextRenderer
import android.util.Log
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.trendflick.data.model.TrendingHashtag

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    navController: NavController
) {
    val selectedFeed by viewModel.selectedFeed.collectAsState()
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentCategory by viewModel.currentCategory.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val trendingHashtags by viewModel.trendingHashtags.collectAsState()
    val currentHashtag by viewModel.currentHashtag.collectAsState()
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (offset.x < with(density) { 80.dp.toPx() }) {
                            viewModel.openDrawer()
                        }
                    },
                    onDragEnd = { },
                    onDragCancel = { },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Feed Selection
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
                            onClick = { viewModel.updateSelectedFeed("Trends") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedFeed == "Trends" || selectedFeed !in listOf("Trends", "Flicks")) 
                                    Color(0xFF6B4EFF) 
                                else 
                                    Color.Transparent,
                                contentColor = if (selectedFeed == "Trends" || selectedFeed !in listOf("Trends", "Flicks")) 
                                    Color.White 
                                else 
                                    Color.White.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = when {
                                    selectedFeed == "Trends" -> "Trends"
                                    currentCategory.isNotEmpty() -> currentCategory
                                    !currentHashtag.isNullOrEmpty() -> "#$currentHashtag"
                                    else -> selectedFeed
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { viewModel.updateSelectedFeed("Flicks") },
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

            // Content Area
            Box(modifier = Modifier.weight(1f)) {
                if (selectedFeed == "Flicks") {
                    FlicksScreen(navController = navController)
                } else {
                    if (isLoading && threads.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                            color = Color(0xFF6B4EFF)
                        )
                    } else if (threads.isEmpty()) {
                        Text(
                            text = "No posts found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { threads.size })
                        
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { index -> threads[index].post.uri }
                        ) { index ->
                            val thread = threads[index]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 8.dp)
                            ) {
                                ThreadCard(
                                    feedPost = thread,
                                    onProfileClick = { onNavigateToProfile(thread.post.author.did) },
                                    modifier = Modifier.fillMaxSize(),
                                    isLiked = viewModel.likedPosts.collectAsState().value.contains(thread.post.uri),
                                    isReposted = viewModel.repostedPosts.collectAsState().value.contains(thread.post.uri),
                                    onLikeClick = { viewModel.toggleLike(thread.post.uri) },
                                    onRepostClick = { viewModel.repost(thread.post.uri) },
                                    onShareClick = { viewModel.sharePost(thread.post.uri) },
                                    onThreadClick = { viewModel.loadThread(thread.post.uri) },
                                    onCommentClick = { 
                                        viewModel.loadComments(thread.post.uri)
                                        viewModel.toggleComments(true)
                                    },
                                    onCreatePost = { /* Will be implemented when needed */ },
                                    onHashtagClick = { hashtag -> viewModel.onHashtagSelected(hashtag) }
                                )

                                // Comments Dialog
                                val showComments by viewModel.showComments.collectAsState()
                                val currentThread by viewModel.currentThread.collectAsState()
                                val currentPostComments by viewModel.currentPostComments.collectAsState()
                                
                                if (showComments && currentThread != null) {
                                    AlertDialog(
                                        onDismissRequest = { viewModel.toggleComments(false) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        containerColor = Color(0xFF1A1A1A),
                                        properties = DialogProperties(
                                            dismissOnBackPress = true,
                                            dismissOnClickOutside = true,
                                            usePlatformDefaultWidth = false
                                        ),
                                        content = {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            ) {
                                                // Header with filter toggle
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 16.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
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
                                                        val showAuthorOnly by viewModel.showAuthorOnly.collectAsState()
                                                        Text(
                                                            text = if (showAuthorOnly) "Author Only" else "All Comments",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = Color.White.copy(alpha = 0.7f)
                                                        )
                                                        Switch(
                                                            checked = showAuthorOnly,
                                                            onCheckedChange = { viewModel.toggleAuthorOnly() },
                                                            colors = SwitchDefaults.colors(
                                                                checkedThumbColor = Color(0xFF6B4EFF),
                                                                checkedTrackColor = Color(0xFF6B4EFF).copy(alpha = 0.5f),
                                                                uncheckedThumbColor = Color.White,
                                                                uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                                                            )
                                                        )
                                                    }
                                                }

                                                FilteredCommentsList(
                                                    thread = currentThread!!,
                                                    viewModel = viewModel,
                                                    onProfileClick = onNavigateToProfile
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Load more posts when reaching the end
                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.currentPage }.collect { page ->
                                if (page >= threads.size - 2) {
                                    viewModel.loadMoreThreads()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Drawer
        CategoryDrawer(
            isOpen = isDrawerOpen,
            onCategorySelected = { category ->
                viewModel.filterByCategory(category)
            },
            onHashtagSelected = { hashtag ->
                viewModel.onHashtagSelected(hashtag)
            },
            trendingHashtags = trendingHashtags,
            currentHashtag = currentHashtag?.takeIf { it.isNotEmpty() },
            currentCategory = currentCategory,
            onDismiss = { viewModel.closeDrawer() }
        )
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
                    items(videos) { video ->
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
private fun FilteredCommentsList(
    thread: ThreadPost,
    viewModel: HomeViewModel,
    onProfileClick: (String) -> Unit
) {
    val showAuthorOnly by viewModel.showAuthorOnly.collectAsState()
    val allCommentsState = rememberLazyListState()
    val authorCommentsState = rememberLazyListState()
    
    val filteredReplies = remember(thread, showAuthorOnly) {
        if (showAuthorOnly) {
            thread.replies?.filter { reply ->
                reply.post.author.did == thread.post.author.did
            }
        } else {
            thread.replies
        }
    }
    
    LazyColumn(
        state = if (showAuthorOnly) authorCommentsState else allCommentsState,
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        filteredReplies?.let { replies ->
            items(
                items = replies,
                key = { reply -> reply.post.uri }
            ) { reply ->
                CommentItem(
                    comment = reply,
                    onProfileClick = onProfileClick,
                    originalPostAuthorDid = thread.post.author.did,
                    showAuthorOnly = showAuthorOnly
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