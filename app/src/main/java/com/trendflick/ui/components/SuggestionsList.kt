package com.trendflick.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trendflick.data.model.TrendingHashtag
import com.trendflick.data.model.UserSearchResult
import coil.compose.AsyncImage
import com.trendflick.ui.model.SuggestionItem

@Composable
fun SuggestionsList(
    suggestions: List<SuggestionItem>,
    onSuggestionSelected: (SuggestionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
    ) {
        items(suggestions) { suggestion ->
            when (suggestion) {
                is SuggestionItem.Mention -> MentionSuggestionItem(
                    mention = suggestion,
                    onClick = { onSuggestionSelected(suggestion) }
                )
                is SuggestionItem.Hashtag -> HashtagSuggestionItem(
                    hashtag = suggestion,
                    onClick = { onSuggestionSelected(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun MentionSuggestionItem(
    mention: SuggestionItem.Mention,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            AsyncImage(
                model = mention.avatar,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp)
            )
        },
        headlineContent = { Text("@${mention.handle}") },
        supportingContent = mention.displayName?.let { { Text(it) } }
    )
}

@Composable
private fun HashtagSuggestionItem(
    hashtag: SuggestionItem.Hashtag,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Text(
                "#",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        headlineContent = { Text(hashtag.tag) },
        supportingContent = { Text("${hashtag.postCount} posts") }
    )
} 
