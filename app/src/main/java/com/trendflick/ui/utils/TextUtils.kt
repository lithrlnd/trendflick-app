package com.trendflick.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.trendflick.data.api.Facet
import com.trendflick.ui.components.HashtagBadge

@Composable
fun FlowTextContent(
    text: String,
    facets: List<Facet>,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val words = text.split(" ")
    
    FlowRow(
        modifier = modifier,
        mainAxisSpacing = 4.dp,
        crossAxisSpacing = 4.dp
    ) {
        words.forEach { word ->
            if (word.startsWith("#")) {
                val hashtag = word.substring(1)
                HashtagBadge(
                    hashtag = hashtag,
                    isTrending = false,
                    onClick = { onHashtagClick(hashtag) }
                )
            } else {
                Text(
                    text = "$word ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> String.format("%.1fK", count / 1000.0)
        else -> String.format("%.1fM", count / 1000000.0)
    }
} 