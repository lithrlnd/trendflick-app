package com.trendflick.ui.components

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
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

@UnstableApi
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    onProgressChanged: (Float) -> Unit = {},
    isPaused: Boolean = false,
    playbackSpeed: Float = 1f
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Create data source factory with more lenient settings
    val httpDataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent("TrendFlick")
            .setConnectTimeoutMs(16000)
            .setReadTimeoutMs(16000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
    }
    
    val dataSourceFactory = remember {
        DefaultDataSource.Factory(context, httpDataSourceFactory)
    }

    // Create media source factory with all supported formats
    val mediaSourceFactory = remember {
        DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
    }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 1f
                playWhenReady = !isPaused
                setHandleAudioBecomingNoisy(true)
            }
    }

    DisposableEffect(videoUrl) {
        val player = exoPlayer // Capture player reference for onDispose
        
        try {
            Log.d("VideoPlayer", "Attempting to play video from URL: $videoUrl")
            
            // Handle Bluesky CDN URLs
            val processedUrl = if (videoUrl.contains("cdn.bsky.social")) {
                // Ensure we're using the correct CDN endpoint
                if (!videoUrl.contains("/video/plain/")) {
                    val ref = videoUrl.substringAfterLast("/")
                    "https://cdn.bsky.social/video/plain/$ref"
                } else videoUrl
            } else videoUrl
            
            Log.d("VideoPlayer", "Processed URL: $processedUrl")
            
            // Configure HTTP data source for Bluesky CDN
            httpDataSourceFactory
                .setDefaultRequestProperties(mapOf(
                    "User-Agent" to "TrendFlick/1.0",
                    "Accept" to "*/*",
                    "Range" to "bytes=0-"
                ))
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)
            
            // Default to MP4 for Bluesky CDN URLs
            val mimeType = when {
                processedUrl.contains("cdn.bsky.social") -> MimeTypes.VIDEO_MP4
                processedUrl.endsWith(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
                processedUrl.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                processedUrl.endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
                processedUrl.endsWith(".webm", ignoreCase = true) -> MimeTypes.VIDEO_WEBM
                processedUrl.endsWith(".mkv", ignoreCase = true) -> MimeTypes.VIDEO_MATROSKA
                else -> MimeTypes.VIDEO_MP4 // Default to MP4 if unknown
            }
            
            val mediaItem = MediaItem.Builder()
                .setUri(processedUrl)
                .setMimeType(mimeType)
                .build()
                
            Log.d("VideoPlayer", "Created MediaItem with MIME type: $mimeType")
            
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.prepare()
            
            // Track progress and loading state
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    super.onPlaybackStateChanged(state)
                    when (state) {
                        Player.STATE_READY -> {
                            isLoading = false
                            hasError = false
                            errorMessage = null
                            scope.launch {
                                while (true) {
                                    if (!isPaused) {
                                        val progress = if (player.duration > 0) {
                                            player.currentPosition.toFloat() / player.duration.toFloat()
                                        } else 0f
                                        onProgressChanged(progress)
                                    }
                                    delay(16) // ~60fps update rate
                                }
                            }
                        }
                        Player.STATE_BUFFERING -> {
                            isLoading = true
                            hasError = false
                            errorMessage = null
                        }
                        Player.STATE_ENDED -> {
                            isLoading = false
                            hasError = false
                            errorMessage = null
                        }
                        Player.STATE_IDLE -> {
                            isLoading = false
                            hasError = true
                            errorMessage = "Failed to initialize playback"
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    super.onPlayerError(error)
                    Log.e("VideoPlayer", "Playback error: ${error.message}", error)
                    
                    hasError = true
                    isLoading = false
                    errorMessage = when {
                        error.message?.contains("UnrecognizedInputFormatException") == true -> {
                            Log.e("VideoPlayer", "Format error for URL: $processedUrl")
                            "Unsupported video format. Please try a different format."
                        }
                        error.message?.contains("Unable to connect") == true -> {
                            Log.e("VideoPlayer", "Connection error for URL: $processedUrl")
                            "Unable to connect to video source. Please check your connection."
                        }
                        error.message?.contains("timeout") == true -> {
                            Log.e("VideoPlayer", "Timeout error for URL: $processedUrl")
                            "Connection timed out. Please try again."
                        }
                        else -> {
                            Log.e("VideoPlayer", "Unknown error for URL: $processedUrl")
                            "Failed to play video: ${error.message}"
                        }
                    }
                    
                    // Try to recover by resetting the player
                    player.stop()
                    player.clearMediaItems()
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    super.onIsLoadingChanged(isLoading)
                    if (isLoading) {
                        Log.d("VideoPlayer", "Loading media...")
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    Log.d("VideoPlayer", "Playing state changed: $isPlaying")
                }
            }
            player.addListener(listener)

            onDispose {
                player.removeListener(listener)
                player.release()
            }
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error setting up player: ${e.message}", e)
            hasError = true
            isLoading = false
            errorMessage = "Failed to initialize player: ${e.message}"
            
            onDispose {
                player.release()
            }
        }
    }

    // Handle pause state
    LaunchedEffect(isPaused) {
        if (isPaused) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    // Handle playback speed
    LaunchedEffect(playbackSpeed) {
        try {
            exoPlayer.setPlaybackSpeed(playbackSpeed)
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error setting playback speed: ${e.message}", e)
            exoPlayer.setPlaybackSpeed(1f)
        }
    }

    DisposableEffect(isVisible) {
        if (!isVisible) {
            exoPlayer.pause()
        } else if (!isPaused) {
            exoPlayer.play()
        }
        onDispose { }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "Failed to load video",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 