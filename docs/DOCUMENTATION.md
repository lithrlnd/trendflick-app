# TrendFlick Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Setup Guide](#setup-guide)
3. [Core Components](#core-components)
4. [AT Protocol Integration](#at-protocol-integration)
5. [Development Guidelines](#development-guidelines)
6. [Security Considerations](#security-considerations)
7. [Troubleshooting](#troubleshooting)

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
- Implements multiple feed types
- Handles pagination
- Manages feed caching
- Implements custom filtering

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

### Video Feed Implementation
- **Comment System**
  - Unified comment system across posts and videos
  - Consistent engagement actions (like, comment, repost, share)
  - Author filtering capability
  - Rich text support in comments
  - Proper state management for comments overlay
  - Smooth animations and transitions
  
- **Engagement Features**
  - Right-aligned engagement column
  - Haptic feedback on actions
  - Proper state persistence
  - Consistent styling with main feed
  - Proper count formatting (K, M suffixes)
  
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

### Rich Text Implementation
The comment system implements AT Protocol's rich text specification:

1. **Facet Processing**
   - UTF-8 byte-based indices for proper text handling
   - Support for @mentions, #hashtags, and URLs
   - Proper emoji and special character handling
   - Facet overlap prevention
   - Validation of facet ranges

2. **Rich Text Overlay**
   - Long-press activation with haptic feedback
   - Smooth fade animations for overlay visibility
   - Fallback mechanism between caption and description
   - Proper text rendering with AT Protocol facets
   - Consistent dark theme styling
   - Tap-to-dismiss functionality
   - Scrollable content for long text
   - Comprehensive state logging
   - Error state handling

3. **Implementation Details**
   ```kotlin
   RichTextPostOverlay(
       visible: Boolean,
       text: String,
       facets: List<Facet>,
       onDismiss: () -> Unit
   )
   ```
   
   Key Features:
   - Semi-transparent black background (alpha: 0.85)
   - Rounded corner surface for content
   - Centered positioning
   - Proper padding and spacing
   - Support for both image and video content
   - Maintains aspect ratio of media content
   - Handles empty caption states

4. **Usage Guidelines**
   - Implement long-press detection using `pointerInput`
   - Add haptic feedback using `HapticFeedbackConstants.LONG_PRESS`
   - Manage overlay visibility with `remember` state
   - Use proper logging for state changes
   - Handle both caption and description fields
   - Implement proper cleanup on dismiss
   - Follow accessibility guidelines

5. **Best Practices**
   - Always provide haptic feedback for long-press
   - Use fallback text when caption is empty
   - Maintain proper state management
   - Log important state changes
   - Handle edge cases (empty text, null facets)
   - Follow consistent animation patterns
   - Ensure proper contrast for text
   - Test with various content lengths

6. **Common Issues**
   - Handle empty captions gracefully
   - Manage proper text contrast
   - Handle orientation changes
   - Manage proper z-indexing
   - Handle system interruptions
   - Manage memory efficiently

### Development Guidelines

When working with the comment system:
1. Always use proper UTF-8 handling for facets
2. Maintain consistent styling with the main feed
3. Implement proper error handling
4. Follow AT Protocol specifications for rich text
5. Ensure proper state management
6. Handle all edge cases (empty states, errors, etc.)
7. Maintain consistent engagement behavior
8. Use proper animation timings
9. Implement proper cleanup
10. Follow accessibility guidelines

### Engagement System
#### Visual Implementation
- **Color Scheme**
  - Active State: `Color(0xFF6B4EFF)` - Deep purple for activated elements
  - Inactive State: `Color(0xFFB4A5FF)` - Light purple for better visibility
  - Consistent across both Trends and Flicks interfaces
  - Designed for visibility against both light and dark backgrounds

- **Animation Effects**
  - Scale animation on interaction (0.8f to 1f)
  - Spring-based animation with medium bouncy damping
  - Smooth transitions for state changes
  - Visual feedback for user interactions

- **Haptic Feedback**
  - Dual haptic response system:
    - `HapticFeedbackConstants.VIRTUAL_KEY`
    - `HapticFeedbackConstants.KEYBOARD_TAP`
  - Consistent feedback across all engagement actions
  - Enhanced tactile response for better user experience

#### Implementation Details
```kotlin
@Composable
private fun EngagementAction(
    icon: ImageVector,
    count: Int = 0,
    isActive: Boolean = false,
    tint: Color = Color(0xFFB4A5FF),
    isHorizontal: Boolean = false,
    onClick: () -> Unit
)
```

- **Features**:
  - Like/Unlike with heart animation
  - Comment system with count display
  - Repost functionality
  - Share capability
  - Numeric formatting (K, M suffixes)
  - Responsive layout (portrait/landscape)

- **Layout Variants**:
  - Portrait: Vertical column on right edge
  - Landscape: Horizontal row at top
  - Adaptive spacing and alignment
  - Proper edge padding and spacing

#### Integration Points
- **Trends Page**:
  - Integrated with `ThreadCard.kt`
  - Consistent styling with text content
  - Proper state management
  - Rich text post context

- **Flicks Page**:
  - Integrated with video/image content
  - Enhanced visibility against media
  - Maintained interaction consistency
  - Media-specific optimizations

#### Best Practices
1. **State Management**
   - Use `remember` for local state
   - Implement proper state hoisting
   - Handle all interaction states
   - Maintain consistency across screens

2. **Performance**
   - Efficient recomposition
   - Proper animation cleanup
   - Optimized haptic feedback
   - Smart layout measurements

3. **Accessibility**
   - Clear touch targets
   - Proper content descriptions
   - Consistent interaction patterns
   - Adequate contrast ratios

4. **Error Handling**
   - Graceful state updates
   - Proper error recovery
   - Network failure handling
   - State restoration

#### Usage Guidelines
```kotlin
EngagementColumn(
    isLiked = isLiked,
    isReposted = isReposted,
    likeCount = likeCount,
    replyCount = replyCount,
    repostCount = repostCount,
    onLikeClick = { /* Handle like */ },
    onCommentClick = { /* Handle comment */ },
    onRepostClick = { /* Handle repost */ },
    onShareClick = { /* Handle share */ }
)
```

Developers should:
1. Maintain consistent color usage
2. Preserve haptic feedback implementation
3. Handle all interaction states
4. Implement proper error handling
5. Follow the established animation patterns
6. Ensure proper state management
7. Test against various background contents
8. Verify landscape/portrait adaptations
9. Maintain accessibility standards
10. Follow performance guidelines

### Image Viewer Implementation
The image viewer provides a full-screen image viewing experience similar to popular social media platforms.

#### Components
1. **ImageViewer.kt**
   ```kotlin
   @Composable
   fun ImageViewer(
       images: List<ImageEmbed>,
       initialImageIndex: Int = 0,
       onDismiss: () -> Unit
   )
   ```

   Key Features:
   - Full-screen display with black background
   - Horizontal paging for multiple images
   - Purple close button (Color: `0xFF6B4EFF`)
   - Interactive pagination dots
   - Proper image scaling and aspect ratio

2. **Integration with ThreadCard**
   - Tracks selected image index
   - Handles image click events
   - Manages viewer visibility state
   - Provides smooth transitions

#### Visual Implementation
- **Layout**
  - Full-screen overlay with black background
  - Top-aligned close button
  - Center-aligned image content
  - Bottom-aligned pagination dots
  - Proper status bar padding

- **Navigation**
  - Horizontal swipe between images
  - Tap to dismiss
  - Visual feedback for current image
  - Smooth transitions between images

- **Styling**
  - Close Button: Circle shape with purple background
  - Pagination Dots:
    - Selected: Solid purple (`0xFF6B4EFF`)
    - Unselected: Semi-transparent white
  - Image Scaling: `ContentScale.Fit`
  - Background: Solid black for optimal viewing

#### Usage Guidelines
1. **Implementation**
   ```kotlin
   var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

   // Show viewer when image is clicked
   AsyncImage(
       modifier = Modifier.clickable { selectedImageIndex = index }
   )

   // Add viewer overlay
   selectedImageIndex?.let { index ->
       ImageViewer(
           images = images,
           initialImageIndex = index,
           onDismiss = { selectedImageIndex = null }
       )
   }
   ```

2. **Best Practices**
   - Handle proper image loading states
   - Implement error handling for failed loads
   - Maintain aspect ratios
   - Consider memory management for large images
   - Implement proper cleanup on dismiss
   - Handle configuration changes
   - Support accessibility features

3. **Error Handling**
   - Handle null or invalid image URLs
   - Provide fallback for loading failures
   - Manage memory efficiently
   - Handle network connectivity issues

4. **Performance Considerations**
   - Lazy loading of images
   - Proper image caching
   - Memory management for large images
   - Efficient state management
   - Smooth animations and transitions

#### Integration Points
1. **ThreadCard Integration**
   - Image click handling
   - State management
   - Proper layout integration
   - Consistent styling

2. **AT Protocol Compliance**
   - Proper image URL construction
   - Blob reference handling
   - Proper alt text support
   - Accessibility considerations

#### Common Issues and Solutions
1. **Image Loading**
   - Use proper error handling
   - Implement loading states
   - Handle network failures
   - Manage memory efficiently

2. **Navigation**
   - Handle gesture conflicts
   - Maintain smooth transitions
   - Proper state management
   - Handle edge cases

3. **State Management**
   - Track selected image index
   - Handle configuration changes
   - Manage viewer visibility
   - Clean up resources

4. **Performance**
   - Optimize image loading
   - Manage memory usage
   - Handle large image sets
   - Smooth animations

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

### Rich Text Handling
TrendFlick implements AT Protocol's rich text specification using Jetpack Compose's AnnotatedString system:

1. **Facet Processing**
   - Handles UTF-8 byte-based indices
   - Supports mentions (@user), hashtags (#tag), and links
   - Validates and filters invalid facets
   - Prevents facet overlaps
   - Proper emoji and special character support

2. **Text Rendering**
   - Converts between UTF-8 bytes and character positions
   - Maintains accurate byte-to-character mapping
   - Handles multi-byte characters (emoji, special characters)
   - Applies appropriate styling for each facet type

3. **Interaction Handling**
   - Click detection for mentions, hashtags, and links
   - Proper navigation and action handling
   - Error handling for invalid interactions

4. **Debug Support**
   - Detailed logging of facet ranges
   - Invalid facet reporting
   - Text extraction verification
   - Position mapping validation

5. **Common Issues**
   - Handles partial facet rendering
   - Manages overlapping facets
   - Processes multi-byte characters correctly
   - Validates facet ranges

## Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Document public APIs
- Write unit tests for new features

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