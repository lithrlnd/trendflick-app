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
import android.util.Log
import java.nio.charset.StandardCharsets

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
        // Convert text to UTF-8 bytes for proper indexing
        val utf8Bytes = text.toByteArray(StandardCharsets.UTF_8)
        val charToByteMap = mutableMapOf<Int, Int>()
        val byteToCharMap = mutableMapOf<Int, Int>()
        
        var charIndex = 0
        var byteIndex = 0
        text.forEach { char ->
            charToByteMap[charIndex] = byteIndex
            byteToCharMap[byteIndex] = charIndex
            byteIndex += char.toString().toByteArray(StandardCharsets.UTF_8).size
            charIndex++
        }

        append(text)

        // Sort facets by byte index for proper processing
        val sortedFacets = facets.sortedWith(compareBy { it.index.start })
            .filter { facet ->
                val startChar = byteToCharMap[facet.index.start]
                val endChar = byteToCharMap[facet.index.end]
                if (startChar == null || endChar == null || 
                    startChar < 0 || endChar > text.length || 
                    startChar >= endChar) {
                    Log.w("RichTextRenderer", "Invalid facet indices: start=$startChar, end=$endChar, text length=${text.length}")
                    false
                } else {
                    true
                }
            }

        // Apply styles to facets
        sortedFacets.forEach { facet ->
            val startChar = byteToCharMap[facet.index.start] ?: return@forEach
            val endChar = byteToCharMap[facet.index.end] ?: return@forEach

            facet.features.forEach { feature ->
                when (feature) {
                    is MentionFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.None
                            ),
                            start = startChar,
                            end = endChar
                        )
                        addStringAnnotation(
                            tag = "mention",
                            annotation = feature.did,
                            start = startChar,
                            end = endChar
                        )
                    }
                    is LinkFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            ),
                            start = startChar,
                            end = endChar
                        )
                        addStringAnnotation(
                            tag = "link",
                            annotation = feature.uri,
                            start = startChar,
                            end = endChar
                        )
                    }
                    is TagFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.None
                            ),
                            start = startChar,
                            end = endChar
                        )
                        addStringAnnotation(
                            tag = "hashtag",
                            annotation = feature.tag,
                            start = startChar,
                            end = endChar
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
