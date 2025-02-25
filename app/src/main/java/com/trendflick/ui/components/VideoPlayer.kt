package com.trendflick.ui.components

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import android.content.Intent

@UnstableApi
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    onProgressChanged: (Float) -> Unit = {},
    onError: (String) -> Unit = {},
    playbackSpeed: Float = 1f,
    isPaused: Boolean = false,
    thumbnailUrl: String? = null,
    isOEmbedVideo: Boolean = false
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Debug logging for video URL
    LaunchedEffect(videoUrl) {
        Log.d("VideoPlayer", """
            🎬 Video player initialized:
            URL: $videoUrl
            Is oEmbed: $isOEmbedVideo
            Thumbnail: $thumbnailUrl
            Is visible: $isVisible
        """.trimIndent())
    }
    
    // Handle oEmbed videos differently
    if (isOEmbedVideo) {
        Box(modifier = modifier) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Show thumbnail while loading if available
            if (isLoading && !thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Determine if this is a Bluesky oEmbed URL
            val isBlueskyEmbed = videoUrl.contains("embed.bsky.app/oembed")
            
            // Determine if this is a YouTube embed URL
            val isYouTubeEmbed = videoUrl.contains("youtube.com/embed/") || videoUrl.contains("youtu.be/")
            
            // Determine if this is a Vimeo embed URL
            val isVimeoEmbed = videoUrl.contains("player.vimeo.com/video/")
            
            // Determine if this is a Twitter/X embed URL
            val isTwitterEmbed = videoUrl.contains("twitter.com") || videoUrl.contains("x.com")
            
            // Determine if this is a TikTok embed URL
            val isTikTokEmbed = videoUrl.contains("tiktok.com")
            
            // Determine if this is an Instagram embed URL
            val isInstagramEmbed = videoUrl.contains("instagram.com")
            
            // Process the URL to ensure it's properly formatted for embedding
            val processedUrl = when {
                // For Bluesky oEmbed URLs, ensure they're properly encoded
                isBlueskyEmbed && !videoUrl.contains("?url=") && videoUrl.contains("https://bsky.app/") -> {
                    val encodedPostUrl = Uri.encode(videoUrl)
                    "https://embed.bsky.app/oembed?url=$encodedPostUrl"
                }
                // For YouTube URLs that aren't already in embed format
                isYouTubeEmbed && !videoUrl.contains("/embed/") -> {
                    val videoId = extractYouTubeVideoId(videoUrl)
                    if (videoId.isNotBlank()) "https://www.youtube.com/embed/$videoId" else videoUrl
                }
                // For Vimeo URLs that aren't already in embed format
                isVimeoEmbed && !videoUrl.contains("/video/") -> {
                    val videoId = extractVimeoVideoId(videoUrl)
                    if (videoId.isNotBlank()) "https://player.vimeo.com/video/$videoId" else videoUrl
                }
                // For Twitter/X URLs, use their oEmbed endpoint
                isTwitterEmbed && !videoUrl.contains("publish.twitter.com") -> {
                    val encodedUrl = Uri.encode(videoUrl)
                    "https://publish.twitter.com/oembed?url=$encodedUrl&omit_script=true"
                }
                // For TikTok URLs, use their embed endpoint
                isTikTokEmbed && !videoUrl.contains("embed") -> {
                    val encodedUrl = Uri.encode(videoUrl)
                    "https://www.tiktok.com/embed/v2/$encodedUrl"
                }
                // For Instagram URLs, use their embed endpoint
                isInstagramEmbed && !videoUrl.contains("embed") -> {
                    val encodedUrl = Uri.encode(videoUrl)
                    "https://www.instagram.com/embed.js?url=$encodedUrl"
                }
                // Default case: use the URL as is
                else -> videoUrl
            }
            
            Log.d("VideoPlayer", "🔄 Processed oEmbed URL: $processedUrl")
            
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        settings.allowContentAccess = true
                        settings.allowFileAccess = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                Log.d("VideoPlayer", "WebView page loaded: $url")
                                
                                // For Twitter embeds, inject script to resize the iframe
                                if (isTwitterEmbed) {
                                    view?.evaluateJavascript("""
                                        (function() {
                                            var tweets = document.querySelectorAll('iframe.twitter-tweet');
                                            for (var i = 0; i < tweets.length; i++) {
                                                tweets[i].style.width = '100%';
                                                tweets[i].style.height = 'auto';
                                            }
                                            return true;
                                        })();
                                    """.trimIndent(), null)
                                }
                                
                                // For Instagram embeds, inject script to resize the iframe
                                if (isInstagramEmbed) {
                                    view?.evaluateJavascript("""
                                        (function() {
                                            var posts = document.querySelectorAll('iframe.instagram-media');
                                            for (var i = 0; i < posts.length; i++) {
                                                posts[i].style.width = '100%';
                                                posts[i].style.height = 'auto';
                                            }
                                            return true;
                                        })();
                                    """.trimIndent(), null)
                                }
                            }
                            
                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                hasError = true
                                val errorDescription = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    error?.description?.toString() ?: "Unknown error"
                                } else {
                                    "Error loading content"
                                }
                                errorMessage = "Failed to load embedded content: $errorDescription"
                                Log.e("VideoPlayer", "WebView error: $errorMessage for URL: ${request?.url}")
                                onError(errorMessage)
                            }
                            
                            // Handle redirects and ensure HTTPS
                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                request?.url?.let { uri ->
                                    // If it's an external link (not part of the embed), open in browser
                                    if (!uri.toString().contains(videoUrl) && 
                                        !uri.toString().contains("embed") &&
                                        !uri.toString().contains("oembed")) {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                        return true
                                    }
                                }
                                return false
                            }
                        }
                        
                        // Try to use processed URL
                        Log.d("VideoPlayer", "Loading WebView URL: $processedUrl")
                        loadUrl(processedUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        // Add a button to open in external browser
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Open in Browser")
                        }
                    }
                }
            }
        }
        return
    }
    
    // Handle regular videos with ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 1f
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            isLoading = false
                            Log.d("VideoPlayer", "ExoPlayer ready: $videoUrl")
                        }
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("VideoPlayer", "ExoPlayer error: ${error.message} for URL: $videoUrl", error)
                        hasError = true
                        errorMessage = "Failed to play video: ${error.message}"
                        onError(errorMessage)
                    }
                })
            }
    }

    DisposableEffect(videoUrl) {
        try {
            Log.d("VideoPlayer", "Loading video with ExoPlayer: $videoUrl")
            
            // Create a custom media source factory with extended MIME type support
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
            
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
            // Create a media item with proper configuration
            val mediaItem = when {
                videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be") -> {
                    MediaItem.Builder()
                        .setUri(videoUrl)
                        .setMimeType(MimeTypes.APPLICATION_MPD)
                        .build()
                }
                videoUrl.endsWith(".mp4", ignoreCase = true) -> {
                    MediaItem.Builder()
                        .setUri(videoUrl)
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build()
                }
                videoUrl.endsWith(".m3u8", ignoreCase = true) -> {
                    MediaItem.Builder()
                        .setUri(videoUrl)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
                }
                else -> MediaItem.fromUri(videoUrl)
            }
            
            exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
            exoPlayer.prepare()
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error loading video: ${e.message} for URL: $videoUrl", e)
            hasError = true
            errorMessage = "Failed to load video: ${e.message}"
            onError(errorMessage)
        }

        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(isPaused) {
        if (isPaused) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }
    
    // Track progress
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            if (exoPlayer.duration > 0) {
                val progress = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                onProgressChanged(progress)
            }
        }
    }

    Box(modifier = modifier) {
        // Show thumbnail while loading
        if (isLoading && !thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color.White
                )
            }
        }
        
        // Error state
        if (hasError) {
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
                        text = errorMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Add a retry button
                    Button(
                        onClick = {
                            hasError = false
                            isLoading = true
                            try {
                                exoPlayer.prepare()
                                exoPlayer.play()
                            } catch (e: Exception) {
                                Log.e("VideoPlayer", "Error retrying video: ${e.message}", e)
                                hasError = true
                                errorMessage = "Failed to retry: ${e.message}"
                                onError(errorMessage)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

// Helper functions for video ID extraction
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

private fun extractTikTokVideoId(url: String): String {
    val pattern = """tiktok\.com\/.*\/video\/([0-9]+)"""
    val regex = Regex(pattern)
    return regex.find(url)?.groupValues?.get(1) ?: ""
} 