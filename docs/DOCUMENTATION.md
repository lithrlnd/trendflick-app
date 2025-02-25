# TrendFlick Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Setup Guide](#setup-guide)
3. [Core Components](#core-components)
4. [Navigation System](#navigation-system)
5. [AT Protocol Integration](#at-protocol-integration)
6. [Embedded Content Components](#embedded-content-components)
7. [Development Guidelines](#development-guidelines)
8. [Security Considerations](#security-considerations)
9. [Troubleshooting](#troubleshooting)

## Architecture Overview

### MVVM Architecture
TrendFlick follows the MVVM (Model-View-ViewModel) architecture pattern:
- **Model**: Data layer containing repositories and data sources
- **View**: Jetpack Compose UI components
- **ViewModel**: Business logic and state management

### Key Components
```
app/
├── data/
│   ├── api/         # AT Protocol API interfaces
│   ├── auth/        # Authentication management
│   ├── local/       # Local storage (Room DB)
│   ├── model/       # Data models
│   └── repository/  # Repository implementations
├── di/              # Hilt dependency injection
├── ui/
│   ├── components/  # Reusable UI components
│   ├── screens/     # Screen implementations
│   └── theme/       # App theming
└── utils/           # Utility classes
```

## Setup Guide

### Prerequisites
1. Android Studio Arctic Fox or newer
2. JDK 11 or newer
3. Firebase project
4. BlueSky API credentials

### Configuration Steps
1. Clone the repository
2. Create `secrets.properties` file with:
   ```properties
   BLUESKY_API_KEY=your_api_key
   FIREBASE_CONFIG=your_config
   ```
3. Set up Firebase:
   - Enable Authentication
   - Set up Realtime Database
   - Configure Storage with `/videos` directory

### Build Configuration
- Minimum SDK: 24
- Target SDK: Latest stable
- Kotlin version: 1.8.x
- Compose version: Latest stable

## Core Components

### Screen Mirroring
- **Overview**
  TrendFlick implements native Android screen mirroring to enable seamless content sharing on Chromecast and compatible devices.

- **Implementation Details**
  ```kotlin
  @Composable
  fun CastButton(
      modifier: Modifier = Modifier,
      tint: Color = Color.White
  )
  ```

  Key Features:
  - Global cast button in app's top bar
  - Uses Android's built-in screen mirroring
  - No additional permissions required
  - Works in both portrait and landscape
  - Maintains audio playback in all orientations
  - Compatible with all Chromecast devices

- **Integration Points**
  - Integrated in MainActivity for app-wide access
  - Positioned above all other UI elements
  - Transparent background for visual consistency
  - Maintains functionality across all screens

- **Best Practices**
  1. Keep cast button accessible at all times
  2. Maintain proper z-ordering for visibility
  3. Use transparent backgrounds to not interfere with content
  4. Follow Android's native casting patterns
  5. Handle orientation changes gracefully

- **Usage Guidelines**
  ```kotlin
  // Add to top-level UI
  TopAppBar(
      title = { },
      actions = {
          CastButton(
              modifier = Modifier.size(48.dp),
              tint = Color.White
          )
      },
      colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent
      )
  )
  ```

- **Common Issues**
  - Handle proper z-ordering in layouts
  - Manage visibility across different screens
  - Ensure proper padding with system bars
  - Handle casting state changes gracefully

### Authentication
- Handles BlueSky authentication
- Manages session tokens
- Implements refresh token logic
- Stores credentials securely

### Feed Management
- Implements multiple feed types:
  - **Trends Feed**
    - Uses "whats-hot" algorithm
    - Fetches 50 posts per load for comprehensive trending content
    - Optimized for discovery and engagement
  
  - **Following Feed**
    - Uses "reverse-chronological" algorithm
    - Fetches 25 posts per load for better reliability
    - Optimized for timeline consistency
  
  - **Implementation Details**
    - Dynamic feed parameter selection based on feed type
    - Smart post validation and filtering
    - Proper timestamp handling per Bluesky specs
    - Efficient cursor-based pagination
    - Comprehensive logging for debugging

- Handles pagination with cursor-based loading
- Manages feed caching with state preservation
- Implements custom filtering with post validation
- Supports dynamic algorithm selection
- Maintains proper timestamp ordering

### Media Handling
- Supports video/image uploads
- Manages blob references
- Handles media compression
- Implements caching strategy

### Social Features
- Like/Unlike functionality
- Reply System:
  - **Core Functionality**
    - Nested reply support
    - Thread hierarchy maintenance
    - Character limit (300) enforcement
    - Rich text processing (@mentions, #hashtags)
    - Author-only filtering
    - Real-time comment refresh
    - Proper thread state management
    
  - **UI Components**
    - Visual thread indentation
    - OP (Original Poster) highlighting
    - Character counter with color feedback
    - Reply input field with validation
    - Author filter toggle switch
    - Refresh button for comments
    - Engagement column with like/comment/repost/share actions
    
  - **AT Protocol Compliance**
    - Strong references (URI + CID)
    - Parent/root reference tracking
    - Proper facet processing
    - UTF-8 text handling
    - Byte-based facet indexing
    - Rich text rendering with proper styling
    
  - **Error Handling**
    - Network failure recovery
    - Rate limit management
    - Input validation
    - State management
    - Loading state indicators
    - Error state with retry options

## Embedded Content Components

### RecordEmbed Component
- **Overview**  
  The RecordEmbed component displays quoted posts (reposts) within the feed, handling both successful and error states gracefully.

- **Implementation Details**
  ```kotlin
  @Composable
  private fun RecordEmbed(
      uri: String,
      cid: String,
      onImageClick: (ImageEmbed) -> Unit,
      onHashtagClick: ((String) -> Unit)?,
      onLinkClick: ((String) -> Unit)?,
      onProfileClick: (() -> Unit)? = null,
      modifier: Modifier = Modifier
  )
  ```

  **Key Features:**
  - Fetches and displays quoted posts using AtProtocolRepository
  - Extracts handle and post ID from URI for better display
  - Converts AT Protocol URIs to web URLs for improved handling
  - Implements in-app browser using WebView within AlertDialog
  - Provides graceful error handling for unavailable posts
  - Includes retry mechanism for temporary network issues
  - Generates thumbnails using multiple fallback mechanisms
  - Supports microlink.io API integration for screenshot-based thumbnails

  **States:**
  1. **Loading State**: Shows a circular progress indicator
  2. **Error State**: Displays "This post is unavailable" with retry button
  3. **Success State**: Renders the quoted post with author info, content, and embeds
  4. **Placeholder State**: Shows enhanced placeholder with avatar and thumbnail when post is loading

  **URI Handling:**
  ```kotlin
  // Convert AT URI to web URL
  val webUrl = if (uri.startsWith("at://")) {
      val didPart = uri.substringAfter("at://").substringBefore("/")
      val collection = uri.substringAfter("$didPart/").substringBefore("/")
      val rkey = uri.substringAfterLast("/")
      "https://bsky.app/profile/$handle/post/$rkey"
  } else {
      uri
  }
  ```

- **Usage Guidelines**
  ```kotlin
  // Example usage in PostEmbed component
  if (embed.type == "app.bsky.embed.record" || embed.record != null) {
      val recordUri = embed.record?.uri ?: ""
      val recordCid = embed.record?.cid ?: ""
      
      if (recordUri.isNotEmpty()) {
          RecordEmbed(
              uri = recordUri,
              cid = recordCid,
              onImageClick = onImageClick,
              onHashtagClick = onHashtagClick,
              onLinkClick = onLinkClick,
              onProfileClick = onProfileClick,
              modifier = modifier
          )
      } else {
          // Fallback for when we can't extract the URI/CID
          Text(
              text = "Unable to display quoted post",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp)
          )
      }
  }
  ```

- **Best Practices**
  1. Always validate URI and CID before passing to RecordEmbed
  2. Handle all states (loading, error, success) appropriately
  3. Provide proper click handlers for interactive elements
  4. Use consistent styling with the rest of the application
  5. Implement proper error logging for debugging

- **Common Issues**
  - Handle HTTP 400 errors for deleted posts
  - Manage proper thumbnail generation for various content types
  - Ensure proper WebView configuration for in-app browser
  - Handle orientation changes during WebView display

### EmbeddedLink Component
- **Overview**  
  The EmbeddedLink component displays rich link previews for external URLs shared in posts.

- **Implementation Details**
  ```kotlin
  @Composable
  private fun EmbeddedLink(
      title: String,
      description: String?,
      thumbnail: ExternalEmbed,
      url: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier
  )
  ```

  **Key Features:**
  - Displays link previews with title, description, and thumbnail
  - Supports large header images for important links
  - Implements multiple thumbnail fallback mechanisms
  - Special handling for social media platforms (YouTube, Twitter/X, Instagram, TikTok, Facebook)
  - Extracts and displays domain name from URL
  - Uses borders for improved visibility
  - Implements conditional layout based on content type
  - Integrates with microlink.io API for screenshot-based thumbnails

  **Thumbnail Generation:**
  ```kotlin
  // Enhanced thumbnail URL generation with multiple fallbacks
  val thumbnailUrl = thumbnail.thumb?.link?.let { link ->
      if (link.startsWith("http")) {
          link
      } else {
          "https://cdn.bsky.app/img/feed_thumbnail/plain/$link@jpeg"
      }
  } ?: run {
      // Fallback mechanisms when thumb link is null
      val uri = Uri.parse(url)
      val host = uri.host
      
      when {
          // YouTube thumbnails
          url.contains("youtube.com") || url.contains("youtu.be") -> {
              val videoId = extractYouTubeVideoId(url)
              if (videoId.isNotBlank()) {
                  "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
              } else {
                  ""
              }
          }
          // Twitter/X thumbnails via microlink
          url.contains("twitter.com") || url.contains("x.com") -> {
              val encodedUrl = Uri.encode(url)
              "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
          }
          // Common domains with known thumbnail patterns
          url.contains("instagram.com") || 
          url.contains("tiktok.com") ||
          url.contains("facebook.com") ||
          url.contains("kingdomsandemo.com") -> {
              val encodedUrl = Uri.encode(url)
              "https://api.microlink.io/?url=$encodedUrl&screenshot=true&meta=false&embed=screenshot.url"
          }
          // Fallback to domain favicon for other sites
          !host.isNullOrBlank() -> {
              "https://www.google.com/s2/favicons?domain=$host&sz=128"
          }
          else -> ""
      }
  }
  ```

- **Usage Guidelines**
  ```kotlin
  // Example usage in PostEmbed component
  if (embed.external != null) {
      EmbeddedLink(
          title = embed.external.title ?: "Untitled",
          description = embed.external.description,
          thumbnail = embed.external,
          url = embed.external.uri,
          onClick = { 
              onLinkClick?.invoke(embed.external.uri) ?: run {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(embed.external.uri))
                  context.startActivity(intent)
              }
          },
          modifier = modifier
      )
  }
  ```

- **Best Practices**
  1. Provide proper error handling for thumbnail loading
  2. Implement consistent styling with the rest of the application
  3. Handle long titles and descriptions with proper truncation
  4. Use appropriate content scaling for thumbnails
  5. Implement proper click handling for the entire card

- **Common Issues**
  - Handle missing thumbnails gracefully
  - Ensure proper URL parsing and validation
  - Manage proper thumbnail aspect ratios
  - Handle orientation changes during thumbnail loading

### Video Feed Implementation
- **Comment System**
  - Unified comment system across posts and videos
  - Consistent engagement actions (like, comment, repost, share)
  - Author filtering capability
  - Rich text support in comments
  - Proper state management for comments overlay
  - Smooth animations and transitions
  
- **Engagement Features**
  - Adaptive layout based on orientation:
    - Portrait: Vertical column on right edge
    - Landscape: Horizontal row aligned at top-right
  - Haptic feedback on actions
  - Proper state persistence
  - Consistent styling with main feed
  - Proper count formatting (K, M suffixes)
  
- **Orientation-Specific Behavior**
  1. **Portrait Mode**
     - Vertical scrolling feed
     - Vertical engagement column on right
     - Bottom-aligned author info
     - Full-screen video/image display

  2. **Landscape Mode**
     - Horizontal scrolling feed with TikTok-style smooth transitions
     - Horizontal engagement row at top-right
     - Bottom-left aligned author info
     - Optimized layout for wider screens
     - Spring-based animation for smooth scrolling:
       ```kotlin
       flingBehavior = PagerDefaults.flingBehavior(
           state = pagerState,
           snapAnimationSpec = spring(
               dampingRatio = Spring.DampingRatioMediumBouncy,
               stiffness = Spring.StiffnessLow
           )
       )
       ```

- **UI/UX Considerations**
  - Bottom sheet comments overlay
  - Smooth animations
  - Proper loading states
  - Error handling with retry options
  - Empty state handling
  - Proper navigation handling
  - Rich Text Overlay:
    - Long-press activation for better discoverability
    - Haptic feedback for enhanced interaction
    - Smooth fade transitions
    - Semi-transparent background for context
    - Proper text contrast and readability
    - Easy dismissal with tap gesture
    - Consistent styling with app theme
    - Proper handling of different text lengths
    - Support for AT Protocol rich text features

### Orientation-Specific Layout Implementation

#### Landscape Mode Behavior
- **Media Content Layout**
  - Left panel takes 60% of screen width for media content
  - Media properly scaled and centered
  - Maintains aspect ratio for different content types
  - Supports videos, images, and link previews
  - Proper error state handling

- **Content Types Handling**
  1. **Posts with Media**
     ```kotlin
     Row(modifier = Modifier.fillMaxSize()) {
         // Left side: Media (60% width)
         Box(modifier = Modifier.weight(0.6f))
         // Right side: Content (40% width)
         Column(modifier = Modifier.weight(0.4f))
     }
     ```
     - Videos maintain 16:9 aspect ratio
     - Images scale appropriately
     - Link previews centered in media panel
     - Proper error state handling

  2. **Text-Only Posts**
     - Uses full width of screen
     - Engagement actions at top
     - Content flows naturally below
     - Proper spacing and alignment

- **Engagement Actions**
  - Horizontal layout in landscape mode
  - Aligned to top-right of content
  - Maintains consistent styling
  - Proper spacing between actions

- **Content Organization**
  - Author info below engagement actions
  - Rich text content with proper padding
  - External links rendered appropriately
  - Proper spacing between elements

#### Portrait Mode Behavior
- Vertical scrolling feed
- FAB visible for post creation
- Engagement actions in vertical column
- Full-width media content
- Bottom-aligned author info

#### Implementation Guidelines
1. **Media Handling**
   ```kotlin
   Box(
       modifier = Modifier
           .fillMaxSize()
           .aspectRatio(16f/9f)
           .align(Alignment.Center)
   )
   ```
   - Use proper aspect ratios
   - Center content in container
   - Handle different media types
   - Implement error states

2. **Layout Switching**
   ```kotlin
   val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
   if (isLandscape) {
       // Landscape layout
   } else {
       // Portrait layout
   }
   ```
   - Check orientation using LocalConfiguration
   - Apply appropriate layout
   - Handle transitions smoothly
   - Maintain state during rotation

3. **Best Practices**
   - Always maintain aspect ratios
   - Center content appropriately
   - Handle all media types
   - Implement proper error states
   - Consider different screen sizes
   - Test on various devices
   - Handle edge cases gracefully

4. **Common Issues**
   - Content scaling in landscape
   - Media aspect ratio maintenance
   - Engagement action positioning
   - State preservation during rotation
   - Error state handling
   - Memory management for media

5. **Testing Guidelines**
   - Test on different screen sizes
   - Verify media scaling
   - Check engagement action placement
   - Validate error states
   - Test orientation changes
   - Verify state preservation

#### Usage Example
```kotlin
// Check orientation
val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

// Apply appropriate layout
when {
    isLandscape && hasMedia -> {
        // Split layout with media
        Row(modifier = Modifier.fillMaxSize()) {
            // Media content (60%)
            Box(modifier = Modifier.weight(0.6f))
            // Post content (40%)
            Column(modifier = Modifier.weight(0.4f))
        }
    }
    isLandscape -> {
        // Full-width layout for text-only posts
        Column(modifier = Modifier.fillMaxSize())
    }
    else -> {
        // Portrait layout
        Box(modifier = Modifier.fillMaxSize())
    }
}
```

#### Performance Considerations
- Proper image loading and caching
- Efficient layout recomposition
- Memory management for media
- Smooth orientation transitions
- State preservation
- Error handling and recovery

## Navigation System

### Customizable Bottom Navigation
The app implements a native-like customizable bottom navigation system with the following features:

#### Core Components
1. **Bottom Navigation Bar**
   ```kotlin
   data class NavItem(
       val screen: Screen,
       val label: String,
       val selectedIcon: ImageVector,
       val unselectedIcon: ImageVector,
       val showBadge: Boolean = false,
       val isLocked: Boolean = false,
       val isDraggable: Boolean = true
   )
   ```
   - Supports up to 7 navigation items
   - Horizontal scrolling for extra items
   - Home item always locked
   - Slide-up gesture for customization

2. **Category System**
   ```kotlin
   data class CustomCategory(
       val id: String,
       val icon: ImageVector,
       val label: String,
       val type: CategoryType,
       description: String,
       onClick: () -> Unit
   )
   ```
   - Organized by type (Feed Types, Content, Organization)
   - Drag-and-drop support
   - Smart filtering of used/available items

#### Edit Mode Features
1. **Wiggle Animation**
   - Native-like wiggle effect
   - Smooth rotation and scaling
   - Haptic feedback integration
   - Small remove buttons (X) in iOS/Android style

2. **Item Management**
   - Long press to enter edit mode
   - Drag and drop between sections
   - Remove items with X button
   - Restore removed navigation items
   - Smart category filtering

#### Implementation Details
1. **State Management**
   ```kotlin
   // Track different item types
   var currentNavItems: List<NavItem>
   var removedInitialItems: List<NavItem>
   var availableCategories: List<CustomCategory>
   ```
   - Maintains separate lists for current, removed, and available items
   - Smart filtering to prevent duplicates
   - Proper state preservation

2. **Animation System**
   ```kotlin
   // Animation specifications
   val rotation = animateFloatAsState(
       targetValue = if (isEditMode) 1.5f else 0f,
       animationSpec = infiniteRepeatable(
           animation = tween(250),
           repeatMode = RepeatMode.Reverse
       )
   )
   ```
   - Smooth transitions
   - Spring-based scaling
   - Color transitions
   - Content size animations

3. **Interaction Handling**
   - Haptic feedback for all interactions
   - Gesture detection for drag and drop
   - Smart position tracking
   - Error prevention

#### Bottom Sheet Implementation
1. **Sheet Content**
   - Current Navigation section
   - Available Items section
   - Smart grouping of items
   - Smooth animations

2. **Drag and Drop**
   - Native-like drag preview
   - Position-based validation
   - Smart category conversion
   - Proper state updates

#### Best Practices
1. **Performance**
   - Efficient recomposition
   - Smart state management
   - Proper animation cleanup
   - Memory leak prevention

2. **User Experience**
   - Consistent haptic feedback
   - Smooth animations
   - Error prevention
   - Clear visual feedback

3. **State Preservation**
   - Proper state restoration
   - Configuration change handling
   - Error recovery
   - Data consistency

#### Usage Example
```kotlin
NavigationBar {
    currentNavItems.forEach { item ->
        NavigationItemWithWiggle(
            item = item,
            isSelected = currentRoute == item.screen.route,
            isEditMode = isEditMode,
            onItemClick = { /* Handle click */ },
            onLongPress = { /* Enter edit mode */ },
            onRemove = { /* Handle removal */ }
        )
    }
}
```

#### Common Issues and Solutions
1. **State Management**
   - Problem: Lost state during configuration changes
   - Solution: Proper state hoisting and remembering

2. **Animation Glitches**
   - Problem: Jerky animations during drag
   - Solution: Proper animation specs and cleanup

3. **Performance**
   - Problem: Lag during drag operations
   - Solution: Efficient recomposition and state updates

## AT Protocol Integration

### Key Concepts
- DID resolution
- Blob handling
- Feed algorithms
- Record management
- Rich Text Processing:
  - UTF-8 byte-based facet handling
  - Mention, link, and hashtag support
  - Proper emoji and special character handling
  - Facet overlap prevention

### Implementation Details
- Session management
- API rate limiting
- Error handling
- Data models
- Reply Processing:
  - Thread structure maintenance
  - Reference resolution
  - Rich text parsing
  - Character validation
- Follow System:
  - User follow/unfollow functionality
  - Follow status tracking and caching
  - Automatic follow status loading for visible posts
  - Visual follow button state management
  - Loading state indicators for follow actions
  - Error handling for follow operations

### Hashtag System Architecture

#### Current Implementation
- **HashtagScreen Components**
  - Tabbed interface (Latest, Trending, Media)
  - HashtagHeader with metrics display
  - Related hashtags horizontal scroll
  - Follow/unfollow functionality
  - ThreadCard integration for posts
  - Follow button for post authors

- **State Management**
  - HashtagViewModel for data handling
  - Proper loading states
  - Error handling and recovery
  - Empty state management
  - Follow status tracking
  - Author follow status management

- **UI Components**
  - HashtagTopBar with navigation
  - Post count and engagement metrics
  - Related hashtags suggestions
  - Adaptive layout for different screen sizes
  - Loading and error state indicators
  - Follow buttons with loading state indicators

#### Future Expansion Support
- **Infrastructure Ready for AT Protocol Updates**
  - Modular hashtag service integration
  - Extensible post fetching system
  - Pagination-ready architecture
  - Scalable state management
  - Flexible data models

- **Prepared Features (Awaiting AT Protocol Support)**
  - Pagination infrastructure
  - Historical post retrieval system
  - Real-time hashtag updates
  - Advanced hashtag search
  - Trending hashtag algorithms
  - Cross-instance hashtag aggregation

### Hashtag Implementation and Limitations
- **Current Limitations**
  - Maximum of 50 posts per hashtag query
  - No native pagination support for hashtag searches
  - No historical hashtag post retrieval
  - Limited real-time hashtag updates

- **Implementation Details**
  - Uses AT Protocol's post retrieval system
  - Implements proper UTF-8 byte handling for hashtag facets
  - Supports hashtag detection in post creation
  - Maintains proper facet indexing for hashtags

- **Hashtag Processing**
  - Regular expression based detection: `(?:^|\s)(#[^\d\s]\S*)(?=\s|$)`
  - UTF-8 byte position calculation for facets
  - Maximum hashtag length enforcement (64 characters)
  - Proper handling of multi-byte characters in hashtags

- **Best Practices**
  - Cache frequently accessed hashtag results
  - Implement proper error handling for failed queries
  - Use proper logging for debugging
  - Handle empty result sets gracefully
  - Maintain proper state management for hashtag views

### Rich Text Handling
TrendFlick implements AT Protocol's rich text specification using Jetpack Compose's AnnotatedString system:

1. **Facet Processing**
   - Handles UTF-8 byte-based indices
   - Supports mentions (@user), hashtags (#tag), and links
   - Validates and filters invalid facets
   - Prevents facet overlaps
   - Proper emoji and special character support
   - Hashtag-specific processing with 50-post limit awareness

2. **Text Rendering**
   - Converts between UTF-8 bytes and character positions
   - Maintains accurate byte-to-character mapping
   - Handles multi-byte characters (emoji, special characters)
   - Applies appropriate styling for each facet type
   - Implements consistent hashtag styling and interaction

3. **Interaction Handling**
   - Click detection for mentions, hashtags, and links
   - Proper navigation and action handling
   - Error handling for invalid interactions
   - Hashtag-specific UI feedback for limit reached

4. **Debug Support**
   - Detailed logging of facet ranges
   - Invalid facet reporting
   - Text extraction verification
   - Position mapping validation
   - Hashtag query monitoring and debugging

5. **Common Issues**
   - Handles partial facet rendering
   - Manages overlapping facets
   - Processes multi-byte characters correctly
   - Validates facet ranges
   - Handles hashtag query limitations gracefully

## Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Document public APIs
- Write unit tests for new features

### Future Feature Development
- **Hashtag System Expansion**
  - Monitor AT Protocol repository for hashtag API updates
  - Follow modular design patterns for new features
  - Maintain backward compatibility
  - Document integration points for future features
  - Test against different AT Protocol versions
  - Consider cross-instance compatibility

- **Implementation Guidelines**
  - Use feature flags for new hashtag capabilities
  - Implement graceful degradation
  - Maintain consistent error handling
  - Follow AT Protocol best practices
  - Document API version dependencies
  - Consider performance implications

### Git Workflow
- Feature branches
- Pull request reviews
- Semantic versioning
- Conventional commits

### Testing
- Unit tests for repositories
- UI tests for screens
- Integration tests
- Performance testing

## Security Considerations

### Authentication
- Secure credential storage
- Token management
- Session handling
- Rate limiting

### Data Protection
- HTTPS enforcement
- API key protection
- Input validation
- Data encryption

### Permissions
- Runtime permissions
- Storage access
- Network access
- Camera/Media access

## Troubleshooting

### Common Issues
1. Authentication failures
   - Check API credentials
   - Verify network connection
   - Check token expiration

2. Media upload issues
   - Verify size limits
   - Check storage permissions
   - Validate media format

3. Feed loading problems
   - Check rate limits
   - Verify cache state
   - Check network connectivity

4. Hashtag-related issues
   - Understand 50-post limit per query
   - Verify hashtag format and length (max 64 chars)
   - Check UTF-8 encoding for special characters
   - Monitor facet index calculations
   - Verify proper error handling for empty results
   - Ensure proper UI feedback for limit reached
   
   Future Expansion Considerations:
   - Monitor AT Protocol updates for hashtag feature expansion
   - Check HashtagViewModel for new API integration points
   - Verify pagination implementation when available
   - Test real-time update system when supported
   - Validate cross-instance hashtag functionality
   - Review trending algorithm integration points

### Debug Tools
- Logcat filtering
- Network inspection
- Memory profiling
- Performance monitoring

### Support Resources
- GitHub Issues
- Stack Overflow tags
- BlueSky documentation
- AT Protocol specs 