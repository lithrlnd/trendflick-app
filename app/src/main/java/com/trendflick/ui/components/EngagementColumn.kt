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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trendflick.data.model.Video
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * Engagement column component for video interactions
 * Displays like, comment, share, and profile buttons in a vertical column
 */
@Composable
fun EngagementColumn(
    video: Video,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: () -> Unit,
    isLiked: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Bottom)
    ) {
        // Profile picture
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(onClick = onProfileClick)
        ) {
            if (video.authorAvatar != null) {
                AsyncImage(
                    model = video.authorAvatar,
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
            
            // Follow button indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6B4EFF))
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Follow",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        
        // Like button with animation
        LikeButton(
            isLiked = isLiked,
            count = video.likes,
            onClick = onLikeClick
        )
        
        // Comment button
        EngagementButton(
            icon = Icons.Default.ChatBubble,
            count = video.comments?.size ?: 0,
            onClick = onCommentClick,
            contentDescription = "Comments"
        )
        
        // Share button
        EngagementButton(
            icon = Icons.Default.Share,
            count = video.shares,
            onClick = onShareClick,
            contentDescription = "Share"
        )
        
        // Bookmark button
        EngagementButton(
            icon = Icons.Default.Bookmark,
            count = null,
            onClick = { /* Bookmark functionality */ },
            contentDescription = "Bookmark"
        )
    }
}

/**
 * Animated like button with heart icon and count
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    
    LaunchedEffect(isLiked) {
        if (isLiked) {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(100)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(100)
            )
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) Color.Red else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Text(
            text = formatCount(count),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Generic engagement button with icon and count
 */
@Composable
fun EngagementButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int?,
    onClick: () -> Unit,
    contentDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        if (count != null) {
            Text(
                text = formatCount(count),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
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
