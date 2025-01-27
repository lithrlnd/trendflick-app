package com.trendflick.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Comment
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun RepostMenu(
    onRepost: () -> Unit,
    onQuotePost: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { showMenu = true }
        ) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = "Repost"
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.width(200.dp)
        ) {
            DropdownMenuItem(
                text = { 
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Repost")
                    }
                },
                onClick = {
                    onRepost()
                    showMenu = false
                }
            )
            
            DropdownMenuItem(
                text = { 
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Quote Post")
                    }
                },
                onClick = {
                    onQuotePost()
                    showMenu = false
                }
            )
        }
    }
} 