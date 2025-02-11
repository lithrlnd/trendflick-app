package com.trendflick.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.trendflick.data.model.Post
import com.trendflick.ui.navigation.CustomCategory
import com.trendflick.ui.navigation.EngagementType
import com.trendflick.ui.navigation.PostType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryFeedScreen(
    category: CustomCategory,
    posts: List<Post>,
    onEngagement: (Post, EngagementType) -> Unit,
    onHashtagClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Show trending section if enabled for category
        if (category.showTrending) {
            TrendingSection(
                category = category,
                onHashtagClick = onHashtagClick
            )
        }

        LazyColumn {
            items(posts) { post ->
                // Only show posts that match the allowed types for this category
                if (category.allowedPostTypes.contains(post.type)) {
                    when (post.type) {
                        PostType.TEXT -> TextPost(
                            post = post,
                            allowedEngagements = category.allowedEngagements,
                            onEngagement = onEngagement
                        )
                        PostType.IMAGE -> ImagePost(
                            post = post,
                            allowedEngagements = category.allowedEngagements,
                            onEngagement = onEngagement
                        )
                        PostType.VIDEO -> VideoPost(
                            post = post,
                            allowedEngagements = category.allowedEngagements,
                            onEngagement = onEngagement
                        )
                        PostType.THREAD -> ThreadPost(
                            post = post,
                            allowedEngagements = category.allowedEngagements,
                            onEngagement = onEngagement
                        )
                        else -> TextPost(
                            post = post,
                            allowedEngagements = category.allowedEngagements,
                            onEngagement = onEngagement
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingSection(
    category: CustomCategory,
    onHashtagClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Trending Now",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (category.showHashtags) {
            LazyRow {
                items(10) { index ->
                    HashtagChip(
                        hashtag = "#trending$index",
                        onClick = { onHashtagClick("#trending$index") }
                    )
                }
            }
        }
    }
}

@Composable
private fun HashtagChip(
    hashtag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = hashtag,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun EngagementBar(
    post: Post,
    allowedEngagements: Set<EngagementType>,
    onEngagement: (Post, EngagementType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        allowedEngagements.forEach { engagementType ->
            IconButton(onClick = { onEngagement(post, engagementType) }) {
                when (engagementType) {
                    EngagementType.LIKE -> Icon(Icons.Default.Favorite, "Like")
                    EngagementType.COMMENT -> Icon(Icons.Default.Comment, "Comment")
                    EngagementType.REPOST -> Icon(Icons.Default.Repeat, "Repost")
                    EngagementType.SHARE -> Icon(Icons.Default.Share, "Share")
                    EngagementType.SAVE -> Icon(Icons.Default.BookmarkBorder, "Save")
                }
            }
        }
    }
}

@Composable
private fun PostContent(
    post: Post,
    modifier: Modifier = Modifier
) {
    // Implement post content display (video/image)
    // This will be similar to your existing TrendFlick post display
}

@Composable
private fun EngagementActions(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Like button
        IconButton(onClick = onLike) {
            Icon(
                imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (post.isLiked) "Unlike" else "Like",
                tint = if (post.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Comment button
        IconButton(onClick = onComment) {
            Icon(
                imageVector = Icons.Default.Comment,
                contentDescription = "Comment"
            )
        }
        
        // Share button
        IconButton(onClick = onShare) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share"
            )
        }
    }
}

@Composable
private fun PostInfo(
    post: Post,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Username
        Text(
            text = post.author.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable { onUserClick(post.author.did) }
        )
        
        // Post description
        if (post.description.isNotEmpty()) {
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
} 