# TrendFlick - Modern BlueSky Social Client

A Material 3 Android client for BlueSky that focuses on rich media sharing and seamless social interactions, built with Jetpack Compose and AT Protocol compliance.

## 🌟 Key Features
- 📱 Modern Material 3 UI with Jetpack Compose
- 🎥 Seamless video and image sharing
- 🔄 AT Protocol-compliant rich text support
- 🏷️ Smart category and hashtag system
- 💬 Advanced threading and replies
- 🎨 Dark/Light theme support
- 🔒 Secure authentication
- 🎯 Customizable Navigation System

## Implemented Features

### Authentication
- ✅ BlueSky account login/signup integration
- ✅ Secure session management with refresh capabilities
- ✅ Persistent credentials storage
- ✅ Automatic session recovery and token refresh

### Feed Management
- ✅ Multiple feed types with optimized loading:
  - Trends feed: 50 posts with "whats-hot" algorithm
  - Following feed: 25 posts with "reverse-chronological" algorithm
- ✅ Smart feed filtering and aggregation:
  - Dynamic parameter selection based on feed type
  - Proper timestamp validation and sorting
  - Efficient post filtering and validation
  - Comprehensive error handling
- ✅ Category system with associated hashtags
- ✅ Efficient post deduplication
- ✅ Pull-to-refresh functionality
- ✅ Cursor-based pagination
- ✅ State preservation during feed switches
- ✅ Proper error recovery and logging

