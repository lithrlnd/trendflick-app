package com.trendflick.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.*
import java.time.LocalTime
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

// Add our own lerp function
private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

sealed class BubbleType {
    object Gallery : BubbleType()
    object Creative : BubbleType()
    object Trending : BubbleType()
}

@Composable
fun CreativeCompass(
    modifier: Modifier = Modifier,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    onGalleryClick: () -> Unit,
    galleryThumbnailUri: String? = null
) {
    var isRecording by remember { mutableStateOf(false) }
    val ringAnimation = rememberInfiniteTransition()
    val density = LocalDensity.current
    
    // Animation states
    var hasAnimationPlayed by remember { mutableStateOf(false) }
    val rotationState = remember { Animatable(0f) }
    val finalPositionState = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // First rotation animation
        rotationState.animateTo(
            targetValue = 720f, // Two full rotations
            animationSpec = tween(
                durationMillis = 3000,
                easing = LinearEasing
            )
        )
        // Then transition to final positions
        finalPositionState.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        hasAnimationPlayed = true
    }
    
    // Ring pulse animation
    val ringScale by ringAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Time-based color
    val currentHour = LocalTime.now().hour
    val ringColor = when {
        currentHour in 6..11 -> Color(0xFFFFB74D) // Morning orange
        currentHour in 12..17 -> Color(0xFF4FC3F7) // Afternoon blue
        currentHour in 18..21 -> Color(0xFFFF7043) // Evening orange-red
        else -> Color(0xFF7E57C2) // Night purple
    }
    
    // Bubble animations
    val bubbleAnimation = rememberInfiniteTransition()
    val bubbleOffset by bubbleAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isRecording = true
                        onRecordStart()
                        tryAwaitRelease()
                        isRecording = false
                        onRecordStop()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Main ring
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
        ) {
            // Draw pulsing ring
            drawCircle(
                color = ringColor,
                radius = size.minDimension / 2 * ringScale,
                style = Stroke(
                    width = with(density) { 8.dp.toPx() },
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(20f, 10f),
                        phase = bubbleOffset
                    )
                )
            )
            
            if (isRecording) {
                // Draw recording indicator
                drawCircle(
                    color = Color.Red,
                    radius = size.minDimension / 4,
                    alpha = 0.6f
                )
            }
        }

        // Floating bubbles
        val bubbleTypes = listOf(BubbleType.Creative, BubbleType.Gallery, BubbleType.Trending)
        bubbleTypes.forEachIndexed { index, type ->
            val radius = 80.dp
            
            // Calculate the current angle for rotation animation
            val initialAngle = rotationState.value + (index * 120f)
            val currentAngle = if (hasAnimationPlayed) {
                when (index) {
                    0 -> 270f  // Left
                    1 -> 270f  // Center
                    else -> 270f  // Right
                }
            } else {
                initialAngle % 360f
            }
            
            // Calculate final x offset for horizontal alignment
            val x = if (hasAnimationPlayed) {
                when (index) {
                    0 -> with(density) { -radius.toPx() * 2f }  // Left (Creative)
                    1 -> 0f  // Center (Gallery)
                    else -> with(density) { radius.toPx() * 2f }  // Right (Trending)
                }
            } else {
                with(density) { cos(Math.toRadians(currentAngle.toDouble())).toFloat() * radius.toPx() }
            }
            
            // Calculate y position - all at same height
            val y = if (hasAnimationPlayed) {
                with(density) { radius.toPx() * 2.0f }  // Moved further down to intersect with circle
            } else {
                with(density) { sin(Math.toRadians(currentAngle.toDouble())).toFloat() * radius.toPx() }
            }
            
            val bubbleScale by animateFloatAsState(
                targetValue = if (isRecording) 0.8f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            
            FloatingBubble(
                type = type,
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(60.dp)
                    .scale(bubbleScale),
                onClick = when (type) {
                    is BubbleType.Gallery -> onGalleryClick
                    else -> ({})
                },
                thumbnailUri = if (type is BubbleType.Gallery) galleryThumbnailUri else null
            )
        }
    }
}

@Composable
private fun FloatingBubble(
    type: BubbleType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    thumbnailUri: String? = null
) {
    val bubbleColor = when (type) {
        is BubbleType.Gallery -> MaterialTheme.colorScheme.primaryContainer
        is BubbleType.Creative -> MaterialTheme.colorScheme.secondaryContainer
        is BubbleType.Trending -> MaterialTheme.colorScheme.tertiaryContainer
    }
    
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bubbleColor.copy(alpha = 0.9f),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                type is BubbleType.Gallery && thumbnailUri != null -> {
                    AsyncImage(
                        model = thumbnailUri,
                        contentDescription = "Gallery Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        imageVector = when (type) {
                            is BubbleType.Gallery -> Icons.Default.PhotoLibrary
                            is BubbleType.Creative -> Icons.Default.AutoAwesome
                            is BubbleType.Trending -> Icons.Default.Whatshot
                        },
                        contentDescription = when (type) {
                            is BubbleType.Gallery -> "Open Gallery"
                            is BubbleType.Creative -> "Creative Tools"
                            is BubbleType.Trending -> "Trending"
                        },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
} 