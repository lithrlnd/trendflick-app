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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Reply
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
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.utils.DateUtils
import kotlinx.coroutines.delay
import com.trendflick.ui.components.CategoryDrawer
import androidx.media3.common.util.UnstableApi
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import com.trendflick.ui.utils.CommonUtils
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// CompositionLocal for providing the AtProtocolRepository
val LocalAtProtocolRepository = staticCompositionLocalOf<AtProtocolRepository> {
    error("AtProtocolRepository not provided")
}

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
private fun RepostEmbed(
    repostInfo: RepostInfo,
    onImageClick: (ImageEmbed) -> Unit,
    onHashtagClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Debug logging for repost embed
    LaunchedEffect(repostInfo) {
        Log.d("RepostEmbed", """
            📝 Processing repost embed:
            Author: ${repostInfo.author.displayName ?: repostInfo.author.handle}
            Text: ${repostInfo.record.text.take(50)}${if (repostInfo.record.text.length > 50) "..." else ""}
            Has facets: ${repostInfo.record.facets?.isNotEmpty() == true}
            Facet count: ${repostInfo.record.facets?.size ?: 0}
            Has embed: ${repostInfo.record.embed != null}
            Embed type: ${repostInfo.record.embed?.type}
            ${repostInfo.record.embed?.let { embed ->
                """
                Images: ${embed.images?.size ?: 0}
                Video: ${embed.video != null}
                External: ${embed.external != null}
                External URI: ${embed.external?.uri}
                """
            } ?: "No embed details"}
        """.trimIndent())
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Author row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onProfileClick != null, onClick = { onProfileClick?.invoke() }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = repostInfo.author.avatar,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        text = when {
                            !repostInfo.author.displayName.isNullOrBlank() -> repostInfo.author.displayName
                            !repostInfo.author.handle.isNullOrBlank() -> repostInfo.author.handle
                            else -> "Unknown User"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${repostInfo.author.handle} · ${DateUtils.formatTimestamp(repostInfo.record.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Post content
            RichTextRenderer(
                text = repostInfo.record.text,
                facets = repostInfo.record.facets ?: emptyList(),
                onMentionClick = { did: String -> onProfileClick?.invoke() },
                onHashtagClick = { tag: String -> onHashtagClick?.invoke(tag) },
                onLinkClick = { url: String -> 
                    onLinkClick?.invoke(url) ?: run {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                }
            )
            
            // Handle embeds in reposted content
            repostInfo.record.embed?.let { embed ->
                Spacer(modifier = Modifier.height(8.dp))
                PostEmbed(
                    embed = embed,
                    onImageClick = onImageClick,
                    onLinkClick = onLinkClick,
                    onProfileClick = onProfileClick,
                    onHashtagClick = onHashtagClick
                )
            }
        }
    }
}

@Composable
private fun RecordEmbed(
    uri: String,
    cid: String,
    onImageClick: (ImageEmbed) -> Unit,
    onHashtagClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var quotedPost by remember { mutableStateOf<Post?>(null) }
    var showInAppBrowser by remember { mutableStateOf(false) }
    var webViewHeight by remember { mutableStateOf(400.dp) }
    
    // Get repository instance
    val atProtocolRepository = LocalAtProtocolRepository.current
    
    // Extract handle and post ID from URI for better display
    val uriParts = uri.split("/")
    val handle = uriParts.getOrNull(uriParts.size - 3)?.removePrefix("@") ?: ""
    val postId = uriParts.lastOrNull() ?: ""
    
    // Convert AT URI to web URL for better handling
    val webUrl = if (uri.startsWith("at://")) {
        val didPart = uri.substringAfter("at://").substringBefore("/")
        val collection = uri.substringAfter("$didPart/").substringBefore("/")
        val rkey = uri.substringAfterLast("/")
        "https://bsky.app/profile/$handle/post/$rkey"
    } else {
        uri
    }
    
    // Generate oEmbed URL if possible
    val oEmbedUrl = if (webUrl.startsWith("https://bsky.app/profile/") && webUrl.contains("/post/")) {
        "https://embed.bsky.app/oembed?url=${Uri.encode(webUrl)}"
    } else {
        null
    }
    
    // Function to fetch the post
    val fetchPost = {
        isLoading = true
        error = null
        
        // Launch in a coroutine
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Fetch the quoted post from the repository
                val postResult = atProtocolRepository.getPostByUri(uri, cid)
                if (postResult.isSuccess) {
                    quotedPost = postResult.getOrNull()
                    Log.d("RecordEmbed", "✅ Successfully fetched quoted post: ${quotedPost?.record?.text?.take(50)}")
                } else {
                    Log.e("RecordEmbed", "❌ Failed to fetch quoted post: ${postResult.exceptionOrNull()?.message}")
                    error = "Failed to load quoted post"
                }
            } catch (e: Exception) {
                Log.e("RecordEmbed", "Error loading quoted post: ${e.message}")
                error = "Failed to load quoted post"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Debug logging
    LaunchedEffect(uri, cid) {
        Log.d("RecordEmbed", """
            📄 Processing record embed:
            URI: $uri
            CID: $cid
            Web URL: $webUrl
            oEmbed URL: $oEmbedUrl
            Handle: $handle
            Post ID: $postId
        """.trimIndent())
        
        // Automatically fetch the post when the component is first displayed
        fetchPost()
    }
    
    // In-app browser implementation
    if (showInAppBrowser) {
        AlertDialog(
            onDismissRequest = { showInAppBrowser = false },
            title = { 
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bluesky Post")
                    IconButton(onClick = { showInAppBrowser = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Use WebView to display the post
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                
                                webViewClient = object : android.webkit.WebViewClient() {
                                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // Inject JavaScript to get the height of the content
                                        evaluateJavascript(
                                            "(function() { return document.body.scrollHeight; })();"
                                        ) { height ->
                                            try {
                                                val contentHeight = height.toFloat()
                                                if (contentHeight > 0) {
                                                    // Limit the height to a reasonable value
                                                    webViewHeight = (contentHeight.coerceAtMost(800f)).dp
                                                }
                                            } catch (e: Exception) {
                                                Log.e("RecordEmbed", "Error parsing height: $e")
                                            }
                                        }
                                    }
                                }
                                
                                // Try to use oEmbed URL if available, otherwise use the web URL
                                loadUrl(oEmbedUrl ?: webUrl)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(webViewHeight)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInAppBrowser = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                    context.startActivity(intent)
                    showInAppBrowser = false
                }) {
                    Text("Open in Browser")
                }
            }
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { showInAppBrowser = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (error != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "This post is unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Add a retry button
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { fetchPost() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Retry")
                }
                
                // Add a "View in Browser" button
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showInAppBrowser = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View in Browser")
                }
            }
        } else if (quotedPost != null) {
            // Display the quoted post content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Author row
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = quotedPost?.author?.avatar,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    
                    Column {
                        Text(
                            text = when {
                                !quotedPost?.author?.displayName.isNullOrBlank() -> quotedPost?.author?.displayName ?: ""
                                !quotedPost?.author?.handle.isNullOrBlank() -> quotedPost?.author?.handle ?: ""
                                else -> "Unknown User"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${quotedPost?.author?.handle ?: "unknown"} · ${DateUtils.formatTimestamp(quotedPost?.record?.createdAt ?: "")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Post content
                quotedPost?.record?.text?.let { text ->
                    RichTextRenderer(
                        text = text,
                        facets = quotedPost?.record?.facets ?: emptyList(),
                        onMentionClick = { did: String -> onProfileClick?.invoke() },
                        onHashtagClick = { tag: String -> onHashtagClick?.invoke(tag) },
                        onLinkClick = { url: String -> 
                            onLinkClick?.invoke(url) ?: run {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
                
                // Handle embeds in quoted post
                quotedPost?.embed?.let { embed ->
                    Spacer(modifier = Modifier.height(8.dp))
                    PostEmbed(
                        embed = embed,
                        onImageClick = onImageClick,
                        onLinkClick = onLinkClick,
                        onProfileClick = onProfileClick,
                        onHashtagClick = onHashtagClick
                    )
                }
            }
        } else {
            // Enhanced placeholder for quoted post with better thumbnail handling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Author row placeholder with better styling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar - try to load actual avatar if handle is available
                    if (handle.isNotEmpty()) {
                        AsyncImage(
                            model = "https://api.dicebear.com/6.x/initials/png?seed=$handle",
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = handle.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Column {
                        if (handle.isNotEmpty()) {
                            Text(
                                text = "@$handle",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Post from Bluesky",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Bluesky Post",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Enhanced thumbnail generation with multiple fallbacks
                val thumbnailUrl = if (handle.isNotEmpty() && postId.isNotEmpty()) {
                    // Try oEmbed thumbnail first
                    if (oEmbedUrl != null) {
                        // Use the oEmbed URL as a screenshot source
                        val encodedOEmbedUrl = Uri.encode(oEmbedUrl)
                        "https://api.microlink.io/?url=$encodedOEmbedUrl&screenshot=true&meta=false&embed=screenshot.url"
                    } else {
                        // Primary option: Use microlink.io for screenshot-based thumbnails
                        val encodedUrl = Uri.encode(webUrl)
                        "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
                    }
                } else {
                    // Fallback: Use a generic Bluesky logo
                    "https://bsky.app/static/apple-touch-icon.png"
                }

                Log.d("RecordEmbed", "🖼️ Using thumbnail URL: $thumbnailUrl")
                
                if (thumbnailUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "Post preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = android.R.drawable.ic_menu_gallery)
                        )
                        
                        // Semi-transparent overlay with tap instruction
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tap to view post",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    // Fallback content preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Tap to view post",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostEmbed(
    embed: Embed,
    onImageClick: (ImageEmbed) -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onProfileClick: (() -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
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
        oEmbedUrl: ${embed.external?.oEmbedUrl}
    """.trimIndent())

    when {
        // Handle record embeds (quote posts)
        embed.type == "app.bsky.embed.record" || embed.record != null -> {
            // Extract the URI and CID from the record embed
            val recordUri = embed.record?.uri ?: ""
            val recordCid = embed.record?.cid ?: ""
            
            Log.d("ThreadCard", """
                🔗 Record embed detected:
                Type: ${embed.type}
                URI: $recordUri
                CID: $recordCid
            """.trimIndent())
            
            if (recordUri.isNotEmpty()) {
                RecordEmbed(
                    uri = recordUri,
                    cid = recordCid,
                    onImageClick = onImageClick,
                    onHashtagClick = onHashtagClick,
                    onLinkClick = onLinkClick,
                    onProfileClick = onProfileClick,
                    modifier = modifier
                )
            } else {
                // Fallback for when we can't extract the URI/CID
                Text(
                    text = "Unable to display quoted post",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        // Handle oEmbed content first
        embed.external?.oEmbedUrl != null -> {
            Log.d("ThreadCard", "🎬 oEmbed URL detected: ${embed.external.oEmbedUrl}")
            
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
            ) {
                VideoPlayer(
                    videoUrl = embed.external.oEmbedUrl!!,
                    isVisible = true,
                    onProgressChanged = { progress = it },
                    isPaused = isPaused,
                    modifier = Modifier.fillMaxSize(),
                    onError = { loadError = it },
                    thumbnailUrl = embed.external.thumb?.link?.let { link ->
                        if (link.startsWith("http")) link else "https://cdn.bsky.app/img/feed_thumbnail/plain/$link@jpeg"
                    },
                    isOEmbedVideo = true
                )
            }
        }
        // Enhanced video content handling
        embed.video != null || (embed.external?.uri?.let { uri ->
            uri.endsWith(".mp4", ignoreCase = true) ||
            uri.endsWith(".mov", ignoreCase = true) ||
            uri.endsWith(".webm", ignoreCase = true) ||
            uri.contains("video", ignoreCase = true) ||
            uri.contains("youtube.com") ||
            uri.contains("youtu.be") ||
            uri.contains("vimeo.com") ||
            uri.contains("tiktok.com") && uri.contains("/video/")
        } == true) -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
            ) {
                // Determine the video URL based on the embed type
                val videoUrl = when {
                    // Direct Bluesky video
                    embed.video?.ref?.link != null -> {
                        "https://cdn.bsky.app/video/plain/${embed.video.ref.link}"
                    }
                    // External video with special handling for platforms
                    embed.external?.uri != null -> {
                        when {
                            // YouTube videos
                            embed.external.uri.contains("youtube.com") || 
                            embed.external.uri.contains("youtu.be") -> {
                                val videoId = extractYouTubeVideoId(embed.external.uri)
                                if (videoId.isNotBlank()) {
                                    "https://www.youtube.com/embed/$videoId"
                                } else {
                                    embed.external.uri
                                }
                            }
                            // Vimeo videos
                            embed.external.uri.contains("vimeo.com") -> {
                                val videoId = extractVimeoVideoId(embed.external.uri)
                                if (videoId.isNotBlank()) {
                                    "https://player.vimeo.com/video/$videoId"
                                } else {
                                    embed.external.uri
                                }
                            }
                            // TikTok videos
                            embed.external.uri.contains("tiktok.com") && embed.external.uri.contains("/video/") -> {
                                val videoId = extractTikTokVideoId(embed.external.uri)
                                if (videoId.isNotBlank()) {
                                    "https://www.tiktok.com/embed/v2/$videoId"
                                } else {
                                    embed.external.uri
                                }
                            }
                            // Twitter/X videos
                            (embed.external.uri.contains("twitter.com") || embed.external.uri.contains("x.com")) && 
                            embed.external.uri.contains("/status/") -> {
                                "https://publish.twitter.com/oembed?url=${Uri.encode(embed.external.uri)}&omit_script=true"
                            }
                            // Instagram videos/posts
                            embed.external.uri.contains("instagram.com/p/") -> {
                                "https://www.instagram.com/embed.js?url=${Uri.encode(embed.external.uri)}"
                            }
                            // Default: use the URI as is
                            else -> embed.external.uri
                        }
                    }
                    else -> ""
                }

                // Get thumbnail URL for video
                val thumbnailUrl = when {
                    embed.video?.ref?.link != null -> {
                        // Try to generate a thumbnail URL for Bluesky videos
                        "https://cdn.bsky.app/img/feed_thumbnail/plain/${embed.video.ref.link}@jpeg"
                    }
                    embed.external?.thumb?.link != null -> {
                        val link = embed.external.thumb.link
                        if (link.startsWith("http")) link else "https://cdn.bsky.app/img/feed_thumbnail/plain/$link@jpeg"
                    }
                    embed.external?.uri?.contains("youtube.com") == true || 
                    embed.external?.uri?.contains("youtu.be") == true -> {
                        val videoId = extractYouTubeVideoId(embed.external.uri!!)
                        if (videoId.isNotBlank()) {
                            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                        } else null
                    }
                    embed.external?.uri?.contains("vimeo.com") == true -> {
                        val videoId = extractVimeoVideoId(embed.external.uri!!)
                        if (videoId.isNotBlank()) {
                            "https://vumbnail.com/$videoId.jpg"
                        } else null
                    }
                    embed.external?.uri?.contains("tiktok.com") == true -> {
                        // Use microlink for TikTok thumbnails
                        val encodedUrl = Uri.encode(embed.external.uri!!)
                        "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
                    }
                    embed.external?.uri?.contains("twitter.com") == true || 
                    embed.external?.uri?.contains("x.com") == true -> {
                        // Use microlink for Twitter/X thumbnails
                        val encodedUrl = Uri.encode(embed.external.uri!!)
                        "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
                    }
                    embed.external?.uri?.contains("instagram.com") == true -> {
                        // Use microlink for Instagram thumbnails
                        val encodedUrl = Uri.encode(embed.external.uri!!)
                        "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
                    }
                    else -> null
                }

                Log.d("ThreadCard", """
                    🎥 Video details:
                    URL: $videoUrl
                    Thumbnail: $thumbnailUrl
                """.trimIndent())

                if (videoUrl.isNotEmpty()) {
                    // Determine if this is an embedded player that should use WebView
                    val isEmbeddedPlayer = videoUrl.contains("youtube.com/embed/") || 
                                          videoUrl.contains("player.vimeo.com/") ||
                                          videoUrl.contains("tiktok.com/embed") ||
                                          videoUrl.contains("publish.twitter.com/oembed") ||
                                          videoUrl.contains("instagram.com/embed")
                    
                    VideoPlayer(
                        videoUrl = videoUrl,
                        isVisible = true,
                        onProgressChanged = { progress = it },
                        isPaused = isPaused,
                        modifier = Modifier.fillMaxSize(),
                        onError = { loadError = it },
                        thumbnailUrl = thumbnailUrl,
                        isOEmbedVideo = isEmbeddedPlayer
                    )

                    // Video controls overlay for non-embedded videos
                    if (!isEmbeddedPlayer) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { isPaused = !isPaused }
                                    )
                                }
                        )
                        
                        // Progress bar for non-embedded videos
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .background(
                    color = if (isActive) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                    else 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    scale = 0.8f
                    onClick()
                    scale = 1f
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        tint,
                    modifier = Modifier.size(24.dp)
                )
            }
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
    isFollowing: Boolean = false,
    isFollowingLoading: Boolean = false,
    onLikeClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: () -> Unit,
    onThreadClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCreatePost: () -> Unit,
    onFollowClick: () -> Unit,
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

    // Debug logging for repost information
    LaunchedEffect(feedPost) {
        if (feedPost.reason?.type == "app.bsky.feed.repost") {
            Log.d("ThreadCard", """
                🔄 Repost detected:
                Type: ${feedPost.reason.type}
                By: ${feedPost.reason.by?.displayName ?: feedPost.reason.by?.handle ?: "Unknown"}
                Has repost info: ${feedPost.reason.repost != null}
                ${feedPost.reason.repost?.let { repost ->
                    """
                    Repost author: ${repost.author.displayName ?: repost.author.handle}
                    Repost text: ${repost.record.text.take(50)}${if (repost.record.text.length > 50) "..." else ""}
                    Has embed: ${repost.record.embed != null}
                    Embed type: ${repost.record.embed?.type}
                    """
                } ?: "No repost details available"}
            """.trimIndent())
        }
    }

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
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
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
                    
                    // Follow button
                    OutlinedButton(
                        onClick = onFollowClick,
                        enabled = !isFollowingLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isFollowing) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                            contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isFollowing) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (isFollowingLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
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

                    // Display repost content if available
                    feedPost.reason?.repost?.let { repostInfo ->
                        Spacer(modifier = Modifier.height(12.dp))
                        RepostEmbed(
                            repostInfo = repostInfo,
                            onImageClick = onImageClick,
                            onHashtagClick = onHashtagClick,
                            onLinkClick = onLinkClick,
                            onProfileClick = { onProfileClick() }
                        )
                    }

                    // Handle embeds
                    feedPost.post.embed?.let { embed ->
                        Spacer(modifier = Modifier.height(8.dp))
                        PostEmbed(
                            embed = embed,
                            onImageClick = { image ->
                                selectedImageIndex = feedPost.post.embed.images?.indexOf(image)
                                onImageClick(image)
                            },
                            onLinkClick = onLinkClick,
                            onProfileClick = onProfileClick,
                            onHashtagClick = onHashtagClick
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
        Site name: ${thumbnail.siteName}
        oEmbed URL: ${thumbnail.oEmbedUrl}
        Embed type: ${thumbnail.getEmbedType()}
    """.trimIndent())

    // Detect if this is a social media post that should be embedded
    val socialMediaInfo = thumbnail.getSocialMediaInfo()
    val isEmbeddableMedia = socialMediaInfo != null || 
                           thumbnail.oEmbedUrl != null ||
                           url.contains("bsky.app/profile") && url.contains("/post/")
    
    // Generate oEmbed URL if possible and not already provided
    val oEmbedUrl = thumbnail.generateOEmbedUrl()
    
    // Define these variables at the function level so they're available throughout the scope
    val shouldShowLargeThumbnail = url.contains("kingdomsandemo.com") || 
                                  url.contains("substack.com") ||
                                  url.contains("medium.com") ||
                                  url.contains("bsky.app") ||
                                  url.contains("youtube.com") ||
                                  url.contains("youtu.be") ||
                                  url.contains("vimeo.com") ||
                                  (thumbnail.thumb != null && !title.contains("http"))
    
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
                    "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                } else {
                    ""
                }
            }
            // Vimeo thumbnails
            url.contains("vimeo.com") -> {
                val videoId = extractVimeoVideoId(url)
                if (videoId.isNotBlank()) {
                    "https://vumbnail.com/$videoId.jpg"
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
            url.contains("facebook.com") ||
            url.contains("kingdomsandemo.com") -> {
                val encodedUrl = Uri.encode(url)
                "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
            }
            // Bluesky posts
            url.contains("bsky.app/profile") && url.contains("/post/") -> {
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
    
    Log.d("ThreadCard", "🔄 Generated oEmbed URL: $oEmbedUrl")
    Log.d("ThreadCard", "🖼️ Using thumbnail URL: $thumbnailUrl")

    // Check if this is a video link that should be embedded
    val isVideoLink = url.endsWith(".mp4", ignoreCase = true) ||
                     url.endsWith(".mov", ignoreCase = true) ||
                     url.endsWith(".webm", ignoreCase = true) ||
                     url.contains("youtube.com/watch") ||
                     url.contains("youtu.be/") ||
                     url.contains("vimeo.com/")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Handle oEmbed content if available
            if (oEmbedUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    VideoPlayer(
                        videoUrl = oEmbedUrl,
                        isVisible = true,
                        modifier = Modifier.fillMaxSize(),
                        onError = { Log.e("EmbeddedLink", "Error loading oEmbed: $it") },
                        isOEmbedVideo = true,
                        thumbnailUrl = thumbnail.thumb?.link?.let { link ->
                            if (link.startsWith("http")) link else "https://cdn.bsky.app/img/feed_thumbnail/plain/$link@jpeg"
                        }
                    )
                }
            }
            // Enhanced thumbnail handling - now as a header image for important links
            else {
                // Handle video links
                if (isVideoLink && shouldShowLargeThumbnail) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clickable(onClick = onClick)
                    ) {
                        if (thumbnailUrl.isNotEmpty()) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = "Video thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // Play button overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
                // Regular image thumbnail
                else if (shouldShowLargeThumbnail && thumbnailUrl.isNotEmpty()) {
                    // Large header image for important links
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "Link preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            
            // Content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show small thumbnail only if we're not showing the large one and no oEmbed
                if (!shouldShowLargeThumbnail && oEmbedUrl == null) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
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
                    
                    // Show domain name and platform info if available
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Show domain name
                        val domain = try {
                            Uri.parse(url).host ?: url
                        } catch (e: Exception) {
                            url
                        }
                        
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Show platform badge if it's a social media link
                        socialMediaInfo?.let {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = it.platform,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function for TikTok video ID extraction
private fun extractTikTokVideoId(url: String): String {
    val pattern = """tiktok\.com\/.*\/video\/([0-9]+)"""
    val regex = Regex(pattern)
    return regex.find(url)?.groupValues?.get(1) ?: ""
}