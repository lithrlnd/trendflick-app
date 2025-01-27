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
import com.trendflick.data.model.SuggestionItem
import coil.compose.AsyncImage

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
                androidx.compose.material3.Divider(
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
        AsyncImage(
            model = mention.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Column {
            Text(
                text = mention.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${hashtag.tag}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${hashtag.postCount} posts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 
