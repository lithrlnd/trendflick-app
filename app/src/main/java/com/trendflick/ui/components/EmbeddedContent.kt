package com.trendflick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.trendflick.data.model.LinkPreview
import com.trendflick.data.model.Post
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign

/**
 * Component for displaying embedded content in posts
 * Handles different types of embeds: link previews, quoted posts, etc.
 */
@Composable
fun EmbeddedContent(
    post: Post,
    onPostClick: () -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Handle embedded post (repost/quote)
        post.embedPost?.let { embeddedPost ->
            EmbeddedPost(
                post = embeddedPost,
                onPostClick = onPostClick,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick
            )
        }
        
        // Handle link preview
        post.linkPreview?.let { linkPreview ->
            LinkPreview(
                linkPreview = linkPreview,
                onClick = { /* Open link */ }
            )
        }
    }
}

/**
 * Component for displaying embedded posts (reposts/quotes)
 */
@Composable
fun EmbeddedPost(
    post: Post,
    onPostClick: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPostClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF252525),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Author info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author avatar
                AsyncImage(
                    model = post.authorAvatar,
                    contentDescription = "Author avatar",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    fallback = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.Gray, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Author name and handle
                Column {
                    Text(
                        text = post.authorName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "@${post.authorHandle}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Post content
            EnhancedRichTextDisplay(
                text = post.content,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontSize = 14.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Media preview if available
            post.mediaUrl?.let { mediaUrl ->
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                ) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (post.isVideo) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Component for displaying link previews in posts
 */
@Composable
fun LinkPreview(
    linkPreview: LinkPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF252525),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // Link info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = linkPreview.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                linkPreview.description?.let { description ->
                    Text(
                        text = description,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = linkPreview.url,
                    color = Color(0xFF6B4EFF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Link image if available
            linkPreview.imageUrl?.let { imageUrl ->
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        .background(Color(0xFF333333))
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Link preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * Component for displaying video thumbnails in grids
 */
@Composable
fun VideoThumbnail(
    video: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(Color(0xFF333333))
    ) {
        // Video thumbnail
        AsyncImage(
            model = video.mediaUrl,
            contentDescription = "Video thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            fallback = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        )
        
        // Play icon overlay
        Box(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play video",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Duration and view count
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(4.dp)
        ) {
            Text(
                text = "1:23", // Mock duration
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Start
            )
            
            Text(
                text = "${formatCount(video.likes)} views",
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Start
            )
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
