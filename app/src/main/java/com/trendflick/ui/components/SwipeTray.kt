package com.trendflick.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * SwipeTray component that provides quick access to additional features
 * Can be swiped up to reveal and down to hide
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeTray(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    onItemClick: (SwipeTrayItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val draggableState = rememberDraggableState { delta ->
        // Positive delta means dragging down (hide), negative means dragging up (show)
        if (delta > 50 && isVisible) {
            scope.launch {
                onVisibilityChange(false)
            }
        } else if (delta < -50 && !isVisible) {
            scope.launch {
                onVisibilityChange(true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    // If velocity is significant, use it to determine visibility
                    if (velocity.absoluteValue > 300) {
                        onVisibilityChange(velocity < 0)
                    }
                }
            )
    ) {
        // Handle indicator always visible
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(vertical = 8.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        )

        // Tray content
        AnimatedVisibility(
            visible = isVisible,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 300),
                expandFrom = Alignment.Top
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 300),
                shrinkTowards = Alignment.Top
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Tray items in a grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SwipeTrayItem.values().take(4).forEach { item ->
                            SwipeTrayItemButton(
                                item = item,
                                onClick = { onItemClick(item) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SwipeTrayItem.values().drop(4).forEach { item ->
                            SwipeTrayItemButton(
                                item = item,
                                onClick = { onItemClick(item) }
                            )
                        }
                        // Add empty spaces to maintain grid alignment if needed
                        repeat(4 - SwipeTrayItem.values().drop(4).size) {
                            Spacer(modifier = Modifier.width(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeTrayItemButton(
    item: SwipeTrayItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = item.backgroundColor,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = item.label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

enum class SwipeTrayItem(
    val label: String,
    val icon: ImageVector,
    val backgroundColor: Color
) {
    CREATE_POST("Post", Icons.Default.Edit, Color(0xFF6B4EFF)),
    CREATE_FLICK("Flick", Icons.Default.Videocam, Color(0xFF4E8AFF)),
    MESSAGES("Messages", Icons.Default.Chat, Color(0xFF4EFF8A)),
    NOTIFICATIONS("Alerts", Icons.Default.Notifications, Color(0xFFFF4E8A)),
    BOOKMARKS("Bookmarks", Icons.Default.Bookmark, Color(0xFFFF8A4E)),
    SETTINGS("Settings", Icons.Default.Settings, Color(0xFF8A4EFF))
}
