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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trendflick.data.model.Post
import com.trendflick.utils.DateUtils

/**
 * Component for displaying a post with rich text support for hashtags and mentions
 */
@Composable
fun PostItem(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Post header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick)
                ) {
                    if (post.avatarUrl != null) {
                        AsyncImage(
                            model = post.avatarUrl,
                            contentDescription = "User avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF6B4EFF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.username.firstOrNull()?.toString() ?: "U",
                                color = Color(0xFF6B4EFF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = post.username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable(onClick = onProfileClick)
                        )
                        
                        if (post.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF6B4EFF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        // Follow button
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { /* TODO: Follow/unfollow user */ },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6B4EFF)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                color = Color(0xFF6B4EFF)
                            )
                        ) {
                            Text(
                                text = "Follow",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@${post.handle}",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable(onClick = onProfileClick)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "•",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = DateUtils.getRelativeTimeSpan(post.timestamp),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
                
                IconButton(
                    onClick = { /* TODO: Show post options */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Post content with rich text
            val contentText = buildRichText(
                text = post.content,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick
            )
            
            Text(
                text = contentText,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            
            // Post media (if any)
            post.mediaUrl?.let { mediaUrl ->
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Post media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Embedded post (if any)
            post.embeddedPost?.let { embeddedPost ->
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF252525)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Embedded post header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User avatar
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            ) {
                                if (embeddedPost.avatarUrl != null) {
                                    AsyncImage(
                                        model = embeddedPost.avatarUrl,
                                        contentDescription = "User avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF6B4EFF).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = embeddedPost.username.firstOrNull()?.toString() ?: "U",
                                            color = Color(0xFF6B4EFF),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = embeddedPost.username,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            
                            if (embeddedPost.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF6B4EFF),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = "@${embeddedPost.handle}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = "•",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = DateUtils.getRelativeTimeSpan(embeddedPost.timestamp),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Embedded post content
                        Text(
                            text = embeddedPost.content,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Embedded post media (if any)
                        embeddedPost.mediaUrl?.let { mediaUrl ->
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = "Embedded post media",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Post actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onLikeClick)
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (post.isLiked) "Unlike" else "Like",
                        tint = if (post.isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = formatCount(post.likes),
                        color = if (post.isLiked) Color.Red else Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onCommentClick)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = formatCount(post.comments),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                // Repost button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onRepostClick)
                ) {
                    Icon(
                        imageVector = if (post.isReposted) Icons.Default.Repeat else Icons.Default.RepeatOutlined,
                        contentDescription = "Repost",
                        tint = if (post.isReposted) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = formatCount(post.reposts),
                        color = if (post.isReposted) Color(0xFF4CAF50) else Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                // Share button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onShareClick)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Build rich text with clickable hashtags and mentions
 */
@Composable
private fun buildRichText(
    text: String,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit
): AnnotatedString {
    val hashtagPattern = Regex("#(\\w+)")
    val mentionPattern = Regex("@(\\w+)")
    
    val hashtagMatches = hashtagPattern.findAll(text)
    val mentionMatches = mentionPattern.findAll(text)
    
    return buildAnnotatedString {
        append(text)
        
        // Style hashtags
        hashtagMatches.forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            val hashtag = matchResult.groupValues[1]
            
            addStyle(
                style = SpanStyle(
                    color = Color(0xFF6B4EFF),
                    fontWeight = FontWeight.Bold
                ),
                start = start,
                end = end
            )
            
            // Make hashtag clickable
            addStringAnnotation(
                tag = "hashtag",
                annotation = hashtag,
                start = start,
                end = end
            )
        }
        
        // Style mentions
        mentionMatches.forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            val mention = matchResult.groupValues[1]
            
            addStyle(
                style = SpanStyle(
                    color = Color(0xFF4E8AFF),
                    fontWeight = FontWeight.Bold
                ),
                start = start,
                end = end
            )
            
            // Make mention clickable
            addStringAnnotation(
                tag = "mention",
                annotation = mention,
                start = start,
                end = end
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fK", count / 1_000f)
        else -> count.toString()
    }
}
