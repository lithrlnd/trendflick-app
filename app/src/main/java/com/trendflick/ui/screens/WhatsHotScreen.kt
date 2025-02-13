@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.trendflick.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trendflick.viewmodel.WhatsHotViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import com.trendflick.ui.components.ThreadCard
import com.trendflick.ui.components.VideoFeedSection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.navigation.NavController
import com.trendflick.ui.navigation.Screen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.trendflick.data.api.FeedPost

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun WhatsHotScreen(
    onNavigateToProfile: (String) -> Unit,
    navController: NavController,
    viewModel: WhatsHotViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var selectedFeed by remember { mutableStateOf("Trends") }
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val repostedPosts by viewModel.repostedPosts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsState()
    val videoLoadError by viewModel.videoLoadError.collectAsState()
    val listState = rememberLazyListState()

    // Initialize with trends feed
    LaunchedEffect(selectedFeed) {
        if (selectedFeed == "Flicks") {
            viewModel.refreshVideoFeed()
        } else {
            viewModel.filterByCategory("whats-hot")
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { 
            if (selectedFeed == "Flicks") {
                viewModel.refreshVideoFeed()
            } else {
                viewModel.filterByCategory("whats-hot")
            }
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top navigation buttons
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { selectedFeed = "Trends" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFeed == "Trends") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedFeed == "Trends") 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                        onClick = { selectedFeed = "Flicks" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFeed == "Flicks") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedFeed == "Flicks") 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 68.dp)
                .pullRefresh(pullRefreshState)
        ) {
            if (selectedFeed == "Flicks") {
                VideoFeedSection(
                    videos = videos,
                    isLoading = isLoadingVideos,
                    error = videoLoadError,
                    onRefresh = { viewModel.refreshVideoFeed() },
                    onCreateVideo = { navController.navigate(Screen.CreateFlick.route) }
                )
            } else {
                if (isLoading && threads.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = threads,
                            key = { it.post.uri }
                        ) { feedPost ->
                            ThreadCard(
                                feedPost = feedPost,
                                isLiked = likedPosts.contains(feedPost.post.uri),
                                isReposted = repostedPosts.contains(feedPost.post.uri),
                                onLikeClick = { viewModel.toggleLike(feedPost.post.uri) },
                                onRepostClick = { viewModel.repost(feedPost.post.uri) },
                                onShareClick = { viewModel.sharePost(feedPost.post.uri) },
                                onProfileClick = { onNavigateToProfile(feedPost.post.author.did) },
                                onThreadClick = { viewModel.loadThread(feedPost.post.uri) },
                                onCommentClick = {
                                    viewModel.loadComments(feedPost.post.uri)
                                    viewModel.toggleComments(true)
                                },
                                onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                                onImageClick = { /* Handle image click */ },
                                onHashtagClick = { tag -> viewModel.onHashtagSelected(tag) },
                                onLinkClick = { /* Handle link click */ },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // FAB for creating new content
        FloatingActionButton(
            onClick = { 
                if (selectedFeed == "Trends") {
                    navController.navigate(Screen.CreatePost.route)
                } else {
                    navController.navigate(Screen.CreateFlick.route)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (selectedFeed == "Trends") 
                    "Create new post" 
                else 
                    "Create new video",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
} 