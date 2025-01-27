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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.foundation.ExperimentalFoundationApi
import android.util.Log
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FollowingScreen(
    viewModel: FollowingViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onCreatePost: () -> Unit
) {
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val likedPosts by viewModel.likedPosts.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // Log state changes
    LaunchedEffect(threads, isLoading, isRefreshing) {
        Log.d("FollowingScreen", """
            📱 Screen State Update:
            Threads count: ${threads.size}
            Is Loading: $isLoading
            Is Refreshing: $isRefreshing
            First thread text: ${threads.firstOrNull()?.post?.record?.text?.take(50)}
        """.trimIndent())
    }

    LaunchedEffect(Unit) {
        Log.d("FollowingScreen", "🚀 Initial load triggered")
        viewModel.loadMoreThreads()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                                    onShareClick = { /* TODO */ },
                                    onProfileClick = { onNavigateToProfile(thread.post.author.did) },
                                    onThreadClick = { /* TODO */ },
                                    onCommentClick = { /* TODO */ },
                                    onCreatePost = onCreatePost,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
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
                contentDescription = "Create new post"
            )
        }

        // Show loading indicator when loading more
        if (isLoading && threads.isNotEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
} 
