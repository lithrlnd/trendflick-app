package com.trendflick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
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
        
        // Pre-calculate character byte lengths
        val charByteLengths = text.map { it.toString().toByteArray(StandardCharsets.UTF_8).size }
        
        // Build mapping
        text.forEachIndexed { index, _ ->
            byteToCharMap[bytePos] = charPos
            bytePos += charByteLengths[index]
            charPos++
        }
        byteToCharMap[bytePos] = charPos

        // Sort and validate facets
        val sortedFacets = facets
            .sortedBy { it.index.start }
            .filter { facet ->
                val startChar = byteToCharMap[facet.index.start]
                val endChar = byteToCharMap[facet.index.end]
                startChar != null && endChar != null && 
                startChar < endChar && 
                endChar <= text.length
            }

        var lastEnd = -1
        sortedFacets.forEach { facet ->
            val startChar = byteToCharMap[facet.index.start] ?: return@forEach
            val endChar = byteToCharMap[facet.index.end] ?: return@forEach

            if (startChar < lastEnd) return@forEach
            lastEnd = endChar

            facet.features.forEach { feature ->
                when (feature) {
                    is TagFeature -> {
                        // Enhanced hashtag styling
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
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
                    is MentionFeature -> {
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
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
                                textDecoration = TextDecoration.Underline,
                                letterSpacing = 0.5.sp
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
                }
            }
        }
    }

    var pressedOffset by remember { mutableStateOf<Int?>(null) }
    
    Box(modifier = modifier) {
        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            pressedOffset = offset.x.toInt()
                            tryAwaitRelease()
                            pressedOffset = null
                        }
                    )
                },
            onClick = { offset ->
                annotatedString.getStringAnnotations(offset, offset)
                    .firstOrNull()?.let { annotation ->
                        when (annotation.tag) {
                            "hashtag" -> onHashtagClick(annotation.item)
                            "mention" -> onMentionClick(annotation.item)
                            "link" -> onLinkClick(annotation.item)
                        }
                    }
            }
        )

        // Visual feedback for interaction
        pressedOffset?.let { offset ->
            annotatedString.getStringAnnotations(offset, offset)
                .firstOrNull()?.let { annotation ->
                    when (annotation.tag) {
                        "hashtag", "mention", "link" -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(0.1f),
                                color = MaterialTheme.colorScheme.primary
                            ) {}
                        }
                    }
                }
        }
    }
} 