### Social Interactions
- ✅ Like/Unlike posts with state persistence
- ✅ Advanced reply system:
  - Nested reply support with proper threading
  - Rich text support in replies (@mentions, #hashtags)
  - Character limit enforcement (300 chars)
  - Visual character counter with color indicators
  - Thread hierarchy visualization with indentation
  - Original Poster (OP) highlighting
  - Reply reference handling per AT Protocol spec
  - Parent and root reference maintenance
- ✅ Repost functionality
- ✅ Thread view support
- ✅ Profile viewing
- ✅ Enhanced quote post handling:
  - Proper URI and CID extraction
  - In-app browser for viewing quoted posts
  - Graceful error handling for deleted posts
  - Automatic thumbnail generation for quoted posts
  - Retry mechanism for temporary network issues
- ✅ Follow/Unfollow functionality:
  - Follow button in ThreadCard component
  - Visual loading state indicators
  - Automatic follow status loading for visible posts
  - Proper state management and caching
  - Error handling for follow operations
  - Consistent UI across all screens (Home, Following, Hashtag)

### Media Handling
- ✅ Image upload and display
- ✅ Video upload support
- ✅ Blob reference management
- ✅ Media size validation
- ✅ MIME type handling
- ✅ Enhanced thumbnail generation:
  - Multiple fallback mechanisms for different content types
  - Special handling for social media platforms (YouTube, Twitter/X, Instagram, TikTok)
  - Domain-specific optimizations for important links
  - Microlink API integration for screenshot-based thumbnails
  - Favicon fallback for domains without thumbnails

### Category System
- ✅ Predefined categories with curated hashtags
- ✅ Smart post filtering based on hashtags
- ✅ Category-specific feeds
- ✅ Trending hashtags support

### Technical Features
- ✅ AT Protocol compliance
  - Strong references for replies (URI + CID)
  - Proper thread structure maintenance
  - Parent/root reference handling
  - Rich text facet processing
- ✅ Efficient caching system
- ✅ Error handling and recovery
- ✅ Rate limiting management
- ✅ Firebase integration for state persistence
- ✅ Background processing for media uploads

### Enhanced Post Creation
- ✅ AI-powered content enhancement
  - Smart post improvement suggestions
  - Automatic hashtag recommendations
  - One-click content optimization
  - Character limit compliance (300 chars)
- ✅ Rich text support in posts
  - @mentions with suggestions
  - #hashtags with trending topics
  - Smart formatting
- ✅ Visual post preview
- ✅ Character counter with visual feedback

### Navigation System
- ✅ Customizable bottom navigation:
  - Slide-up drawer for navigation management
  - Drag-and-drop category organization
  - Native-like edit mode with wiggle animation
  - Add/remove navigation items
  - Restore removed items
  - Smart category grouping
  - Maximum 7 navigation items
  - Haptic feedback for interactions
  - Smooth animations and transitions
  - Home item always locked
  - Horizontal scrolling for extra items

### UI/UX Features
- ✅ Material 3 design implementation
- ✅ Dark/Light theme support
- ✅ Smooth animations and transitions
- ✅ Native-like interaction patterns:
  - iOS/Android-style edit mode
  - Wiggle animations
  - Small remove buttons
  - Haptic feedback
  - Drag and drop
- ✅ State preservation across view changes
- ✅ Responsive layout design

## Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **Network**: Retrofit with AT Protocol
- **Storage**: 
  - Firebase Realtime Database (likes)
  - SharedPreferences (session)
  - Room Database (local cache)
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Rich Text**: Custom AT Protocol facet implementation

## Project Structure
```
app/
├── data/
│   ├── api/         # AT Protocol API interfaces
│   ├── auth/        # Authentication management
│   ├── local/       # Local storage and caching
│   ├── model/       # Data models
│   └── repository/  # Repository implementations
├── di/              # Dependency injection modules
├── ui/
│   ├── components/  # Reusable UI components
│   │   └── RichTextRenderer.kt  # AT Protocol rich text implementation
│   ├── screens/     # Screen implementations
│   └── theme/       # App theming
└── utils/           # Utility classes
```

## Current Status
- Core AT Protocol integration complete
- Category and hashtag system implemented
- Feed management and filtering operational
- Reply system fully implemented:
  - Proper AT Protocol threading
  - Rich text support
  - Visual hierarchy
  - Character limits
  - Error handling
- Media upload framework in place
- Session management robust and reliable

## Next Steps
1. Enhance reply system:
   - Add mention/hashtag suggestions
   - Implement floating suggestion UI
   - Add rich text preview
   - Improve facet processing for clickable elements
2. Enhance media playback experience
3. Implement custom feed algorithms
4. Add offline support
5. Enhance error recovery mechanisms
6. Implement advanced search features

## Development Guidelines
- Follow Material 3 design principles
- Maintain AT Protocol compliance
- Ensure proper error handling
- Write unit tests for new features
- Document public APIs

## Getting Started
1. Clone the repository
2. Add your BlueSky API credentials
3. Set up Firebase project
4. Build and run

## Contributing
Contributions are welcome! Please read our contributing guidelines and submit pull requests for any enhancements.

## License
[Add your license information here]

## Core Purpose
TrendFlick is a BlueSky-compatible social media client that focuses on creating a seamless video and image sharing experience while strictly adhering to the AT Protocol standards.

## Key Features
1. **Authentication**
   - BlueSky account login/signup
   - Session management
   - Handle resolution

2. **Feed Management**
   - Timeline viewing
   - Post creation with media
   - Support for BlueSky's feed algorithms
   - Custom feed filtering
   - Repost functionality with proper CID validation
   - Like/Unlike posts with status caching

3. **Media Handling**
   - Video upload and playback
   - Image sharing (up to 4 images per post)
   - Blob reference management
   - Media size limit compliance (1MB per image)
   - EXIF data stripping

4. **Social Interactions**
   - Likes and reposts with proper AT Protocol strong references
   - Threading and replies
   - Quote posts
   - User profiles
   - Following/Follower management

5. **Embed Support**
   - External links with preview cards
   - Image embeds
   - Record embeds
   - Record with media embeds

## Technical Requirements
1. **AT Protocol Compliance**
   - Follow all lexicon specifications
   - Proper blob handling
   - Correct JSON structures
   - Handle all required fields
   - Strong reference validation for reposts and likes
   - CID format validation for content references

2. **Performance Goals**
   - Fast feed loading
   - Efficient media caching
   - Smooth scrolling
   - Quick post creation
   - Optimized like/repost state management
   - Status caching for improved responsiveness

3. **User Experience**
   - Material 3 design
   - Dark/Light theme support
   - Offline capability
   - Error recovery
   - Clear feedback
   - Immediate UI state updates for social actions

## Development Priorities
1. ✅ Optimize feed loading for Following and Categories
2. Enhance hashtag-based content filtering
3. ✅ Implement proper Following feed integration
4. ✅ Enhance feed caching and performance
5. Core AT Protocol integration
6. Media handling and uploads
7. Feed viewing and interaction
8. Profile management
9. AI features integration
10. Advanced features (custom feeds, messaging)

## Security Considerations
1. Secure credential storage
2. API key protection
3. Media access permissions
4. User data privacy
5. Network security

## Development Notes

- Firebase Storage must have `/videos`

## Features

### Core Features
- BlueSky account integration
- Video and image sharing
- Feed management
- Social interactions

### Enhanced Comments System
- Smart comment filtering with author-only view
- State-preserving scroll position
- Seamless toggle between all comments and author responses
- Real-time comment updates
- Rich text support in comments

### Media Handling
- Video upload and playback
- Image sharing (up to 4 images per post)
- Blob reference management
- Media size limit compliance (1MB per image)
- EXIF data stripping

### Social Interactions
- Likes and reposts
- Threading and replies
- Quote posts
- User profiles
- Following/Follower management

### UI/UX Features
- Material 3 design implementation
- Dark/Light theme support
- Smooth animations and transitions
- State preservation across view changes
- Responsive layout design

## Technical Details

### AT Protocol Integration
- Full lexicon specification compliance
- Proper blob handling
- Correct JSON structures
- Handle resolution
- Feed algorithms support

### Performance
- Efficient state management
- Scroll position preservation
- Smart recomposition handling
- Optimized list rendering
- Memory efficient media handling

### Security
- Secure authentication
- API rate limiting
- Data validation
- Safe media handling

## Development Setup

[Development setup instructions...]

## Contributing

[Contributing guidelines...]

## License

[License information...]

## 🎥 Video Upload Feature

TrendFlick now supports video uploads directly to Bluesky! This feature allows users to share videos up to 60 seconds in length with automatic optimization and compression.

### Features
- Upload videos up to 60 seconds long
- Automatic video compression and optimization
- Support for both vertical and horizontal videos
- Smart quality adjustment to meet Bluesky's size limits
- Progress tracking and error handling
- Maintains aspect ratio and video quality

### Technical Specifications
- **Maximum Duration**: 60 seconds
- **File Format**: MP4
- **Maximum File Size**: 50MB
- **Video Codec**: H.264/AVC
- **Target Resolution**: Up to 360p (optimized for mobile)
- **Bitrate**: 250Kbps (adaptive)
- **Frame Rate**: 20fps

### How to Use
1. Tap the FAB (Floating Action Button)
2. Select "Record" or choose an existing video
3. Add a description
4. Preview your video
5. Post to your Bluesky feed

### Implementation Details
- Automatic video compression if file size exceeds limits
- Vertical video detection for optimal display
- Proper error handling and user feedback
- Progress tracking during upload
- Compliant with Bluesky's AT Protocol standards

## Core Features
- Authentication with BlueSky
- Timeline viewing and interaction
- Post creation with media support
- Custom feed filtering
- Social interactions (likes, reposts, etc.)
- Profile management

## Technical Requirements
- Android SDK 24+
- Kotlin-first development
- Jetpack Compose UI
- Material 3 design

## Getting Started
1. Clone the repository
2. Add your Bluesky API credentials
3. Build and run the project

## Contributing
We welcome contributions! Please read our contributing guidelines before submitting pull requests.

## License
[Add your license information here]

## Rich Text Implementation
- ✅ Full AT Protocol compliance for rich text facets
- ✅ Proper UTF-8 byte indexing for international text
- ✅ Support for mentions, links, and hashtags
- ✅ Efficient facet processing and rendering
- ✅ Proper handling of overlapping facets
- ✅ Clickable text elements with proper styling
- ✅ Error-resistant facet processing
- ✅ Comprehensive logging for debugging
- ✅ Emoji and special character support
- ✅ Accurate byte-to-character position mapping
- ✅ Smart hashtag handling with '#' symbol inclusion
- ✅ Proper multi-byte character handling
- ✅ Efficient text range validation
- ✅ Robust facet overlap prevention

## Rich Text Architecture
The app implements AT Protocol's rich text specification with the following features:

### Facet Processing
- Proper UTF-8 byte indexing for international text support
- Efficient facet validation and filtering
- Smart handling of overlapping facets
- Proper character-to-byte mapping for accurate indices
- Comprehensive debug logging system
- Multi-byte character support for emoji and special characters
- Efficient text range validation and correction

### Supported Features
- @mentions with proper DID resolution
- #hashtags with category integration and proper symbol handling
- URLs with proper styling and validation
- Proper handling of emoji and special characters
- Click handling for all interactive elements
- Visual feedback for interactive elements

### Implementation Details
- Uses Jetpack Compose's AnnotatedString for efficient rendering
- Implements proper byte-to-character index mapping
- Handles edge cases and invalid facets gracefully
- Provides comprehensive logging for debugging
- Maintains AT Protocol compliance for all text processing
- Efficient overlap detection and prevention
- Smart text range validation and correction

### Posts
- Create text posts with rich text support
- Mention users with @ suggestions
- Add hashtags with # suggestions
- Support for BlueSky's facet system
- Character limit compliance (300 characters)

### Video Sharing
- Record and share videos up to 60 seconds
- Upload videos from gallery
- Video preview and playback
- Automatic video duration validation
- EXIF data stripping

### Social Features
- User mentions with real-time suggestions
- Hashtag support with trending suggestions
- Profile viewing and management
- Follow/Unfollow functionality
- Like and repost interactions

### Authentication
- BlueSky account login/signup
- Session management
- Secure credential handling

## Technical Details

### AT Protocol Integration
- Full compliance with AT Protocol standards
- Rich text handling with facets
- Proper blob handling for media
- Rate limit handling
- Error recovery

### User Experience
- Material 3 design language
- Dark/Light theme support
- Real-time suggestions for mentions and hashtags
- Smooth video recording and playback
- Error feedback and recovery

### Performance
- Efficient media caching
- Optimized video handling
- Fast feed loading
- Smooth scrolling

## Development Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on an Android device or emulator

## Requirements

- Android SDK 24+
- Kotlin 1.8+
- BlueSky account for testing

## Contributing

Please read our contributing guidelines before submitting pull requests.

## License

[Your License Here]

## Recent Enhancements

### Quote Post Improvements (February 2025)
- ✅ Enhanced RecordEmbed component:
  - Proper URI and CID extraction from embedded posts
  - Improved placeholder content for unavailable posts
  - In-app browser implementation using WebView in AlertDialog
  - Better styling aligned with Bluesky's design
  - Handle and post ID extraction from URI for better display
  - Conversion from AT Protocol URIs to web URLs
  - Microlink API integration for thumbnail generation

### Link Preview Enhancements (February 2025)
- ✅ Improved EmbeddedLink component:
  - Support for large header images for important links
  - Enhanced thumbnail URL generation with multiple fallbacks
  - Special handling for YouTube, Twitter/X, Instagram, TikTok, and Facebook
  - Domain name display extracted from URL
  - Better styling with borders for improved visibility
  - Improved layout with better spacing and padding
  - Conditional display based on content type

### Debugging Improvements (February 2025)
- ✅ Enhanced logging for embed processing
- ✅ Detailed tracking of thumbnail generation
- ✅ Better error reporting for failed post fetching
- ✅ Tracking of record embed processing

### Follow System Implementation (March 2025)
- ✅ Comprehensive follow button functionality:
  - Follow/unfollow capability across all screens
  - Visual loading state indicators during API operations
  - Automatic follow status loading for visible posts
  - Proper state management with MutableStateFlow
  - Error handling with appropriate user feedback
  - Consistent UI implementation across Home, Following, and Hashtag screens
  - Optimized follow status caching for better performance
  - Fixed compilation issues with proper state handling
  - Improved LaunchedEffect implementation for follow status loading