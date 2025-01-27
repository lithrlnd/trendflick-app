package com.trendflick.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.navigation.NavController

@Composable
fun PostInteractions(
    likes: Int,
    reposts: Int,
    replies: Int,
    isLiked: Boolean,
    isReposted: Boolean,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onQuotePost: () -> Unit,
    onReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reply button
        IconButton(onClick = onReply) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "Reply",
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = replies.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Repost menu
        RepostMenu(
            onRepost = onRepost,
            onQuotePost = onQuotePost,
            modifier = Modifier.wrapContentSize()
        )
        if (reposts > 0) {
            Text(
                text = reposts.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Like button
        IconButton(onClick = onLike) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = likes.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
} 