package com.trendflick.ui.screens.flicks

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.trendflick.data.model.Video
import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.trendflick.utils.PermissionUtils
import com.trendflick.ui.components.RequestPermissions
import com.trendflick.ui.components.VideoPlayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.util.UnstableApi
import com.trendflick.ui.components.VideoControls
import com.trendflick.ui.components.CommentDialog
import com.trendflick.ui.components.EngagementColumn

/**
 * Enhanced FlicksScreen that shows videos only with proper engagement column functionality
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    UnstableApi::class
)
@Composable
fun FlicksScreen(
    navController: NavController,
    isLandscape: Boolean = false,
    viewModel: FlicksViewModel = hiltViewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pagerState = rememberPagerState(pageCount = { videos.size })
    val context = LocalContext.current
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.loadVideos() }
    )
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionsGranted by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var currentVideoIndex by remember { mutableStateOf(0) }
    var currentPlaybackProgress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }

    // Filter to ensure only videos are displayed (not images)
    val filteredVideos = remember(videos) {
        videos.filter { !it.isImage }
    }

    RequestPermissions(
        onPermissionsGranted = {
            permissionsGranted = true
            viewModel.loadVideos()
        },
        onPermissionsDenied = {
            permissionsGranted = false
        }
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pullRefresh(pullRefreshState)
    ) {
        // Debug test button - only visible in debug mode
        if (viewModel.isDebugMode()) {
            Button(
                onClick = { viewModel.testSmallFileUpload() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoading) Color.Gray else Color(0xFF4EFF8A)
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Test Storage", color = Color.Black, fontSize = 12.sp)
                }
            }
        }

        if (!permissionsGranted) {
            EmptyState(
                onTestClick = { /* Disabled until permissions granted */ },
                onFolderTestClick = { /* Disabled until permissions granted */ },
                onSmallFileTestClick = { /* Disabled until permissions granted */ }
            )
        } else if (filteredVideos.isEmpty()) {
            EmptyState(
                onTestClick = { viewModel.testVideoInFolder() },
                onFolderTestClick = { viewModel.testFolderAccess() },
                onSmallFileTestClick = { viewModel.testSmallFileUpload() }
            )
        } else {
            // Show video count for debugging only in debug mode
            if (viewModel.isDebugMode()) {
                Text(
                    text = "${filteredVideos.size} videos loaded",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
            
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> filteredVideos[index].uri }
            ) { page ->
                val video = filteredVideos[page]
                currentVideoIndex = page
                
                Box(modifier = Modifier.fillMaxSize()) {
                    // Video Player using ExoPlayer instead of VideoView for better performance
                    VideoPlayer(
                        videoUrl = video.videoUrl,
                        modifier = Modifier.fillMaxSize(),
                        isVisible = page == pagerState.currentPage,
                        onProgressChanged = { progress -> 
                            currentPlaybackProgress = progress
                        },
                        isPaused = isPaused,
                        playbackSpeed = playbackSpeed
                    )

                    // Video Controls Overlay
                    if (isLandscape) {
                        // Landscape layout with side-by-side engagement column
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(0.8f)) {
                                VideoControls(
                                    video = video,
                                    onLikeClick = { viewModel.likeVideo(video.uri) },
                                    onCommentClick = { showComments = true },
                                    onShareClick = { viewModel.shareVideo(video.uri) },
                                    onProfileClick = { navController.navigate("profile/${video.authorHandle}") },
                                    onRelatedVideosClick = { /* Show related videos */ },
                                    isLiked = video.isLiked,
                                    progress = currentPlaybackProgress,
                                    isPaused = isPaused,
                                    onPauseToggle = { isPaused = !isPaused },
                                    playbackSpeed = playbackSpeed,
                                    onSpeedChange = { newSpeed -> playbackSpeed = newSpeed },
                                    modifier = Modifier.fillMaxSize(),
                                    isLandscape = true
                                )
                            }
                            
                            // Engagement column on the right in landscape mode
                            EngagementColumn(
                                video = video,
                                onLikeClick = { viewModel.likeVideo(video.uri) },
                                onCommentClick = { showComments = true },
                                onShareClick = { viewModel.shareVideo(video.uri) },
                                onProfileClick = { navController.navigate("profile/${video.authorHandle}") },
                                isLiked = video.isLiked,
                                modifier = Modifier
                                    .width(80.dp)
                                    .fillMaxHeight()
                                    .padding(end = 16.dp)
                            )
                        }
                    } else {
                        // Portrait layout with overlay controls
                        VideoControls(
                            video = video,
                            onLikeClick = { viewModel.likeVideo(video.uri) },
                            onCommentClick = { showComments = true },
                            onShareClick = { viewModel.shareVideo(video.uri) },
                            onProfileClick = { navController.navigate("profile/${video.authorHandle}") },
                            onRelatedVideosClick = { /* Show related videos */ },
                            isLiked = video.isLiked,
                            progress = currentPlaybackProgress,
                            isPaused = isPaused,
                            onPauseToggle = { isPaused = !isPaused },
                            playbackSpeed = playbackSpeed,
                            onSpeedChange = { newSpeed -> playbackSpeed = newSpeed },
                            modifier = Modifier.fillMaxSize(),
                            isLandscape = false
                        )
                    }
                }
            }
        }

        // Show pull to refresh indicator
        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color(0xFF6B4EFF),
            contentColor = Color.White
        )

        // Add FloatingActionButton for creating new flicks
        FloatingActionButton(
            onClick = { navController.navigate("create_flick") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding(),
            containerColor = Color(0xFF6B4EFF),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create new flick"
            )
        }
        
        // Comments Dialog
        if (showComments && filteredVideos.isNotEmpty() && currentVideoIndex < filteredVideos.size) {
            val currentVideo = filteredVideos[currentVideoIndex]
            CommentDialog(
                isVisible = showComments,
                onDismiss = { showComments = false },
                comments = currentVideo.comments ?: emptyList(),
                onCommentSubmit = { comment -> 
                    viewModel.addComment(currentVideo.uri, comment)
                },
                onCommentLike = { commentId ->
                    viewModel.likeComment(currentVideo.uri, commentId)
                },
                onReplyClick = { commentId ->
                    viewModel.replyToComment(currentVideo.uri, commentId)
                }
            )
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    UnstableApi::class
)
@Composable
fun EmptyState(
    onTestClick: () -> Unit,
    onFolderTestClick: () -> Unit,
    onSmallFileTestClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No videos found",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        
        Button(
            onClick = onTestClick,
            modifier = Modifier.padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EFF))
        ) {
            Text("Test Video Upload", color = Color.White)
        }
        
        Button(
            onClick = onFolderTestClick,
            modifier = Modifier.padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E8AFF))
        ) {
            Text("Test Folder Access", color = Color.White)
        }

        Button(
            onClick = onSmallFileTestClick,
            modifier = Modifier.padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4EFF8A))
        ) {
            Text("Test Small File Upload", color = Color.White)
        }
    }
}
