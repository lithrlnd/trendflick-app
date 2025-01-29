# Rich Text Implementation Guide

## Overview
TrendFlick implements the AT Protocol rich text specification using Jetpack Compose's AnnotatedString system. This implementation handles mentions, hashtags, and links while properly managing UTF-8 byte indices as required by the AT Protocol.

## Key Components

### RichTextRenderer
The main component responsible for rendering rich text with proper AT Protocol facet support.

```kotlin
@Composable
fun RichTextRenderer(
    text: String,
    facets: List<Facet>,
    onMentionClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

## Implementation Details

### UTF-8 Byte Handling
The AT Protocol requires facet indices to be based on UTF-8 byte positions. Our implementation:
1. Converts text to UTF-8 bytes
2. Maintains mapping between character and byte indices
3. Validates facet indices before applying styles

### Facet Processing
1. Sorts facets by start index
2. Filters out invalid facets:
   - Null indices
   - Out-of-bounds indices
   - Overlapping facets
3. Applies styles and click handlers for each facet type

### Supported Facet Types
1. **Mentions** (`app.bsky.richtext.facet#mention`)
   - Styled with primary color
   - No text decoration
   - Clickable with DID resolution

2. **Links** (`app.bsky.richtext.facet#link`)
   - Styled with primary color
   - Underline decoration
   - Clickable with URI handling

3. **Hashtags** (`app.bsky.richtext.facet#tag`)
   - Styled with primary color
   - No text decoration
   - Clickable with tag handling

## Error Handling
- Invalid facets are filtered out
- Index validation prevents out-of-bounds errors
- Comprehensive logging for debugging
- Graceful fallback to plain text

## Usage Example

```kotlin
RichTextRenderer(
    text = "Hello @user #trending https://example.com",
    facets = listOf(
        Facet(
            index = Index(byteStart = 6, byteEnd = 11),
            features = listOf(MentionFeature(did = "user_did"))
        ),
        // ... more facets
    ),
    onMentionClick = { did -> /* Handle mention click */ },
    onHashtagClick = { tag -> /* Handle hashtag click */ },
    onLinkClick = { uri -> /* Handle link click */ }
)
```

## Best Practices
1. Always validate facet indices before rendering
2. Handle UTF-8 encoding properly
3. Maintain proper error logging
4. Test with various character encodings
5. Follow AT Protocol specifications for facet handling

## Common Issues and Solutions
1. **Invalid Indices**
   - Problem: Facet indices don't match text
   - Solution: Validate and filter invalid facets

2. **Overlapping Facets**
   - Problem: Multiple facets targeting same text
   - Solution: Sort and handle conflicts

3. **UTF-8 Encoding**
   - Problem: Incorrect byte positions
   - Solution: Proper UTF-8 conversion and mapping

## Testing
Test cases should cover:
1. ASCII text
2. Unicode characters
3. Emoji
4. Invalid facets
5. Overlapping facets
6. Empty text/facets
7. Click handling
8. Style application 