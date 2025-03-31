package com.trendflick.ui.screens.hashtag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.trendflick.data.model.Post
import com.trendflick.ui.components.EnhancedPostItem
import com.trendflick.ui.components.HashtagPill
import com.trendflick.ui.viewmodels.HashtagViewModel
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch

/**
 * Screen for displaying posts with a specific hashtag
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun HashtagScreen(
    navController: NavController,
    hashtag: String,
    viewModel: HashtagViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val isLoading by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { /* Refresh data */ }
    )
    
    // Track hashtag view for analytics
    LaunchedEffect(hashtag) {
        viewModel.trackHashtagUsage(hashtag)
    }
    
    // Mock posts for the hashtag
    val posts = remember {
        List(10) { index ->
            Post(
                id = "post_$index",
                authorName = "User ${index + 1}",
                authorHandle = "user${index + 1}",
                authorAvatar = null,
                content = "This is a post with #$hashtag and some other content. #trending",
                timestamp = System.currentTimeMillis() - (index * 3600000),
                likes = (10..500).random(),
                comments = (0..50).random(),
                reposts = (0..30).random(),
                hashtags = listOf(hashtag, "trending"),
                mentions = emptyList(),
                mediaUrl = if (index % 3 == 0) "https://example.com/image.jpg" else null,
                isVideo = index % 6 == 0
            )
        }
    }
    
    // Tab state
    val tabs = listOf("Top", "Latest", "Videos", "Photos")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "#$hashtag",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${posts.size} posts",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Related hashtags
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Related:",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("trending", "viral", "fyp", "foryou").forEach { relatedTag ->
                                HashtagPill(
                                    hashtag = relatedTag,
                                    onClick = {
                                        navController.navigate("hashtag/$relatedTag")
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Tabs
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color(0xFF121212),
                contentColor = Color(0xFF6B4EFF),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        height = 3.dp,
                        color = Color(0xFF6B4EFF)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = Color(0xFF6B4EFF),
                        unselectedContentColor = Color.Gray
                    )
                }
            }
            
            // Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        val filteredPosts = when (page) {
                            0 -> posts.sortedByDescending { it.likes }
                            1 -> posts.sortedByDescending { it.timestamp }
                            2 -> posts.filter { it.isVideo }
                            3 -> posts.filter { it.mediaUrl != null && !it.isVideo }
                            else -> posts
                        }
                        
                        if (filteredPosts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No posts found",
                                        color = Color.Gray,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredPosts) { post ->
                                EnhancedPostItem(
                                    post = post,
                                    onLikeClick = { /* Like post */ },
                                    onCommentClick = { /* Open comments */ },
                                    onShareClick = { /* Share post */ },
                                    onProfileClick = { navController.navigate("profile/${post.authorHandle}") },
                                    onHashtagClick = { clickedHashtag ->
                                        navController.navigate("hashtag/$clickedHashtag")
                                    },
                                    onMentionClick = { username ->
                                        navController.navigate("profile/$username")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                    
                    PullRefreshIndicator(
                        refreshing = isLoading,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = Color(0xFF6B4EFF),
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}
