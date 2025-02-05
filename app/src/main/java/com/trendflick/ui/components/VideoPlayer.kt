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

@UnstableApi
@Composable
fun VideoPlayer(
    videoUrl: String,
    isVisible: Boolean,
    onProgressChanged: (Float) -> Unit,
    playbackSpeed: Float = 1f,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var errorState by remember { mutableStateOf<String?>(null) }

    DisposableEffect(videoUrl) {
        try {
            // Create player instance
            val newPlayer = ExoPlayer.Builder(context).build().apply {
                // Set up media source
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                
                // Add error listener
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("VideoPlayer", "Player error: ${error.message}")
                        errorState = error.message
                        onError(error.message ?: "Failed to load video")
                    }
                })
                
                // Prepare player
                prepare()
                playWhenReady = !isPaused
                repeatMode = Player.REPEAT_MODE_ONE
            }
            
            player = newPlayer
            
            onDispose {
                newPlayer.release()
                player = null
            }
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error initializing player: ${e.message}")
            errorState = e.message
            onError(e.message ?: "Failed to initialize video player")
            onDispose {}
        }
    }

    // Handle visibility changes
    LaunchedEffect(isVisible) {
        player?.playWhenReady = isVisible && !isPaused
    }

    // Handle pause state changes
    LaunchedEffect(isPaused) {
        player?.playWhenReady = !isPaused && isVisible
    }

    // Handle playback speed changes
    LaunchedEffect(playbackSpeed) {
        player?.setPlaybackSpeed(playbackSpeed)
    }

    // Update progress
    LaunchedEffect(player) {
        while (true) {
            delay(16) // ~60fps
            player?.let { exoPlayer ->
                if (exoPlayer.isPlaying) {
                    val progress = exoPlayer.currentPosition.toFloat() / 
                                 exoPlayer.duration.coerceAtLeast(1)
                    onProgressChanged(progress.coerceIn(0f, 1f))
                }
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show error state if needed
        errorState?.let { error ->
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
                        text = "Failed to load video",
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