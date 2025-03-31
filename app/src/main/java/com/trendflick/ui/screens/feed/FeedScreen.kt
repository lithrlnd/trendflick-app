package com.trendflick.ui.screens.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.trendflick.ui.components.EnhancedPostItem
import com.trendflick.ui.viewmodels.FeedViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import com.trendflick.ui.screens.flicks.FlicksScreen
import kotlinx.coroutines.launch

/**
 * Main feed screen with toggle between Trends and Flicks
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFeed by viewModel.selectedFeed.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    val pagerState = rememberPagerState(initialPage = if (selectedFeed == "Trends") 0 else 1) { 2 }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(selectedFeed) {
        val page = if (selectedFeed == "Trends") 0 else 1
        if (pagerState.currentPage != page) {
            pagerState.animateScrollToPage(page)
        }
    }
    
    LaunchedEffect(pagerState.currentPage) {
        val feed = if (pagerState.currentPage == 0) "Trends" else "Flicks"
        if (selectedFeed != feed) {
            viewModel.updateSelectedFeed(feed)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            modifier = Modifier.width(200.dp),
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    height = 3.dp,
                                    color = Color(0xFF6B4EFF)
                                )
                            }
                        ) {
                            Tab(
                                selected = pagerState.currentPage == 0,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                text = {
                                    Text(
                                        "Trends",
                                        fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                            Tab(
                                selected = pagerState.currentPage == 1,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                text = {
                                    Text(
                                        "Flicks",
                                        fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        navController.navigate("create_post")
                    } else {
                        navController.navigate("create_flick")
                    }
                },
                containerColor = Color(0xFF6B4EFF),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (pagerState.currentPage == 0) "Create Post" else "Create Flick"
                )
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> TrendsTab(
                        posts = posts,
                        isLoading = isLoading,
                        navController = navController,
                        viewModel = viewModel,
                        isLandscape = isLandscape
                    )
                    1 -> FlicksScreen(
                        navController = navController,
                        isLandscape = isLandscape
                    )
                }
            }
            
            if (isLoading && posts.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF6B4EFF)
                )
            }
        }
    }
}

@Composable
private fun TrendsTab(
    posts: List<com.trendflick.data.model.Post>,
    isLoading: Boolean,
    navController: NavController,
    viewModel: FeedViewModel,
    isLandscape: Boolean
) {
    val lazyListState = rememberLazyListState()
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = posts,
            key = { it.id }
        ) { post ->
            EnhancedPostItem(
                post = post,
                onLikeClick = { viewModel.toggleLikePost(post.id) },
                onCommentClick = { navController.navigate("post/${post.id}/comments") },
                onRepostClick = { viewModel.repostPost(post.id) },
                onShareClick = { viewModel.sharePost(post.id) },
                onProfileClick = { username -> navController.navigate("profile/$username") },
                onHashtagClick = { hashtag -> navController.navigate("hashtag/$hashtag") },
                onMentionClick = { username -> navController.navigate("profile/$username") },
                onPostClick = { postId -> navController.navigate("post/$postId") },
                onLinkClick = { url -> /* Handle link click */ }
            )
        }
        
        if (isLoading && posts.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF6B4EFF),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        if (!isLoading && posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No posts yet",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Follow more accounts to see posts",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { navController.navigate("discover") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6B4EFF)
                            )
                        ) {
                            Text("Discover Accounts")
                        }
                    }
                }
            }
        }
    }
}
