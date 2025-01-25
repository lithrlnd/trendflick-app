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
import android.widget.VideoView
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

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
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
            style = MaterialTheme.typography.titleLarge
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

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
@Composable
fun FlicksScreen(
    navController: NavController,
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

    var permissionsGranted by remember { mutableStateOf(false) }

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
        // Debug test button - always visible
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

        // Debug buttons at the top
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "${videos.size} videos",
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (!permissionsGranted) {
            EmptyState(
                onTestClick = { /* Disabled until permissions granted */ },
                onFolderTestClick = { /* Disabled until permissions granted */ },
                onSmallFileTestClick = { /* Disabled until permissions granted */ }
            )
        } else if (videos.isEmpty()) {
            EmptyState(
                onTestClick = { /* Disabled for now */ },
                onFolderTestClick = { /* Disabled for now */ },
                onSmallFileTestClick = { /* Disabled for now */ }
            )
        } else {
            // Show video count for debugging
            Text(
                text = "${videos.size} videos loaded",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
            
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = videos[page]
                Box(modifier = Modifier.fillMaxSize()) {
                    // Video Player
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                setVideoURI(Uri.parse(video.videoUrl))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                                setOnErrorListener { _, what, extra ->
                                    Log.e("FlicksScreen", "Video playback error: what=$what extra=$extra uri=${video.videoUrl}")
                                    true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Video Info Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        // User info and description
                        Text(
                            text = "@${video.handle}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = video.description,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Interaction buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side - Like and Comment
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = { /* Handle like */ },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Like",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = { /* Handle comment */ },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Comment,
                                        contentDescription = "Comment",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Right side - Share and More options
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = { /* Handle share */ },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = { /* Handle more options */ },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
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
    }
} 