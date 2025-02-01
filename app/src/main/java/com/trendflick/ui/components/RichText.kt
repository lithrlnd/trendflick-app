package com.trendflick.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.trendflick.data.api.Facet
import com.trendflick.data.api.LinkFeature
import com.trendflick.data.api.MentionFeature
import com.trendflick.data.api.TagFeature
import androidx.compose.ui.graphics.Color

@Composable
fun RichText(
    text: String,
    facets: List<Facet>,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        append(text)

        // Sort facets by start index to process them in order
        val sortedFacets = facets.sortedBy { it.index.start }

        sortedFacets.forEach { facet ->
            val start = facet.index.start
            val end = facet.index.end

            facet.features.forEach { feature ->
                when (feature) {
                    is MentionFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = Color(0xFF6B4EFF),
                                textDecoration = TextDecoration.None
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
                    is TagFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = Color(0xFF6B4EFF),
                                textDecoration = TextDecoration.None
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
                    is LinkFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = Color(0xFF6B4EFF),
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
                }
            }
        }
    }

    ClickableText(
        text = annotatedString,
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
        style = MaterialTheme.typography.bodyLarge.copy(
            color = Color.White
        ),
        modifier = modifier
    )
} 