package com.trendflick.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration

/**
 * Rich text editor component with enhanced hashtag and mention selection
 * This component ensures hashtags and mentions are always selectable and clickable
 */
@Composable
fun RichTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = LocalTextStyle.current.copy(color = Color.White),
    cursorColor: Color = MaterialTheme.colorScheme.primary
) {
    // Create annotated string with highlighted hashtags and mentions
    val annotatedText = buildAnnotatedString {
        val text = value.text
        
        // Start with default style
        withStyle(SpanStyle(color = textStyle.color)) {
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
                    withStyle(SpanStyle(
                        color = Color(0xFF6B4EFF),
                        fontWeight = FontWeight.Bold
                    )) {
                        append(matchText)
                    }
                    pop()
                } else if (matchText.startsWith("@")) {
                    // Mention style
                    pushStringAnnotation("mention", matchText.substring(1))
                    withStyle(SpanStyle(
                        color = Color(0xFF4E8AFF),
                        fontWeight = FontWeight.Bold
                    )) {
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
    
    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(cursorColor),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = Color.Gray)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Rich text display component with enhanced hashtag and mention selection
 * This component ensures hashtags and mentions are always selectable and clickable
 */
@Composable
fun EnhancedRichTextDisplay(
    text: String,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current.copy(color = Color.White)
) {
    val annotatedText = buildAnnotatedString {
        // Start with default style
        withStyle(SpanStyle(color = textStyle.color)) {
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
                    withStyle(SpanStyle(
                        color = Color(0xFF6B4EFF),
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.None
                    )) {
                        append(matchText)
                    }
                    pop()
                } else if (matchText.startsWith("@")) {
                    // Mention style
                    pushStringAnnotation("mention", matchText.substring(1))
                    withStyle(SpanStyle(
                        color = Color(0xFF4E8AFF),
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.None
                    )) {
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
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                // Find annotations at the tap position
                val position = annotatedText.getStringAnnotations(
                    tag = "hashtag",
                    start = 0,
                    end = annotatedText.length
                ).firstOrNull { annotation ->
                    val textLayoutResult = textLayoutResultState.value
                    if (textLayoutResult != null) {
                        val bounds = textLayoutResult.getBoundingBox(annotation.start)
                        val endBounds = textLayoutResult.getBoundingBox(annotation.end)
                        offset.x >= bounds.left && offset.x <= endBounds.right &&
                                offset.y >= bounds.top && offset.y <= bounds.bottom
                    } else {
                        false
                    }
                }
                
                if (position != null) {
                    onHashtagClick(position.item)
                    return@detectTapGestures
                }
                
                // Check for mention annotations
                val mentionPosition = annotatedText.getStringAnnotations(
                    tag = "mention",
                    start = 0,
                    end = annotatedText.length
                ).firstOrNull { annotation ->
                    val textLayoutResult = textLayoutResultState.value
                    if (textLayoutResult != null) {
                        val bounds = textLayoutResult.getBoundingBox(annotation.start)
                        val endBounds = textLayoutResult.getBoundingBox(annotation.end)
                        offset.x >= bounds.left && offset.x <= endBounds.right &&
                                offset.y >= bounds.top && offset.y <= bounds.bottom
                    } else {
                        false
                    }
                }
                
                if (mentionPosition != null) {
                    onMentionClick(mentionPosition.item)
                }
            }
        }
    ) {
        val textLayoutResultState = remember { mutableStateOf<TextLayoutResult?>(null) }
        
        Text(
            text = annotatedText,
            style = textStyle,
            onTextLayout = { textLayoutResultState.value = it },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
