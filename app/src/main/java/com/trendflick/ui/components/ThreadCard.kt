package com.trendflick.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trendflick.data.api.*
import com.trendflick.utils.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import kotlinx.coroutines.delay

@Composable
fun ThreadCard(
    feedPost: FeedPost,
    isLiked: Boolean,
    isReposted: Boolean,
    onLikeClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: () -> Unit,
    onThreadClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCreatePost: () -> Unit,
    onHashtagClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showHeartAnimation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { 
                        showHeartAnimation = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLikeClick()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Author row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onProfileClick() })
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Safely load avatar with error handling
                    AsyncImage(
                        model = feedPost.post.author.avatar,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                        onError = {
                            // Handle image load error
                            it.result.throwable.printStackTrace()
                        }
                    )
                    Column {
                        Text(
                            text = feedPost.post.author.displayName ?: feedPost.post.author.handle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${feedPost.post.author.handle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = DateUtils.formatTimestamp(feedPost.post.record.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content and engagement layout
            Row(modifier = Modifier.fillMaxWidth()) {
                // Post content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    RichTextRenderer(
                        text = feedPost.post.record.text,
                        facets = feedPost.post.record.facets ?: emptyList(),
                        onMentionClick = { did ->
                            onProfileClick()
                        },
                        onHashtagClick = { tag ->
                            // Handle hashtag click in parent
                            onHashtagClick?.invoke(tag)
                        },
                        onLinkClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Only load first image if available
                    feedPost.post.record.embed?.images?.firstOrNull()?.let { image ->
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = image.image,
                            contentDescription = image.alt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio = (image.aspectRatio ?: 1.77).toFloat())
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            onError = {
                                // Handle image load error
                                it.result.throwable.printStackTrace()
                            }
                        )
                    }
                }

                // Engagement metrics column
                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Like button and count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                println("DEBUG: TrendFlick ❤️ Like button clicked - isLiked before: $isLiked")
                                showHeartAnimation = true 
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onLikeClick()
                                println("DEBUG: TrendFlick ❤️ Like animation triggered")
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Color(0xFF6B4EFF) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            println("DEBUG: TrendFlick ❤️ Like icon rendered - isLiked: $isLiked, uri: ${feedPost.post.uri}")
                        }
                        Text(
                            text = "${feedPost.post.likeCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Comment button and count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onCommentClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "${feedPost.post.replyCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Repost button and count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onRepostClick()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repost",
                                tint = if (isReposted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "${feedPost.post.repostCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Heart Animation
        AnimatedVisibility(
            visible = showHeartAnimation,
            enter = fadeIn(animationSpec = tween(150)) + 
                    scaleIn(
                        initialScale = 0.3f,
                        animationSpec = tween(150)
                    ),
            exit = fadeOut(animationSpec = tween(150)) +
                   scaleOut(
                       targetScale = 1.2f,
                       animationSpec = tween(150)
                   ),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFF6B4EFF),
                modifier = Modifier.size(100.dp)
            )

            LaunchedEffect(showHeartAnimation) {
                if (showHeartAnimation) {
                    delay(800)
                    showHeartAnimation = false
                }
            }
        }
    }
}

@Composable
fun EmbeddedLink(
    title: String,
    description: String?,
    thumbnail: ExternalEmbed,
    url: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Thumbnail
            val thumbnailUrl = when {
                thumbnail.thumbUrl != null -> thumbnail.thumbUrl
                thumbnail.thumbBlob?.link != null -> "https://cdn.bsky.app/img/feed_thumbnail/plain/${thumbnail.thumbBlob.link}@jpeg"
                else -> null
            }
            
            thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Link thumbnail",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = Uri.parse(url).host ?: url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    onClick: (Int) -> Unit
) {
    Text(
        text = text,
        modifier = modifier.pointerInput(onClick) {
            detectTapGestures { pos ->
                // Get the character offset for the click position
                val offset = text.length * (pos.x / size.width).coerceIn(0f, 1f).toInt()
                onClick(offset)
            }
        },
        style = style
    )
}

@Composable
fun CommentItem(
    comment: ThreadPost,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    level: Int = 0,
    authorDid: String? = null,
    showAuthorOnly: Boolean = false
) {
    // Skip non-author comments when in author-only mode
    if (showAuthorOnly && authorDid != null && comment.post.author.did != authorDid) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = (level * 16).dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        // Author row with profile picture and name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = comment.post.author.avatar,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onProfileClick() },
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = comment.post.author.displayName ?: comment.post.author.handle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${comment.post.author.handle} · ${DateUtils.formatTimestamp(comment.post.record.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Comment content
        Text(
            text = comment.post.record.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 40.dp) // Align with the text above
        )

        // Engagement metrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Likes",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${comment.post.likeCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Replies",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${comment.post.replyCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Divider between comments
        if (level == 0) {
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        }

        // Recursively render replies with proper composable context
        comment.replies?.let { replies ->
            replies.forEach { reply ->
                key(reply.post.uri) {
                    CommentItem(
                        comment = reply,
                        onProfileClick = onProfileClick,
                        level = level + 1,
                        authorDid = authorDid,
                        showAuthorOnly = showAuthorOnly
                    )
                }
            }
        }
    }
}

@Composable
fun CommentsSection(
    comments: List<ThreadPost>,
    onProfileClick: () -> Unit,
    authorDid: String,
    modifier: Modifier = Modifier
) {
    var showAuthorOnly by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Filter toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (showAuthorOnly) "Author Only" else "All Comments",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showAuthorOnly,
                    onCheckedChange = { showAuthorOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        // Comments list
        comments.forEach { comment ->
            key(comment.post.uri) {
                CommentItem(
                    comment = comment,
                    onProfileClick = onProfileClick,
                    authorDid = authorDid,
                    showAuthorOnly = showAuthorOnly
                )
            }
        }
    }
} 