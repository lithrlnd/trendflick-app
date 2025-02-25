@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.trendflick.ui.screens.following

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.trendflick.ui.components.ThreadCard
import com.trendflick.ui.components.VideoFeedSection
import androidx.compose.foundation.ExperimentalFoundationApi
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import com.trendflick.data.model.Video
import com.trendflick.ui.viewmodels.SharedViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun FollowingScreen(
    viewModel: FollowingViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onCreatePost: () -> Unit
) {
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val repostedPosts by viewModel.repostedPosts.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsState()
    val videoLoadError by viewModel.videoLoadError.collectAsState()
    val selectedFeed by sharedViewModel.selectedFeed.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var currentBrowserUrl by remember { mutableStateOf<String?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // Enhanced logging for state changes
    LaunchedEffect(threads, isLoading, isRefreshing) {
        Log.d("FollowingScreen", """
            📱 Screen State Update:
            • Selected Feed: $selectedFeed
            • Threads count: ${threads.size}
            • Videos count: ${videos.size}
            • Is Loading: $isLoading
            • Is Loading Videos: $isLoadingVideos
            • Is Refreshing: $isRefreshing
            • First thread details: ${
                threads.firstOrNull()?.let { thread ->
                    """
                    - URI: ${thread.post.uri}
                    - Author: ${thread.post.author.handle}
                    - Text: ${thread.post.record.text.take(50)}...
                    """
                } ?: "No threads available"
            }
            • Liked posts count: ${likedPosts.size}
            • Reposted posts count: ${repostedPosts.size}
        """.trimIndent())
    }

    // Feed switching effect
    LaunchedEffect(selectedFeed) {
        if (isTransitioning) return@LaunchedEffect
        
        try {
            isTransitioning = true
            Log.d("FollowingScreen", "🔄 Switching feed to: $selectedFeed")
            
            // Clear current content before switching
            if (selectedFeed == "Flicks") {
                viewModel.clearThreads()
            } else {
                viewModel.clearVideos()
            }
            
            // Add delay for smooth transition
            delay(300)
            
            viewModel.updateSelectedFeed(selectedFeed)
            
            // Initial load based on selected feed
            if (selectedFeed == "Flicks") {
                Log.d("FollowingScreen", "🎥 Initializing video feed")
                viewModel.refreshVideoFeed()
            } else {
                Log.d("FollowingScreen", "📱 Initializing trends feed")
                viewModel.refreshThreads()
            }
            
            isTransitioning = false
            
        } catch (e: Exception) {
            Log.e("FollowingScreen", "❌ Error switching feeds: ${e.message}")
            isTransitioning = false
        }
    }

    // Initial load effect
    LaunchedEffect(Unit) {
        Log.d("FollowingScreen", "🚀 Initial load triggered")
        if (selectedFeed == "Flicks") {
            viewModel.refreshVideoFeed()
        } else {
            viewModel.refreshThreads()
        }
    }

    // Load follow status for visible posts
    LaunchedEffect(threads) {
        if (threads.isNotEmpty()) {
            viewModel.loadFollowStatusForVisiblePosts()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Feed selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { 
                                if (!isTransitioning && selectedFeed != "Trends") {
                                    sharedViewModel.updateSelectedFeed("Trends")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedFeed == "Trends") 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surface,
                                contentColor = if (selectedFeed == "Trends") 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.height(36.dp),
                            enabled = !isLoading && !isLoadingVideos && !isTransitioning
                        ) {
                            Text("Trends")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { 
                                if (!isTransitioning && selectedFeed != "Flicks") {
                                    sharedViewModel.updateSelectedFeed("Flicks")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedFeed == "Flicks") 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surface,
                                contentColor = if (selectedFeed == "Flicks") 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.height(36.dp),
                            enabled = !isLoading && !isLoadingVideos && !isTransitioning
                        ) {
                            Text("Flicks")
                        }
                    }
                }
            }

            // Content area
            Box(modifier = Modifier.weight(1f)) {
                when (selectedFeed) {
                    "Trends" -> {
                        SwipeRefresh(
                            state = swipeRefreshState,
                            onRefresh = { 
                                Log.d("FollowingScreen", "🔄 Refresh triggered")
                                viewModel.refreshThreads() 
                            }
                        ) {
                            when {
                                isLoading && threads.isEmpty() -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                threads.isEmpty() && !isLoading && !isRefreshing -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "No posts found",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { 
                                                Log.d("FollowingScreen", "🔄 Manual refresh triggered")
                                                viewModel.loadMoreThreads() 
                                            }
                                        ) {
                                            Text("Refresh")
                                        }
                                    }
                                }
                                else -> {
                                    val pagerState = rememberPagerState(pageCount = { threads.size })

                                    LaunchedEffect(pagerState.currentPage) {
                                        if (pagerState.currentPage >= threads.size - 2 && !isLoading && !isRefreshing) {
                                            Log.d("FollowingScreen", "📜 Loading more threads at page ${pagerState.currentPage}")
                                            viewModel.loadMoreThreads()
                                        }
                                    }

                                    if (isLandscape) {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize(),
                                            pageSize = PageSize.Fill
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
                                                        isFollowing = viewModel.followedUsers.collectAsState().value.contains(thread.post.author.did),
                                                        isFollowingLoading = viewModel.isFollowingLoading.collectAsState().value.contains(thread.post.author.did),
                                                        onLikeClick = { viewModel.toggleLike(thread.post.uri) },
                                                        onRepostClick = { viewModel.repost(thread.post.uri) },
                                                        onShareClick = { viewModel.sharePost(thread.post.uri) },
                                                        onProfileClick = { onNavigateToProfile(thread.post.author.did) },
                                                        onThreadClick = { /* TODO: Implement thread click */ },
                                                        onCommentClick = { /* TODO: Implement comments */ },
                                                        onCreatePost = onCreatePost,
                                                        onFollowClick = { viewModel.toggleFollow(thread.post.author.did) },
                                                        onImageClick = { image ->
                                                            val imageUrl = image.fullsize ?: image.image?.link?.let { link ->
                                                                "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                                                            } ?: ""
                                                            image // Return the image to satisfy the type requirement
                                                        },
                                                        onHashtagClick = { /* TODO: Implement hashtag click */ },
                                                        onLinkClick = { url -> currentBrowserUrl = url },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        VerticalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize(),
                                            key = { threads[it].post.uri },
                                            pageSize = PageSize.Fill,
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
                                                        isFollowing = viewModel.followedUsers.collectAsState().value.contains(thread.post.author.did),
                                                        isFollowingLoading = viewModel.isFollowingLoading.collectAsState().value.contains(thread.post.author.did),
                                                        onLikeClick = { viewModel.toggleLike(thread.post.uri) },
                                                        onRepostClick = { viewModel.repost(thread.post.uri) },
                                                        onShareClick = { viewModel.sharePost(thread.post.uri) },
                                                        onProfileClick = { onNavigateToProfile(thread.post.author.did) },
                                                        onThreadClick = { /* TODO: Implement thread click */ },
                                                        onCommentClick = { /* TODO: Implement comments */ },
                                                        onCreatePost = onCreatePost,
                                                        onFollowClick = { viewModel.toggleFollow(thread.post.author.did) },
                                                        onImageClick = { image ->
                                                            val imageUrl = image.fullsize ?: image.image?.link?.let { link ->
                                                                "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                                                            } ?: ""
                                                            image // Return the image to satisfy the type requirement
                                                        },
                                                        onHashtagClick = { /* TODO: Implement hashtag click */ },
                                                        onLinkClick = { url -> currentBrowserUrl = url },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Flicks" -> {
                        VideoFeedSection(
                            videos = videos,
                            isLoading = isLoadingVideos,
                            error = videoLoadError,
                            onRefresh = { viewModel.refreshVideoFeed() },
                            onCreateVideo = onCreatePost,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Show loading indicator when loading more
        if ((isLoading && threads.isNotEmpty()) || (isLoadingVideos && videos.isNotEmpty())) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // Floating Action Button
        if (!isLandscape) {
            FloatingActionButton(
                onClick = onNavigateToCreatePost,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                containerColor = Color(0xFF6B4EFF),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedFeed == "Trends") "Create new post" else "Create new video"
                )
            }
        }
    }
} 
