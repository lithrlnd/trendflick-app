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
        append(text)

        // Create byte-to-character position mapping
        val utf8Bytes = text.toByteArray(StandardCharsets.UTF_8)
        val byteToCharMap = mutableMapOf<Int, Int>()
        var charPos = 0
        var bytePos = 0
        
        // Pre-calculate all character byte lengths
        val charByteLengths = text.map { it.toString().toByteArray(StandardCharsets.UTF_8).size }
        
        // Build the mapping
        text.forEachIndexed { index, _ ->
            byteToCharMap[bytePos] = charPos
            bytePos += charByteLengths[index]
            charPos++
        }
        // Add final position
        byteToCharMap[bytePos] = charPos

        // Debug logging for facet ranges
        facets.forEach { facet ->
            Log.d("RichTextRenderer", "Facet range: ${facet.index.start}-${facet.index.end}, " +
                "Text: '${text.substring(
                    byteToCharMap[facet.index.start] ?: 0,
                    byteToCharMap[facet.index.end] ?: text.length
                )}'")
        }

        // Sort and validate facets
        val sortedFacets = facets
            .sortedBy { it.index.start }
            .filter { facet ->
                val startChar = byteToCharMap[facet.index.start]
                val endChar = byteToCharMap[facet.index.end]
                if (startChar == null || endChar == null || 
                    startChar < 0 || endChar > text.length || 
                    startChar >= endChar) {
                    Log.w("RichTextRenderer", 
                        "Invalid facet range: start=$startChar, end=$endChar, " +
                        "byteStart=${facet.index.start}, byteEnd=${facet.index.end}, " +
                        "textLength=${text.length}")
                    false
                } else {
                    true
                }
            }

        // Track processed ranges to avoid overlaps
        var lastEnd = -1

        sortedFacets.forEach { facet ->
            val startChar = byteToCharMap[facet.index.start] ?: return@forEach
            val endChar = byteToCharMap[facet.index.end] ?: return@forEach

            // Skip if this facet overlaps with a previous one
            if (startChar < lastEnd) {
                Log.w("RichTextRenderer", "Skipping overlapping facet at position $startChar")
                return@forEach
            }

            lastEnd = endChar

            // Extract the actual text being decorated
            val facetText = text.substring(startChar, endChar)
            Log.d("RichTextRenderer", "Applying facet to text: '$facetText'")

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
                        // Make sure hashtag includes the # symbol
                        val tagStart = if (facetText.startsWith("#")) startChar else startChar - 1
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.None
                            ),
                            start = tagStart,
                            end = endChar
                        )
                        addStringAnnotation(
                            tag = "hashtag",
                            annotation = feature.tag,
                            start = tagStart,
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
