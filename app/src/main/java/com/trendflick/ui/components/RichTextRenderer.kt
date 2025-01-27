package com.trendflick.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.trendflick.data.api.*

@Composable
fun RichTextRenderer(
    text: String,
    facets: List<Facet>,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        append(text)

        // Sort facets by index for proper processing
        val sortedFacets = facets.sortedWith(compareBy { it.index.start })

        // Apply styles to facets
        sortedFacets.forEach { facet ->
            facet.features.forEach { feature ->
                val start = facet.index.start
                val end = facet.index.end
                
                when (feature) {
                    is MentionFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            start = start,
                            end = end
                        )
                        addStringAnnotation(
                            tag = "mention",
                            annotation = feature.did,
                            start = start,
                            end = end
                        )
                    }
                    is LinkFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            ),
                            start = start,
                            end = end
                        )
                        addStringAnnotation(
                            tag = "link",
                            annotation = feature.uri,
                            start = start,
                            end = end
                        )
                    }
                    is TagFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            start = start,
                            end = end
                        )
                        addStringAnnotation(
                            tag = "hashtag",
                            annotation = feature.tag,
                            start = start,
                            end = end
                        )
                    }
                    else -> {
                        // Handle any future feature types that might be added
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            start = start,
                            end = end
                        )
                    }
                }
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(offset, offset)
                .firstOrNull()?.let { annotation ->
                    when (annotation.tag) {
                        "mention" -> onMentionClick(annotation.item)
                        "hashtag" -> onHashtagClick(annotation.item)
                        "link" -> onLinkClick(annotation.item)
                    }
                }
        },
        modifier = modifier
    )
} 
