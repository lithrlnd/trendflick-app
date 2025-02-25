# TrendFlick oEmbed Integration

This document outlines the oEmbed integration enhancements made to TrendFlick to improve video playback and embedded content support.

## Overview

TrendFlick now supports comprehensive oEmbed integration for various social media platforms, allowing users to view embedded content from Bluesky, YouTube, Vimeo, Twitter/X, TikTok, Instagram, and more directly within the app.

## Key Components

### 1. VideoPlayer Component

The `VideoPlayer` component has been enhanced to support various types of embedded content:

- **Regular Videos**: Played using ExoPlayer with proper error handling and thumbnail fallbacks
- **oEmbed Content**: Rendered using WebView with platform-specific optimizations
- **Platform Detection**: Automatically detects and optimizes for different platforms (YouTube, Vimeo, Twitter, etc.)
- **URL Processing**: Transforms regular URLs into proper embed URLs for each platform
- **Error Handling**: Provides graceful error states with retry options and external browser fallback

```kotlin
@Composable
fun VideoPlayer(
    videoUrl: String,
    isVisible: Boolean = true,
    onProgressChanged: (Float) -> Unit = {},
    onError: (String) -> Unit = {},
    playbackSpeed: Float = 1f,
    isPaused: Boolean = false,
    thumbnailUrl: String? = null,
    isOEmbedVideo: Boolean = false
)
```

### 2. Enhanced Embed Models

The `ExternalEmbed` model has been extended with new methods to better support oEmbed content:

- **getSocialMediaInfo()**: Detects the social media platform and content type
- **getEmbedType()**: Returns the type of embed (oembed, video, youtube, vimeo, etc.)
- **generateOEmbedUrl()**: Generates the appropriate oEmbed URL for the content

```kotlin
data class ExternalEmbed(
    val uri: String,
    val title: String? = null,
    val description: String? = null,
    val thumb: BlobRef? = null,
    val oEmbedUrl: String? = null,
    val siteName: String? = null,
    val platform: String? = null
) {
    fun getSocialMediaInfo(): SocialMediaInfo? { ... }
    fun getEmbedType(): String { ... }
    fun generateOEmbedUrl(): String? { ... }
}
```

### 3. Embedded Content Components

Several components have been enhanced to better handle embedded content:

- **EmbeddedLink**: Displays link previews with thumbnails and platform badges
- **RecordEmbed**: Handles quoted posts with proper rendering and in-app browser support
- **PostEmbed**: Manages different types of embeds (images, videos, links, records)
- **RichTextPostOverlay**: Displays rich text content when long-pressing on a video

## Platform Support

TrendFlick now supports the following platforms for embedded content:

| Platform | Content Types | Implementation |
|----------|---------------|----------------|
| Bluesky | Posts, Reposts | Native oEmbed via embed.bsky.app |
| YouTube | Videos | Embedded player via youtube.com/embed |
| Vimeo | Videos | Embedded player via player.vimeo.com |
| Twitter/X | Posts, Videos | oEmbed via publish.twitter.com |
| TikTok | Videos | Embedded player via tiktok.com/embed |
| Instagram | Posts | Embedded script via instagram.com/embed.js |
| Facebook | Posts | Thumbnail preview with link |
| LinkedIn | Posts | Thumbnail preview with link |
| Reddit | Posts | Thumbnail preview with link |

## Implementation Details

### URL Processing

The app processes URLs to ensure they're properly formatted for embedding:

```kotlin
// Example: Processing YouTube URLs
val videoId = extractYouTubeVideoId(url)
if (videoId.isNotBlank()) "https://www.youtube.com/embed/$videoId" else url
```

### Thumbnail Generation

Multiple fallback mechanisms for thumbnail generation:

1. Use provided thumbnail if available
2. Generate platform-specific thumbnail (e.g., YouTube thumbnails)
3. Use microlink.io for screenshot-based thumbnails
4. Fall back to domain favicon for other sites

### WebView Configuration

Enhanced WebView configuration for oEmbed content:

```kotlin
settings.javaScriptEnabled = true
settings.mediaPlaybackRequiresUserGesture = false
settings.domStorageEnabled = true
settings.allowContentAccess = true
settings.allowFileAccess = true
settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
```

### Platform-Specific Optimizations

- **Twitter/X**: JavaScript injection to resize iframes
- **Instagram**: JavaScript injection to handle responsive layout
- **YouTube/Vimeo**: Direct embed URL generation with proper video IDs
- **TikTok**: Custom embed URL generation

## User Experience Improvements

- **Rich Text Overlay**: Long-press on videos to view full post text with proper formatting
- **Platform Badges**: Visual indicators for social media platforms
- **Error Handling**: Graceful error states with retry options and external browser fallback
- **In-App Browser**: View embedded content without leaving the app
- **Progress Tracking**: Visual progress indicators for video playback

## Future Enhancements

1. **Caching**: Implement caching for embedded content to improve performance
2. **Offline Support**: Allow viewing cached embedded content offline
3. **Custom Controls**: Platform-specific video controls for better user experience
4. **Analytics**: Track engagement with embedded content
5. **Content Filtering**: Allow users to filter feeds by content type

## Conclusion

These enhancements significantly improve the embedded content experience in TrendFlick, allowing users to seamlessly view and interact with content from various platforms without leaving the app. 