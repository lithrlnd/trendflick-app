package com.trendflick.ui.screens.hashtag

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trendflick.ui.components.ThreadCard
import com.trendflick.data.api.FeedPost
import com.trendflick.ui.components.HashtagBadge
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagScreen(
    hashtag: String,
    onBackClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: HashtagViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(HashtagTab.Latest) }
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val relatedHashtags by viewModel.relatedHashtags.collectAsState()
    val postCount by viewModel.postCount.collectAsState()
    val engagementRate by viewModel.engagementRate.collectAsState()

    LaunchedEffect(hashtag) {
        viewModel.loadHashtagData(hashtag)
    }

    Scaffold(
        topBar = {
            HashtagTopBar(
                hashtag = hashtag,
                onBackClick = onBackClick,
                isFollowing = isFollowing,
                onFollowClick = { viewModel.toggleFollow(hashtag) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hashtag Header
            HashtagHeader(
                hashtag = hashtag,
                postCount = postCount,
                engagementRate = engagementRate
            )

            // Related Hashtags
            if (relatedHashtags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(relatedHashtags) { relatedTag ->
                        HashtagBadge(
                            hashtag = relatedTag,
                            onClick = { /* Handle related hashtag click */ }
                        )
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                HashtagTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                posts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No posts found for #$hashtag")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = posts,
                            key = { it.post.uri }
                        ) { post ->
                            ThreadCard(
                                feedPost = post,
                                isLiked = false, // Implement like state
                                isReposted = false, // Implement repost state
                                onLikeClick = { /* Handle like */ },
                                onRepostClick = { /* Handle repost */ },
                                onShareClick = { /* Handle share */ },
                                onProfileClick = { onNavigateToProfile(post.post.author.did) },
                                onThreadClick = { /* Handle thread click */ },
                                onCommentClick = { /* Handle comment */ },
                                onCreatePost = { /* Handle create post */ },
                                onImageClick = { /* Handle image click */ },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HashtagTopBar(
    hashtag: String,
    onBackClick: () -> Unit,
    isFollowing: Boolean,
    onFollowClick: () -> Unit
) {
    TopAppBar(
        title = { Text("#$hashtag") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },
        actions = {
            IconButton(onClick = onFollowClick) {
                Icon(
                    imageVector = if (isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFollowing) "Unfollow" else "Follow"
                )
            }
        }
    )
}

@Composable
private fun HashtagHeader(
    hashtag: String,
    postCount: Int,
    engagementRate: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6B4EFF).copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = "#$hashtag",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF6B4EFF)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = formatCount(postCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Posts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column {
                Text(
                    text = "${String.format("%.1f", engagementRate)}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Engagement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private enum class HashtagTab(
    val title: String,
    val icon: ImageVector
) {
    Latest("Latest", Icons.Default.Schedule),
    Trending("Trending", Icons.Default.TrendingUp),
    Media("Media", Icons.Default.Image)
}

private fun formatCount(count: Int): String = when {
    count < 1000 -> count.toString()
    count < 1000000 -> String.format("%.1fK", count / 1000f)
    else -> String.format("%.1fM", count / 1000000f)
} 