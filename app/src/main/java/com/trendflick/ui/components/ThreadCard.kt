@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.trendflick.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trendflick.data.api.*
import com.trendflick.utils.DateUtils
import kotlinx.coroutines.delay
import com.trendflick.ui.components.CategoryDrawer
import androidx.media3.common.util.UnstableApi
import androidx.compose.foundation.border
import com.trendflick.ui.utils.CommonUtils

@Composable
private fun RepostHeader(
    repostAuthor: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = "Reposted by",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$repostAuthor reposted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun PostEmbed(
    embed: Embed,
    onImageClick: (ImageEmbed) -> Unit,
    onLinkClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Log.d("ThreadCard", """
        📎 Processing embed:
        Type: ${embed.type}
        Has images: ${embed.images != null}
        Has video: ${embed.video != null}
        Has external: ${embed.external != null}
        External URI: ${embed.external?.uri}
        Thumbnail: ${embed.external?.thumb?.link}
    """.trimIndent())

    when {
        // Enhanced video content handling
        embed.video != null || (embed.external?.uri?.let { uri ->
            uri.endsWith(".mp4", ignoreCase = true) ||
            uri.endsWith(".mov", ignoreCase = true) ||
            uri.endsWith(".webm", ignoreCase = true) ||
            uri.contains("video", ignoreCase = true) ||
            uri.contains("youtube.com") ||
            uri.contains("youtu.be") ||
            uri.contains("vimeo.com")
        } == true) -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
            ) {
                val videoUrl = when {
                    embed.video?.ref?.link != null -> {
                        "https://cdn.bsky.app/video/plain/${embed.video.ref.link}"
                    }
                    embed.external?.uri != null -> {
                        when {
                            embed.external.uri.contains("youtube.com") || 
                            embed.external.uri.contains("youtu.be") -> {
                                val videoId = extractYouTubeVideoId(embed.external.uri)
                                "https://www.youtube.com/embed/$videoId"
                            }
                            embed.external.uri.contains("vimeo.com") -> {
                                val videoId = extractVimeoVideoId(embed.external.uri)
                                "https://player.vimeo.com/video/$videoId"
                            }
                            else -> embed.external.uri
                        }
                    }
                    else -> ""
                }

                if (videoUrl.isNotEmpty()) {
                    VideoPlayer(
                        videoUrl = videoUrl,
                        isVisible = true,
                        onProgressChanged = { progress = it },
                        isPaused = isPaused,
                        modifier = Modifier.fillMaxSize(),
                        onError = { loadError = it }
                    )

                    // Video controls overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { isPaused = !isPaused }
                                )
                            }
                    )

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Error state
                    loadError?.let { error ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = error,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        embed.images != null -> {
            when (embed.images.size) {
                1 -> SingleImageLayout(
                    image = embed.images[0],
                    onImageClick = onImageClick,
                    modifier = modifier
                )
                2 -> TwoImagesLayout(
                    images = embed.images,
                    onImageClick = onImageClick,
                    modifier = modifier
                )
                3 -> ThreeImagesLayout(
                    images = embed.images,
                    onImageClick = onImageClick,
                    modifier = modifier
                )
                4 -> FourImagesLayout(
                    images = embed.images,
                    onImageClick = onImageClick,
                    modifier = modifier
                )
            }
        }
        embed.external != null -> {
            EmbeddedLink(
                title = embed.external.title ?: "Untitled",
                description = embed.external.description,
                thumbnail = embed.external,
                url = embed.external.uri,
                onClick = { 
                    onLinkClick?.invoke(embed.external.uri) ?: run {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(embed.external.uri))
                        context.startActivity(intent)
                    }
                },
                modifier = modifier
            )
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
            tint = if (isReposted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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

@Composable
private fun EngagementAction(
    icon: ImageVector,
    count: Int = 0,
    isActive: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    onClick: () -> Unit
) {
    val view = LocalView.current
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                scale = 0.8f
                onClick()
                scale = 1f
            },
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else tint,
                modifier = Modifier.size(24.dp)
            )
        }
        if (count > 0) {
            Text(
                text = CommonUtils.formatCount(count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

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
    onImageClick: (ImageEmbed) -> Unit,
    onHashtagClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var showHeartAnimation by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    val view = LocalView.current
    val context = LocalContext.current

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
                // Show repost header if this is a repost
                if (feedPost.reason?.type == "app.bsky.feed.repost") {
                    RepostHeader(
                        repostAuthor = feedPost.reason?.by?.let { author ->
                            when {
                                !author.displayName.isNullOrBlank() -> author.displayName
                                !author.handle.isNullOrBlank() -> author.handle
                                else -> "Someone"
                            }
                        } ?: "Someone"
                    )
                }

                // Author row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = feedPost.post.author?.avatar,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    
                    Column {
                        Text(
                            text = when {
                                !feedPost.post.author?.displayName.isNullOrBlank() -> feedPost.post.author?.displayName ?: ""
                                !feedPost.post.author?.handle.isNullOrBlank() -> feedPost.post.author?.handle ?: ""
                                else -> "Unknown User"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${feedPost.post.author?.handle ?: "unknown"} · ${DateUtils.formatTimestamp(feedPost.post.record.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Rich text content
                    RichTextRenderer(
                        text = feedPost.post.record.text,
                        facets = feedPost.post.record.facets ?: emptyList(),
                        onMentionClick = { did: String -> onProfileClick() },
                        onHashtagClick = { tag: String -> onHashtagClick?.invoke(tag) },
                        onLinkClick = { url: String -> 
                            onLinkClick?.invoke(url) ?: run {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        }
                    )

                    // Handle embeds
                    feedPost.post.embed?.let { embed ->
                        Spacer(modifier = Modifier.height(8.dp))
                        PostEmbed(
                            embed = embed,
                            onImageClick = { image ->
                                selectedImageIndex = feedPost.post.embed.images?.indexOf(image)
                                onImageClick(image)
                            },
                            onLinkClick = onLinkClick
                        )
                    }
                }
            }

            // Engagement column
            if (!isLandscape) {
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

        // Heart animation overlay
        AnimatedVisibility(
            visible = showHeartAnimation,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(100.dp)
            )
        }

        // Image viewer overlay
        feedPost.post.embed?.images?.let { images ->
            selectedImageIndex?.let { index ->
                ImageViewer(
                    images = images,
                    initialImageIndex = index,
                    onDismiss = { selectedImageIndex = null }
                )
            }
        }
    }
}

private fun extractYouTubeVideoId(url: String): String {
    val pattern = """(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})"""
    val regex = Regex(pattern)
    return regex.find(url)?.groupValues?.get(1) ?: ""
}

private fun extractVimeoVideoId(url: String): String {
    val pattern = """vimeo\.com\/(?:.*#|.*/videos/)?([0-9]+)"""
    val regex = Regex(pattern)
    return regex.find(url)?.groupValues?.get(1) ?: ""
}

@Composable
private fun SingleImageLayout(
    image: ImageEmbed,
    onImageClick: (ImageEmbed) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onImageClick(image) }
    ) {
        AsyncImage(
            model = image.fullsize ?: image.image?.link?.let { link ->
                "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
            },
            contentDescription = image.alt,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun TwoImagesLayout(
    images: List<ImageEmbed>,
    onImageClick: (ImageEmbed) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        images.take(2).forEach { image ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick(image) }
            ) {
                AsyncImage(
                    model = image.fullsize ?: image.image?.link?.let { link ->
                        "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                    },
                    contentDescription = image.alt,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun ThreeImagesLayout(
    images: List<ImageEmbed>,
    onImageClick: (ImageEmbed) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // First image takes full width
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f/9f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onImageClick(images[0]) }
        ) {
            AsyncImage(
                model = images[0].fullsize ?: images[0].image?.link?.let { link ->
                    "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                },
                contentDescription = images[0].alt,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Two images in a row below
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            images.drop(1).take(2).forEach { image ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(16f/9f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(image) }
                ) {
                    AsyncImage(
                        model = image.fullsize ?: image.image?.link?.let { link ->
                            "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                        },
                        contentDescription = image.alt,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun FourImagesLayout(
    images: List<ImageEmbed>,
    onImageClick: (ImageEmbed) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                images.drop(row * 2).take(2).forEach { image ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(image) }
                    ) {
                        AsyncImage(
                            model = image.fullsize ?: image.image?.link?.let { link ->
                                "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                            },
                            contentDescription = image.alt,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddedLink(
    title: String,
    description: String?,
    thumbnail: ExternalEmbed,
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Log thumbnail details for debugging
    Log.d("ThreadCard", """
        🔗 Processing link embed:
        URL: $url
        Title: $title
        Has thumb: ${thumbnail.thumb != null}
        Thumb link: ${thumbnail.thumb?.link}
        Platform: ${thumbnail.getSocialMediaInfo()?.platform}
    """.trimIndent())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Enhanced thumbnail handling
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Enhanced thumbnail URL generation with multiple fallbacks
                val thumbnailUrl = thumbnail.thumb?.link?.let { link ->
                    if (link.startsWith("http")) {
                        link
                    } else {
                        "https://cdn.bsky.app/img/feed_thumbnail/plain/$link@jpeg"
                    }
                } ?: run {
                    // Fallback mechanisms when thumb link is null
                    val uri = Uri.parse(url)
                    val host = uri.host
                    
                    when {
                        // YouTube thumbnails
                        url.contains("youtube.com") || url.contains("youtu.be") -> {
                            val videoId = extractYouTubeVideoId(url)
                            if (videoId.isNotBlank()) {
                                "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
                            } else {
                                ""
                            }
                        }
                        // Twitter/X thumbnails via microlink
                        url.contains("twitter.com") || url.contains("x.com") -> {
                            val encodedUrl = Uri.encode(url)
                            "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
                        }
                        // Common domains with known thumbnail patterns
                        url.contains("instagram.com") || 
                        url.contains("tiktok.com") ||
                        url.contains("facebook.com") -> {
                            val encodedUrl = Uri.encode(url)
                            "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
                        }
                        // Fallback to domain favicon for other sites
                        !host.isNullOrBlank() -> {
                            "https://www.google.com/s2/favicons?domain=$host&sz=128"
                        }
                        else -> ""
                    }
                }

                Log.d("ThreadCard", "🖼️ Using thumbnail URL: $thumbnailUrl")

                if (thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "Link preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                }
            }

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