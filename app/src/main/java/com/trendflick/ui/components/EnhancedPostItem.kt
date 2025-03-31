package com.trendflick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trendflick.data.model.Post
import com.trendflick.ui.components.RichTextDisplay
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.media3.common.util.UnstableApi
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced post item component with proper hashtag and mention functionality
 */
@OptIn(UnstableApi::class)
@Composable
fun EnhancedPostItem(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Author row with follow button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.clickable(onClick = onProfileClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Author avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333))
                    ) {
                        if (post.authorAvatar != null) {
                            AsyncImage(
                                model = post.authorAvatar,
                                contentDescription = "Author avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Author",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Author info
                    Column {
                        Text(
                            text = post.authorName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        
                        Text(
                            text = "@${post.authorHandle}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Follow button
                OutlinedButton(
                    onClick = { /* Toggle follow state */ },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B4EFF)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Follow",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Post content with rich text
            RichTextDisplay(
                text = post.content,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Media content (if any)
            if (post.mediaUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                ) {
                    if (post.isVideo) {
                        // Video thumbnail with play button
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Video player would go here in a real implementation
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            )
                        }
                    } else {
                        // Image
                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription = "Post image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Hashtags
            if (post.hashtags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(post.hashtags) { hashtag ->
                        HashtagPill(
                            hashtag = hashtag,
                            onClick = { onHashtagClick(hashtag) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Engagement stats and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Timestamp
                Text(
                    text = formatTimestamp(post.timestamp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                // Engagement stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Likes
                    EngagementStat(
                        count = post.likes,
                        icon = Icons.Default.Favorite,
                        contentDescription = "Likes",
                        onClick = onLikeClick
                    )
                    
                    // Comments
                    EngagementStat(
                        count = post.comments,
                        icon = Icons.Default.ChatBubble,
                        contentDescription = "Comments",
                        onClick = onCommentClick
                    )
                    
                    // Reposts
                    EngagementStat(
                        count = post.reposts,
                        icon = Icons.Default.Repeat,
                        contentDescription = "Reposts",
                        onClick = onShareClick
                    )
                }
            }
        }
    }
}

/**
 * Engagement stat component for displaying likes, comments, reposts
 */
@Composable
fun EngagementStat(
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = formatCount(count),
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "just now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        diff < 604800000 -> "${diff / 86400000}d"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
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
