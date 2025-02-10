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

@UnstableApi
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
            when {
                // Case 1: Post has media (images/video) or external link
                feedPost.post.embed != null -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left side - Media content or link preview
                        Box(
                            modifier = Modifier
                                .weight(0.6f)  // Give more space to media
                                .fillMaxHeight()
                                .padding(8.dp)
                        ) {
                            when {
                                // Handle video content
                                feedPost.post.embed?.video != null || (feedPost.post.embed?.external?.uri?.let { uri ->
                                    uri.endsWith(".mp4", ignoreCase = true) ||
                                    uri.endsWith(".mov", ignoreCase = true) ||
                                    uri.endsWith(".webm", ignoreCase = true) ||
                                    uri.contains("video", ignoreCase = true)
                                } == true) -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .aspectRatio(16f/9f)
                                            .align(Alignment.Center)
                                    ) {
                                        var isPaused by remember { mutableStateOf(false) }
                                        var progress by remember { mutableStateOf(0f) }
                                        var loadError by remember { mutableStateOf<String?>(null) }

                                        val videoUrl = when {
                                            feedPost.post.embed?.video?.ref?.link != null -> {
                                                "https://cdn.bsky.app/video/plain/${feedPost.post.embed?.video?.ref?.link}"
                                            }
                                            feedPost.post.embed?.external?.uri != null -> {
                                                when {
                                                    feedPost.post.embed?.external?.uri?.contains("bsky.app/profile") == true -> {
                                                        val encodedUrl = Uri.encode(feedPost.post.embed?.external?.uri)
                                                        "https://embed.bsky.app/oembed?url=$encodedUrl&format=json"
                                                    }
                                                    else -> feedPost.post.embed?.external?.uri
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
                                        }
                                    }
                                }
                                // Handle images
                                feedPost.post.embed?.images != null -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .align(Alignment.Center)
                                    ) {
                                        when (feedPost.post.embed?.images?.size) {
                                            1 -> SingleImageLayout(
                                                image = feedPost.post.embed?.images!![0],
                                                onImageClick = { selectedImageIndex = 0 },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            2 -> TwoImagesLayout(
                                                images = feedPost.post.embed?.images!!,
                                                onImageClick = { image -> selectedImageIndex = feedPost.post.embed?.images?.indexOf(image) },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            3 -> ThreeImagesLayout(
                                                images = feedPost.post.embed?.images!!,
                                                onImageClick = { image -> selectedImageIndex = feedPost.post.embed?.images?.indexOf(image) },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            4 -> FourImagesLayout(
                                                images = feedPost.post.embed?.images!!,
                                                onImageClick = { image -> selectedImageIndex = feedPost.post.embed?.images?.indexOf(image) },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                                // Handle external link preview
                                feedPost.post.embed?.external != null -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .align(Alignment.Center)
                                    ) {
                                        EmbeddedLink(
                                            title = feedPost.post.embed?.external?.title ?: "Untitled",
                                            description = feedPost.post.embed?.external?.description,
                                            thumbnail = feedPost.post.embed?.external!!,
                                            url = feedPost.post.embed?.external?.uri ?: "",
                                            onClick = { 
                                                onLinkClick?.invoke(feedPost.post.embed?.external?.uri ?: "") ?: run {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(feedPost.post.embed?.external?.uri))
                                                    context.startActivity(intent)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Right side - Post content and engagement
                        Column(
                            modifier = Modifier
                                .weight(0.4f)  // Less space for content
                                .fillMaxHeight()
                                .padding(end = 16.dp)
                        ) {
                            // Top engagement row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                            // Author info and post content
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 8.dp)
                            ) {
                                // Author row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
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

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        text = DateUtils.formatTimestamp(feedPost.post.record.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Post content
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

                                // Handle external embeds if present
                                feedPost.post.embed?.external?.let { external ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    EmbeddedLink(
                                        title = external.title ?: "Untitled",
                                        description = external.description,
                                        thumbnail = external,
                                        url = external.uri,
                                        onClick = { 
                                            onLinkClick?.invoke(external.uri) ?: run {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(external.uri))
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                // Case 2: Text-only post
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Top engagement row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                        // Author info and post content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 8.dp)
                        ) {
                            // Author row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Post content
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

                            // Handle external embeds if present
                            feedPost.post.embed?.external?.let { external ->
                                Spacer(modifier = Modifier.height(8.dp))
                                EmbeddedLink(
                                    title = external.title ?: "Untitled",
                                    description = external.description,
                                    thumbnail = external,
                                    url = external.uri,
                                    onClick = { 
                                        onLinkClick?.invoke(external.uri) ?: run {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(external.uri))
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(0.8f)
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
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

                            // Handle embeds immediately after text
                            feedPost.post.embed?.let { embed ->
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Handle video content first
                                when {
                                    embed.video != null || (embed.external?.uri?.let { uri ->
                                        uri.endsWith(".mp4", ignoreCase = true) ||
                                        uri.endsWith(".mov", ignoreCase = true) ||
                                        uri.endsWith(".webm", ignoreCase = true) ||
                                        uri.contains("video", ignoreCase = true)
                                    } == true) -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f/9f)
                                        ) {
                                            var isPaused by remember { mutableStateOf(false) }
                                            var progress by remember { mutableStateOf(0f) }
                                            var loadError by remember { mutableStateOf<String?>(null) }

                                            val videoUrl = when {
                                                embed.video?.ref?.link != null -> {
                                                    "https://cdn.bsky.app/video/plain/${embed.video.ref.link}"
                                                }
                                                embed.external?.uri != null -> {
                                                    when {
                                                        embed.external.uri.contains("bsky.app/profile") -> {
                                                            val encodedUrl = Uri.encode(embed.external.uri)
                                                            "https://embed.bsky.app/oembed?url=$encodedUrl&format=json"
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

                                                // Tap to play/pause
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
                                                            .background(Color(0xFF6B4EFF))
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
                                    // Handle external website embeds
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
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        )
                                    }
                                    // Handle images
                                    embed.images != null -> {
                                        when (embed.images.size) {
                                            1 -> SingleImageLayout(
                                                image = embed.images[0],
                                                onImageClick = { selectedImageIndex = 0 }
                                            )
                                            2 -> TwoImagesLayout(
                                                images = embed.images,
                                                onImageClick = { image -> selectedImageIndex = embed.images.indexOf(image) }
                                            )
                                            3 -> ThreeImagesLayout(
                                                images = embed.images,
                                                onImageClick = { image -> selectedImageIndex = embed.images.indexOf(image) }
                                            )
                                            4 -> FourImagesLayout(
                                                images = embed.images,
                                                onImageClick = { image -> selectedImageIndex = embed.images.indexOf(image) }
                                            )
                                        }
                                    }
                                }
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
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
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
                        trendingHashtags = emptyList(),
                        currentHashtag = null,
                        currentCategory = "",
                        onDismiss = { isDrawerOpen = false },
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    )
                }

                // Only show FAB in portrait mode
                FloatingActionButton(
                    onClick = onCreatePost,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create new post",
                        modifier = Modifier.size(24.dp)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(100.dp)
            )
        }

        // Add ImageViewer overlay
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
                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                tint = if (isReposted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
}

@Composable
private fun EngagementAction(
    icon: ImageVector,
    count: Int = 0,
    isActive: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    isHorizontal: Boolean = false,
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

    if (isHorizontal) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
    } else {
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
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
}

@Composable
fun EmbeddedLink(
    title: String,
    description: String?,
    thumbnail: ExternalEmbed,
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
            val thumbnailUrl = thumbnail.thumb?.link?.let { link ->
                "https://cdn.bsky.app/img/feed_thumbnail/plain/$link@jpeg"
            }
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

@Composable
private fun SingleImageLayout(
    image: ImageEmbed,
    onImageClick: (ImageEmbed) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onImageClick(image) }
    ) {
        AsyncImage(
            model = image.fullsize ?: image.image?.link?.let { link ->
                "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
            } ?: "",
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
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        images.forEach { image ->
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
                    } ?: "",
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
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
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
                } ?: "",
                contentDescription = images[0].alt,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(16f/9f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(images[i]) }
                ) {
                    AsyncImage(
                        model = images[i].fullsize ?: images[i].image?.link?.let { link ->
                            "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                        } ?: "",
                        contentDescription = images[i].alt,
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
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0..1) {
                    val index = row * 2 + col
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(images[index]) }
                    ) {
                        AsyncImage(
                            model = images[index].fullsize ?: images[index].image?.link?.let { link ->
                                "https://cdn.bsky.app/img/feed_fullsize/plain/$link@jpeg"
                            } ?: "",
                            contentDescription = images[index].alt,
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
public fun HashtagBadge(
    hashtag: String,
    postCount: Int? = null,
    isTrending: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "#$hashtag",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (isTrending) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Trending",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            
            postCount?.let {
                Text(
                    text = CommonUtils.formatCount(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}