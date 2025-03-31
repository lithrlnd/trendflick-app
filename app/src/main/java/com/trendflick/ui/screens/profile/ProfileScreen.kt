package com.trendflick.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.trendflick.data.model.Post
import com.trendflick.data.model.UserProfile
import com.trendflick.ui.components.EnhancedPostItem
import com.trendflick.ui.components.VideoThumbnail
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

/**
 * Enhanced ProfileScreen with follow/unfollow functionality and proper navigation
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    username: String? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isCurrentUser = username == null || username == viewModel.getCurrentUserHandle()
    val isFollowing = remember { mutableStateOf(false) }
    
    // Load user profile
    LaunchedEffect(username) {
        viewModel.loadUserProfile(username)
    }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.loadUserProfile(username) }
    )
    
    // Tab state
    val tabs = listOf("Posts", "Videos", "Likes", "Media")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = userProfile?.displayName ?: "Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isCurrentUser) {
                        IconButton(onClick = { /* Share profile */ }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share profile"
                            )
                        }
                    } else {
                        IconButton(onClick = { /* Edit profile */ }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit profile"
                            )
                        }
                    }
                    
                    IconButton(onClick = { /* Open menu */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            if (userProfile == null && isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF6B4EFF)
                    )
                }
            } else if (userProfile == null) {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "User not found",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { viewModel.loadUserProfile(username) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6B4EFF)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                // Profile content
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Profile header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Avatar and stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF333333))
                                ) {
                                    if (userProfile?.avatarUrl != null) {
                                        AsyncImage(
                                            model = userProfile?.avatarUrl,
                                            contentDescription = "Profile picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Stats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ProfileStat(
                                        count = userProfile?.posts ?: 0,
                                        label = "Posts"
                                    )
                                    
                                    ProfileStat(
                                        count = userProfile?.followers ?: 0,
                                        label = "Followers"
                                    )
                                    
                                    ProfileStat(
                                        count = userProfile?.following ?: 0,
                                        label = "Following"
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Name and bio
                            Text(
                                text = userProfile?.displayName ?: "",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "@${userProfile?.handle ?: ""}",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            
                            if (!userProfile?.bio.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = userProfile?.bio ?: "",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Follow/Edit button
                            if (isCurrentUser) {
                                // Edit profile button
                                OutlinedButton(
                                    onClick = { /* Edit profile */ },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Edit Profile")
                                }
                            } else {
                                // Follow/Unfollow button
                                Button(
                                    onClick = { 
                                        isFollowing.value = !isFollowing.value
                                        if (isFollowing.value) {
                                            viewModel.followUser(userProfile?.handle ?: "")
                                        } else {
                                            viewModel.unfollowUser(userProfile?.handle ?: "")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing.value) Color.Gray else Color(0xFF6B4EFF)
                                    )
                                ) {
                                    Text(if (isFollowing.value) "Following" else "Follow")
                                }
                            }
                        }
                    }
                    
                    // Tabs
                    item {
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
                    }
                    
                    // Content
                    item {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp)
                        ) { page ->
                            when (page) {
                                0 -> PostsGrid(
                                    posts = userProfile?.recentPosts ?: emptyList(),
                                    onPostClick = { postId ->
                                        navController.navigate("post/$postId")
                                    },
                                    onHashtagClick = { hashtag ->
                                        navController.navigate("hashtag/$hashtag")
                                    },
                                    onMentionClick = { username ->
                                        navController.navigate("profile/$username")
                                    }
                                )
                                1 -> VideosGrid(
                                    videos = userProfile?.recentVideos ?: emptyList(),
                                    onVideoClick = { videoId ->
                                        navController.navigate("video/$videoId")
                                    }
                                )
                                2 -> LikesGrid(
                                    likes = userProfile?.recentLikes ?: emptyList(),
                                    onPostClick = { postId ->
                                        navController.navigate("post/$postId")
                                    }
                                )
                                3 -> MediaGrid(
                                    media = userProfile?.recentMedia ?: emptyList(),
                                    onMediaClick = { mediaId ->
                                        navController.navigate("media/$mediaId")
                                    }
                                )
                            }
                        }
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

/**
 * Profile stat component (posts, followers, following)
 */
@Composable
fun ProfileStat(
    count: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatCount(count),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

/**
 * Posts grid component
 */
@Composable
fun PostsGrid(
    posts: List<Post>,
    onPostClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit
) {
    if (posts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No posts yet",
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(posts) { post ->
                EnhancedPostItem(
                    post = post,
                    onLikeClick = { /* Like post */ },
                    onCommentClick = { /* Open comments */ },
                    onShareClick = { /* Share post */ },
                    onProfileClick = { /* Already on profile */ },
                    onHashtagClick = onHashtagClick,
                    onMentionClick = onMentionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPostClick(post.id) }
                )
            }
        }
    }
}

/**
 * Videos grid component
 */
@Composable
fun VideosGrid(
    videos: List<Post>,
    onVideoClick: (String) -> Unit
) {
    if (videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No videos yet",
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos) { video ->
                VideoThumbnail(
                    video = video,
                    onClick = { onVideoClick(video.id) },
                    modifier = Modifier
                        .aspectRatio(0.8f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

/**
 * Likes grid component
 */
@Composable
fun LikesGrid(
    likes: List<Post>,
    onPostClick: (String) -> Unit
) {
    if (likes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No likes yet",
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(likes) { post ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                        .clickable { onPostClick(post.id) }
                ) {
                    if (post.mediaUrl != null) {
                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription = "Post thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        if (post.isVideo) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    } else {
                        Text(
                            text = post.content.take(50) + if (post.content.length > 50) "..." else "",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Media grid component
 */
@Composable
fun MediaGrid(
    media: List<Post>,
    onMediaClick: (String) -> Unit
) {
    if (media.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No media yet",
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(media) { post ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                        .clickable { onMediaClick(post.id) }
                ) {
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = "Media thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (post.isVideo) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format count for display (e.g., 1.2K, 3.4M)
 */
private fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> String.format("%.1fK", count / 1000f).replace(".0K", "K")
        else -> String.format("%.1fM", count / 1000000f).replace(".0M", "M")
    }
}
