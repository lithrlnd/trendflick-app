package com.trendflick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun ColorSchemeSelector(
    selectedHue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableStateOf(0f) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Color Scheme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .size(250.dp)
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragPosition = offset
                                val angle = calculateAngle(center, offset)
                                onHueChange(angle)
                            },
                            onDrag = { change, _ ->
                                dragPosition = change.position
                                val angle = calculateAngle(center, change.position)
                                onHueChange(angle)
                            },
                            onDragEnd = {
                                dragPosition = null
                            }
                        )
                    }
            ) {
                center = Offset(size.width / 2f, size.height / 2f)
                radius = minOf(size.width, size.height) / 2f - 16f

                // Draw color wheel
                for (angle in 0..360 step 2) {
                    val color = Color.hsv(angle.toFloat(), 1f, 1f)
                    drawArc(
                        color = color,
                        startAngle = angle.toFloat() - 1,
                        sweepAngle = 2f,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }

                // Draw selector
                val selectorAngle = selectedHue
                val selectorRadius = radius + 8f
                val selectorPosition = Offset(
                    x = center.x + cos((selectorAngle - 90) * PI / 180f).toFloat() * selectorRadius,
                    y = center.y + sin((selectorAngle - 90) * PI / 180f).toFloat() * selectorRadius
                )

                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = selectorPosition,
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
                
                drawCircle(
                    color = Color.hsv(selectedHue, 1f, 1f),
                    radius = 8f,
                    center = selectorPosition
                )
            }
        }
    }
}

private fun calculateAngle(center: Offset, position: Offset): Float {
    val angle = atan2(
        position.y - center.y,
        position.x - center.x
    ) * 180f / PI.toFloat()
    return (angle + 90f).mod(360f)
} 