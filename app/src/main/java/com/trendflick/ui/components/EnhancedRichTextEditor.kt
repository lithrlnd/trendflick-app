package com.trendflick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced rich text editor with hashtag and mention highlighting and clickable functionality
 */
@Composable
fun EnhancedRichTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {}
) {
    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 16.sp
    )
    
    // Create annotated string with highlighted hashtags and mentions
    val annotatedText = buildAnnotatedString {
        val text = value.text
        
        // Start with default style
        withStyle(SpanStyle(color = Color.White)) {
            var lastIndex = 0
            
            // Regex to find hashtags and mentions
            val regex = Regex("""(#\w+)|(@\w+)""")
            val matches = regex.findAll(text)
            
            for (match in matches) {
                // Add text before the match
                append(text.substring(lastIndex, match.range.first))
                
                // Add the match with appropriate style
                val matchText = match.value
                if (matchText.startsWith("#")) {
                    // Hashtag style
                    pushStringAnnotation("hashtag", matchText.substring(1))
                    withStyle(SpanStyle(color = Color(0xFF6B4EFF), fontWeight = FontWeight.Bold)) {
                        append(matchText)
                    }
                    pop()
                } else if (matchText.startsWith("@")) {
                    // Mention style
                    pushStringAnnotation("mention", matchText.substring(1))
                    withStyle(SpanStyle(color = Color(0xFF4E8AFF), fontWeight = FontWeight.Bold)) {
                        append(matchText)
                    }
                    pop()
                }
                
                lastIndex = match.range.last + 1
            }
            
            // Add remaining text
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            cursorBrush = SolidColor(Color(0xFF6B4EFF)),
            keyboardOptions = keyboardOptions,
            decorationBox = { innerTextField ->
                Box {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    
                    ClickableText(
                        text = annotatedText,
                        style = textStyle,
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(
                                tag = "hashtag",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let { annotation ->
                                onHashtagClick(annotation.item)
                            }
                            
                            annotatedText.getStringAnnotations(
                                tag = "mention",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let { annotation ->
                                onMentionClick(annotation.item)
                            }
                        }
                    )
                    
                    // This is a hidden text field that handles the actual input
                    // We need this because ClickableText is not editable
                    Box(modifier = Modifier.fillMaxWidth()) {
                        innerTextField()
                    }
                }
            }
        )
    }
}

/**
 * Hashtag pill component for displaying clickable hashtags
 */
@Composable
fun HashtagPill(
    hashtag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF6B4EFF).copy(alpha = 0.2f)
    ) {
        Text(
            text = "#$hashtag",
            color = Color(0xFF6B4EFF),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Mention pill component for displaying clickable user mentions
 */
@Composable
fun MentionPill(
    username: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF4E8AFF).copy(alpha = 0.2f)
    ) {
        Text(
            text = "@$username",
            color = Color(0xFF4E8AFF),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Rich text display component for showing formatted text with clickable hashtags and mentions
 */
@Composable
fun RichTextDisplay(
    text: String,
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val annotatedText = buildAnnotatedString {
        // Regex to find hashtags and mentions
        val regex = Regex("""(#\w+)|(@\w+)""")
        val matches = regex.findAll(text)
        
        var lastIndex = 0
        
        for (match in matches) {
            // Add text before the match
            append(text.substring(lastIndex, match.range.first))
            
            // Add the match with appropriate style
            val matchText = match.value
            if (matchText.startsWith("#")) {
                // Hashtag style
                pushStringAnnotation("hashtag", matchText.substring(1))
                withStyle(SpanStyle(color = Color(0xFF6B4EFF), fontWeight = FontWeight.Bold)) {
                    append(matchText)
                }
                pop()
            } else if (matchText.startsWith("@")) {
                // Mention style
                pushStringAnnotation("mention", matchText.substring(1))
                withStyle(SpanStyle(color = Color(0xFF4E8AFF), fontWeight = FontWeight.Bold)) {
                    append(matchText)
                }
                pop()
            }
            
            lastIndex = match.range.last + 1
        }
        
        // Add remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    
    ClickableText(
        text = annotatedText,
        style = TextStyle(
            color = Color.White,
            fontSize = 16.sp
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedText.getStringAnnotations(
                tag = "hashtag",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                onHashtagClick(annotation.item)
            }
            
            annotatedText.getStringAnnotations(
                tag = "mention",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                onMentionClick(annotation.item)
            }
        }
    )
}
