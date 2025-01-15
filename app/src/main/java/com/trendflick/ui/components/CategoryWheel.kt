package com.trendflick.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trendflick.data.model.VideoCategory
import kotlin.math.*

@Composable
fun CategoryWheel(
    categories: List<VideoCategory>,
    selectedCategory: VideoCategory?,
    onCategorySelected: (VideoCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationState by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.size(200.dp)) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        // Snap to nearest category
                        val segmentSize = 360f / categories.size
                        val targetRotation = (rotationState / segmentSize).roundToInt() * segmentSize
                        rotationState = targetRotation
                        
                        // Calculate selected category
                        val selectedIndex = ((-rotationState / segmentSize).roundToInt() + categories.size) % categories.size
                        onCategorySelected(categories[selectedIndex])
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dragAngle = dragAmount.x.coerceIn(-10f, 10f)
                        rotationState = (rotationState + dragAngle).coerceIn(-360f, 360f)
                    }
                )
            }
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.width.coerceAtMost(size.height) / 2 * 0.8f
            
            // Draw segments
            categories.forEachIndexed { index, category ->
                val startAngle = 360f / categories.size * index + rotationState
                val sweepAngle = 360f / categories.size
                
                // Draw segment
                drawArc(
                    color = category.color.copy(alpha = if (isDragging) 0.7f else 0.5f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(centerX - radius, centerY - radius)
                )
                
                // Draw border
                drawArc(
                    color = category.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    style = Stroke(width = 2.dp.toPx()),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(centerX - radius, centerY - radius)
                )
                
                // Calculate text position
                val textAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                val textRadius = radius * 0.7
                val textX = (centerX + cos(textAngle) * textRadius).toFloat()
                val textY = (centerY + sin(textAngle) * textRadius).toFloat()
                
                // Draw category icon and name
                drawContext.canvas.nativeCanvas.drawText(
                    category.icon,
                    textX,
                    textY,
                    Paint().apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 24.sp.toPx()
                        typeface = Typeface.DEFAULT
                        color = android.graphics.Color.WHITE
                    }
                )
            }
        }
        
        // Selected category indicator
        selectedCategory?.let { category ->
            Text(
                text = category.name,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
} 