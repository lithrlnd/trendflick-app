package com.trendflick.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.trendflick.data.model.Post
import com.trendflick.ui.navigation.EngagementType
import com.trendflick.ui.components.VideoPlayer

@Composable
fun TextPost(
    post: Post,
    allowedEngagements: Set<EngagementType>,
    onEngagement: (Post, EngagementType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Author info
            PostHeader(post = post)
            
            // Post content
            Text(
                text = post.content,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            
            // Engagement bar
            EngagementBar(
                post = post,
                allowedEngagements = allowedEngagements,
                onEngagement = onEngagement
            )
        }
    }
}

@Composable
fun ImagePost(
    post: Post,
    allowedEngagements: Set<EngagementType>,
    onEngagement: (Post, EngagementType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            PostHeader(post = post)
            
            // Image content
            AsyncImage(
                model = post.mediaUrl,
                contentDescription = "Post image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f),
                contentScale = ContentScale.Crop
            )
            
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            EngagementBar(
                post = post,
                allowedEngagements = allowedEngagements,
                onEngagement = onEngagement
            )
        }
    }
}

@Composable
fun VideoPost(
    post: Post,
    allowedEngagements: Set<EngagementType>,
    onEngagement: (Post, EngagementType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            PostHeader(post = post)
            
            // Video player
            VideoPlayer(
                videoUrl = post.mediaUrl,
                thumbnailUrl = post.thumbnailUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
            )
            
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            EngagementBar(
                post = post,
                allowedEngagements = allowedEngagements,
                onEngagement = onEngagement
            )
        }
    }
}

@Composable
fun ThreadPost(
    post: Post,
    allowedEngagements: Set<EngagementType>,
    onEngagement: (Post, EngagementType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            PostHeader(post = post)
            
            // Thread content
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                // Thread indicators
                if (post.replyCount > 0) {
                    Text(
                        text = "${post.replyCount} replies",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            EngagementBar(
                post = post,
                allowedEngagements = allowedEngagements,
                onEngagement = onEngagement
            )
        }
    }
}

@Composable
private fun PostHeader(post: Post) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Author avatar
        AsyncImage(
            model = post.author.avatarUrl,
            contentDescription = "Author avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        ) {
            Text(
                text = post.author.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@${post.author.handle}",
                style = MaterialTheme.typography.labelMedium
            )
        }
        
        // Post timestamp
        Text(
            text = post.timestamp,
            style = MaterialTheme.typography.labelSmall
        )
    }
} 