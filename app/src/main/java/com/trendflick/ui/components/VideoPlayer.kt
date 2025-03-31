package com.trendflick.ui.components

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 1f
            playWhenReady = !isPaused
            setHandleAudioBecomingNoisy(true)
        }
    }

    DisposableEffect(videoUrl) {
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        
        // Track progress
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                if (state == Player.STATE_READY) {
                    scope.launch {
                        while (true) {
                            if (!isPaused) {
                                val progress = if (exoPlayer.duration > 0) {
                                    exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                                } else 0f
                                onProgressChanged(progress)
                            }
                            delay(16) // ~60fps update rate
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                // Handle any additional playback state changes if needed
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
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
            // Fallback to normal speed if setting fails
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

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = modifier
    )
} 