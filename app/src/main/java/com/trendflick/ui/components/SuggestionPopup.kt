package com.trendflick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Surface
import com.trendflick.ui.model.SuggestionItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.layout.ContentScale

@Composable
fun SuggestionPopup(
    suggestions: List<SuggestionItem>,
    onSuggestionSelected: (SuggestionItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = suggestions.isNotEmpty(),
        onDismissRequest = onDismiss,
        modifier = modifier
            .width(300.dp)
            .heightIn(max = 300.dp)
    ) {
        LazyColumn {
            items(suggestions) { suggestion ->
                when (suggestion) {
                    is SuggestionItem.Mention -> MentionItem(
                        mention = suggestion,
                        onClick = { onSuggestionSelected(suggestion) }
                    )
                    is SuggestionItem.Hashtag -> HashtagItem(
                        hashtag = suggestion,
                        onClick = { onSuggestionSelected(suggestion) }
                    )
                }
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun MentionItem(
    mention: SuggestionItem.Mention,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        mention.avatar?.let { avatarUrl ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Column {
            mention.displayName?.let { displayName ->
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "@${mention.handle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HashtagItem(
    hashtag: SuggestionItem.Hashtag,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${hashtag.tag}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                hashtag.postCount?.let { count ->
                    if (count > 0) {
                        Text(
                            text = "${count} posts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Add a small indicator if the tag is trending
            hashtag.postCount?.let { count ->
                if (count > 1000) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Trending",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
} 
