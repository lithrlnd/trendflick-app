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

## Implemented Features

### Authentication
- ✅ BlueSky account login/signup integration
- ✅ Secure session management with refresh capabilities
- ✅ Persistent credentials storage
- ✅ Automatic session recovery and token refresh

### Feed Management
- ✅ Multiple feed types:
  - Trending ("what's hot")
  - Following
  - Category-based feeds
  - Hashtag-based feeds
- ✅ Smart feed filtering and aggregation
- ✅ Category system with associated hashtags
- ✅ Efficient post deduplication
- ✅ Pull-to-refresh functionality

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

### Media Handling
- ✅ Image upload and display
- ✅ Video upload support
- ✅ Blob reference management
- ✅ Media size validation
- ✅ MIME type handling

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
1. Fix feed loading for Following and Categories
2. Optimize hashtag-based content filtering
3. Implement proper Following feed integration
4. Enhance feed caching and performance
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

## Rich Text Architecture
The app implements AT Protocol's rich text specification with the following features:

### Facet Processing
- Proper UTF-8 byte indexing for international text support
- Efficient facet validation and filtering
- Smart handling of overlapping facets
- Proper character-to-byte mapping for accurate indices

### Supported Features
- @mentions with proper DID resolution
- #hashtags with category integration
- URLs with proper styling and validation
- Proper handling of emoji and special characters

### Implementation Details
- Uses Jetpack Compose's AnnotatedString for efficient rendering
- Implements proper byte-to-character index mapping
- Handles edge cases and invalid facets gracefully
- Provides comprehensive logging for debugging