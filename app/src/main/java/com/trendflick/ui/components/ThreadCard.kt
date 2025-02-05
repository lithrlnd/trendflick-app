package com.trendflick.ui.components

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trendflick.data.api.*
import com.trendflick.utils.DateUtils
import kotlinx.coroutines.delay
import com.trendflick.ui.components.CategoryDrawer

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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var showHeartAnimation by remember { mutableStateOf(false) }
    val view = LocalView.current
    val context = LocalContext.current
    var isDrawerOpen by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    val drawerWidth = 250.dp
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
        if (isLandscape) {
            // Landscape layout
            Column(modifier = Modifier.fillMaxSize()) {
                // Top row with engagement actions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Engagement actions in landscape
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        EngagementColumn(
                            isLiked = isLiked,
                            isReposted = isReposted,
                            likeCount = feedPost.post.likeCount ?: 0,
                            replyCount = feedPost.post.replyCount ?: 0,
                            repostCount = feedPost.post.repostCount ?: 0,
                            onLikeClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onLikeClick()
                            },
                            onCommentClick = onCommentClick,
                            onRepostClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onRepostClick()
                            },
                            onShareClick = onShareClick
                        )
                    }
                }

                // Main content
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Author row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onProfileClick() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(
                                    model = feedPost.post.author.avatar,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A1A1A)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                Column {
                                    Text(
                                        text = feedPost.post.author.displayName ?: feedPost.post.author.handle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${feedPost.post.author.handle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Text(
                                text = DateUtils.formatTimestamp(feedPost.post.record.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Post content
                        RichTextRenderer(
                            text = feedPost.post.record.text,
                            facets = feedPost.post.record.facets ?: emptyList(),
                            onMentionClick = { onProfileClick() },
                            onHashtagClick = { tag -> onHashtagClick?.invoke(tag) },
                            onLinkClick = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Post media if exists
                        feedPost.post.embed?.images?.firstOrNull()?.let { image ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val imageUrl = when {
                                !image.fullsize.isNullOrEmpty() -> image.fullsize
                                !image.thumb.isNullOrEmpty() -> image.thumb
                                image.image?.link != null -> "https://cdn.bsky.social/img/feed_fullsize/plain/${image.image.link}@jpeg"
                                else -> null
                            }
                            
                            if (!imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = image.alt,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(
                                            image.aspectRatio?.let { 
                                                it.width.toFloat() / it.height.toFloat() 
                                            } ?: 16f/9f
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Portrait layout with drawer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                if (offset.x < with(density) { 80.dp.toPx() }) {
                                    isDrawerOpen = true
                                }
                            },
                            onDragEnd = {
                                if (offsetX < with(density) { drawerWidth.toPx() } / 2) {
                                    isDrawerOpen = false
                                }
                            },
                            onDragCancel = {
                                if (offsetX < with(density) { drawerWidth.toPx() } / 2) {
                                    isDrawerOpen = false
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount).coerceIn(0f, with(density) { drawerWidth.toPx() })
                            }
                        )
                    }
            ) {
                // Add tap area on the left edge
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight()
                        .clickable { isDrawerOpen = true }
                )

                // Main content
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Main content area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        // Author row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onProfileClick() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(
                                    model = feedPost.post.author.avatar,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onProfileClick() },
                                    contentScale = ContentScale.Crop
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Column {
                                    Text(
                                        text = feedPost.post.author.displayName ?: feedPost.post.author.handle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${feedPost.post.author.handle} · ${DateUtils.formatTimestamp(feedPost.post.record.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Post content
                        RichTextRenderer(
                            text = feedPost.post.record.text,
                            facets = feedPost.post.record.facets ?: emptyList(),
                            onMentionClick = { onProfileClick() },
                            onHashtagClick = { tag -> onHashtagClick?.invoke(tag) },
                            onLinkClick = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Post media if exists
                        feedPost.post.embed?.images?.firstOrNull()?.let { image ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val imageUrl = when {
                                !image.fullsize.isNullOrEmpty() -> image.fullsize
                                !image.thumb.isNullOrEmpty() -> image.thumb
                                image.image?.link != null -> "https://cdn.bsky.social/img/feed_fullsize/plain/${image.image.link}@jpeg"
                                else -> null
                            }
                            
                            if (!imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = image.alt,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(
                                            image.aspectRatio?.let { 
                                                it.width.toFloat() / it.height.toFloat() 
                                            } ?: 16f/9f
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Engagement column for portrait mode
                    EngagementColumn(
                        isLiked = isLiked,
                        isReposted = isReposted,
                        likeCount = feedPost.post.likeCount ?: 0,
                        replyCount = feedPost.post.replyCount ?: 0,
                        repostCount = feedPost.post.repostCount ?: 0,
                        onLikeClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onLikeClick()
                        },
                        onCommentClick = onCommentClick,
                        onRepostClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onRepostClick()
                        },
                        onShareClick = onShareClick
                    )
                }

                // Drawer overlay
                if (isDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { isDrawerOpen = false }
                    )
                }

                // Category Drawer
                AnimatedVisibility(
                    visible = isDrawerOpen,
                    enter = slideInHorizontally(initialOffsetX = { -it }),
                    exit = slideOutHorizontally(targetOffsetX = { -it }),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    CategoryDrawer(
                        isOpen = isDrawerOpen,
                        onCategorySelected = { category ->
                            // Handle category selection
                        },
                        onHashtagSelected = { hashtag ->
                            // Handle hashtag selection
                        },
                        trendingHashtags = emptyList(), // Pass actual trending hashtags
                        currentHashtag = null,
                        currentCategory = "",
                        onDismiss = { isDrawerOpen = false },
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Heart Animation Overlay
        AnimatedVisibility(
            visible = showHeartAnimation,
            enter = fadeIn(animationSpec = tween(150)) + 
                    scaleIn(initialScale = 0.3f, animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)) +
                   scaleOut(targetScale = 1.2f, animationSpec = tween(150)),
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
private fun EngagementColumn(
    isLiked: Boolean,
    isReposted: Boolean,
    likeCount: Int,
    replyCount: Int,
    repostCount: Int,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Horizontal layout for landscape mode at the top
        Row(
            modifier = Modifier
                .height(52.dp)
                .padding(end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Like
            EngagementAction(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                count = likeCount,
                isActive = isLiked,
                onClick = onLikeClick,
                tint = if (isLiked) Color(0xFF6B4EFF) else Color.White,
                isHorizontal = true
            )

            // Comment
            EngagementAction(
                icon = Icons.Default.ChatBubbleOutline,
                count = replyCount,
                onClick = onCommentClick,
                isHorizontal = true
            )

            // Repost
            EngagementAction(
                icon = Icons.Default.Repeat,
                count = repostCount,
                isActive = isReposted,
                onClick = onRepostClick,
                tint = if (isReposted) Color(0xFF6B4EFF) else Color.White,
                isHorizontal = true
            )

            // Share
            EngagementAction(
                icon = Icons.Default.Share,
                onClick = onShareClick,
                isHorizontal = true
            )
        }
    } else {
        // Original vertical layout for portrait mode
        Column(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Like
            EngagementAction(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                count = likeCount,
                isActive = isLiked,
                onClick = onLikeClick,
                tint = if (isLiked) Color(0xFF6B4EFF) else Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Comment
            EngagementAction(
                icon = Icons.Default.ChatBubbleOutline,
                count = replyCount,
                onClick = onCommentClick
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Repost
            EngagementAction(
                icon = Icons.Default.Repeat,
                count = repostCount,
                isActive = isReposted,
                onClick = onRepostClick,
                tint = if (isReposted) Color(0xFF6B4EFF) else Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Share
            EngagementAction(
                icon = Icons.Default.Share,
                onClick = onShareClick
            )

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
private fun EngagementAction(
    icon: ImageVector,
    count: Int = 0,
    isActive: Boolean = false,
    tint: Color = Color.White,
    isHorizontal: Boolean = false,
    onClick: () -> Unit
) {
    if (isHorizontal) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (count > 0) {
                Text(
                    text = formatEngagementCount(count),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (count > 0) {
                Text(
                    text = formatEngagementCount(count),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

private fun formatEngagementCount(count: Int): String = when {
    count < 1000 -> count.toString()
    count < 1000000 -> String.format("%.1fK", count / 1000f)
    else -> String.format("%.1fM", count / 1000000f)
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
            val thumbnailUrl = thumbnail.thumbUrl
            if (!thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Link preview",
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