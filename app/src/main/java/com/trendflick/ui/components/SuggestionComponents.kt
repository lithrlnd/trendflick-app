package com.trendflick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NumberSign
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trendflick.data.model.UserSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Component for displaying hashtag suggestions
 */
@Composable
fun HashtagSuggestionList(
    query: String,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Simulate loading suggestions from a repository
    LaunchedEffect(query) {
        isLoading = true
        delay(300) // Simulate network delay
        
        // Generate mock suggestions based on query
        // In a real app, this would come from a repository
        suggestions = listOf(
            "${query}trending",
            "${query}viral",
            "${query}challenge",
            "${query}2025",
            "${query}official"
        ).filter { it.length > 2 }
        
        isLoading = false
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 8.dp
    ) {
        Box {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF6B4EFF),
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (suggestions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hashtags found",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { suggestion ->
                        HashtagSuggestionItem(
                            hashtag = suggestion,
                            onClick = {
                                coroutineScope.launch {
                                    onSuggestionClick(suggestion)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual hashtag suggestion item
 */
@Composable
fun HashtagSuggestionItem(
    hashtag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF6B4EFF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NumberSign,
                contentDescription = null,
                tint = Color(0xFF6B4EFF)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = "#$hashtag",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Trending hashtag",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Component for displaying user mention suggestions
 */
@Composable
fun MentionSuggestionList(
    query: String,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf<List<UserSuggestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Simulate loading suggestions from a repository
    LaunchedEffect(query) {
        isLoading = true
        delay(300) // Simulate network delay
        
        // Generate mock suggestions based on query
        // In a real app, this would come from a repository
        suggestions = listOf(
            UserSuggestion("${query.capitalize()} User", "${query.lowercase()}user", null),
            UserSuggestion("${query.capitalize()} Official", "${query.lowercase()}_official", null),
            UserSuggestion("${query.capitalize()} Creator", "real${query.lowercase()}", null),
            UserSuggestion("${query.capitalize()} Fan", "${query.lowercase()}_fan", null)
        )
        
        isLoading = false
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 8.dp
    ) {
        Box {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4E8AFF),
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (suggestions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No users found",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { suggestion ->
                        MentionSuggestionItem(
                            user = suggestion,
                            onClick = {
                                coroutineScope.launch {
                                    onSuggestionClick(suggestion.handle)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual user mention suggestion item
 */
@Composable
fun MentionSuggestionItem(
    user: UserSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF4E8AFF).copy(alpha = 0.2f))
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "User avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF4E8AFF),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.Center)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = user.username,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "@${user.handle}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Extension function to capitalize the first letter of a string
 */
private fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this[0].uppercase() + this.substring(1)
    } else {
        this
    }
}
