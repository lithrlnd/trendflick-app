package com.trendflick.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trendflick.data.model.TrendingHashtag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.trendflick.data.model.Category
import com.trendflick.data.model.MainFeed
import com.trendflick.data.model.categories
import com.trendflick.data.model.mainFeeds

@Composable
fun CategoryDrawer(
    isOpen: Boolean,
    onCategorySelected: (String) -> Unit,
    onHashtagSelected: (String) -> Unit,
    trendingHashtags: List<TrendingHashtag>,
    currentHashtag: String?,
    currentCategory: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetX by animateDpAsState(
        targetValue = if (isOpen) 0.dp else (-300).dp,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Semi-transparent overlay
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDismiss() }
            )
        }

        // Drawer content
        Surface(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .offset(x = offsetX),
            color = Color(0xFF1A1A1A),
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Main Feeds Section
                item {
                    Text(
                        "For You",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    mainFeeds.forEach { feed ->
                        CategoryItem(
                            category = feed.name,
                            icon = feed.icon,
                            emoji = feed.emoji,
                            isSelected = false,
                            onClick = { 
                                onCategorySelected(feed.name)
                                onDismiss()
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Categories Section
                item {
                    Text(
                        "Discover",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(categories) { category ->
                    CategoryItem(
                        category = category.name,
                        emoji = category.emoji,
                        isSelected = currentCategory == category.name,
                        onClick = { 
                            onCategorySelected(category.name)
                            onDismiss()
                        },
                        hashtags = category.hashtags
                    )
                }

                // Trending Hashtags Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Trending",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(trendingHashtags) { hashtag ->
                    HashtagItem(
                        hashtag = hashtag,
                        isSelected = false,
                        onClick = { 
                            onHashtagSelected(hashtag.tag)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    hashtags: Set<String>? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
                
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            
            // Show hashtags if available
            if (!hashtags.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(start = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    hashtags.take(2).forEach { hashtag ->
                        Text(
                            text = "#$hashtag",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                    if (hashtags.size > 2) {
                        Text(
                            text = "+${hashtags.size - 2} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HashtagItem(
    hashtag: TrendingHashtag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            hashtag.emoji?.let { emoji ->
                Text(
                    text = emoji,
                    fontSize = 24.sp
                )
            }
            
            Column {
                Text(
                    text = "#${hashtag.tag}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "${hashtag.count} posts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
} 