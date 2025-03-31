package com.trendflick.ui.screens.flicks

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.trendflick.ui.components.EngagementColumn
import com.trendflick.ui.components.VideoPlayer
import kotlinx.coroutines.launch

/**
 * Landscape mode for FlicksScreen with optimized horizontal layout
 * Provides smooth horizontal swiping experience with side-by-side engagement column
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LandscapeFlicksScreen(
    navController: NavController,
    viewModel: FlicksViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videos by viewModel.videos.collectAsState()
    val currentVideoIndex by viewModel.currentVideoIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.loadVideos() }
    )
    
    val pagerState = rememberPagerState(
        initialPage = currentVideoIndex,
        pageCount = { videos.size }
    )
    
    // Keep pager and viewModel in sync
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentVideoIndex(pagerState.currentPage)
    }
    
    LaunchedEffect(currentVideoIndex) {
        if (currentVideoIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(currentVideoIndex)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pullRefresh(pullRefreshState)
    ) {
        if (videos.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No videos available",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { viewModel.loadVideos() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6B4EFF)
                            )
                        ) {
                            Text("Refresh")
                        }
                    }
                }
            }
        } else {
            // Content with videos
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main video content (70% of width)
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        orientation = Orientation.Horizontal
                    ) { page ->
                        val video = videos[page]
                        
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Video player
                            VideoPlayer(
                                videoUrl = video.mediaUrl ?: "",
                                isPlaying = page == currentVideoIndex,
                                onPlayPauseToggle = { /* Toggle play/pause */ },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Video info overlay at bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Author info
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = video.authorName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        
                                        Text(
                                            text = "@${video.authorHandle}",
                                            color = Color.LightGray,
                                            fontSize = 14.sp
                                        )
                                    }
                                    
                                    // Follow button
                                    OutlinedButton(
                                        onClick = { /* Follow user */ },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Follow")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Video description
                                Text(
                                    text = video.content,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                
                                // Hashtags
                                if (video.hashtags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(video.hashtags) { hashtag ->
                                            Surface(
                                                color = Color(0xFF6B4EFF).copy(alpha = 0.3f),
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = "#$hashtag",
                                                    color = Color(0xFF6B4EFF),
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Engagement column (30% of width)
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .background(Color(0xFF121212))
                ) {
                    if (currentVideoIndex < videos.size) {
                        val currentVideo = videos[currentVideoIndex]
                        
                        EngagementColumn(
                            post = currentVideo,
                            onLikeClick = { /* Like video */ },
                            onCommentClick = { /* Open comments */ },
                            onShareClick = { /* Share video */ },
                            onProfileClick = { 
                                navController.navigate("profile/${currentVideo.authorHandle}")
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        
        // Pull to refresh indicator
        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color(0xFF6B4EFF),
            contentColor = Color.White
        )
    }
}
