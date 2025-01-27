# TrendFlick App

A modern BlueSky-compatible social media client focused on creating a seamless video and image sharing experience while strictly adhering to the AT Protocol standards.

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
- ✅ View and create replies
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
│   ├── screens/     # Screen implementations
│   └── theme/       # App theming
└── utils/           # Utility classes
```

## Current Status
- Core AT Protocol integration complete
- Category and hashtag system implemented
- Feed management and filtering operational
- Social interactions (likes, replies) working
- Media upload framework in place
- Session management robust and reliable

## Next Steps
1. Enhance media playback experience
2. Implement custom feed algorithms
3. Add offline support
4. Enhance error recovery mechanisms
5. Implement advanced search features

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
   - Rich text rendering with interactive elements:
     - Clickable @mentions
     - Clickable #hashtags
     - Clickable links with underline styling
     - UTF-8 compliant text processing
     - Future-proof feature handling

3. **Media Handling**
   - Video upload and playback
   - Image sharing (up to 4 images per post)
   - Blob reference management
   - Media size limit compliance (1MB per image)
   - EXIF data stripping

4. **Social Interactions**
   - Likes and reposts
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
   - Rich text facet processing
   - UTF-8 byte offset handling

2. **Performance Goals**
   - Fast feed loading
   - Efficient media caching
   - Smooth scrolling
   - Quick post creation
   - Responsive text rendering

3. **User Experience**
   - Material 3 design
   - Dark/Light theme support
   - Offline capability
   - Error recovery
   - Clear feedback
   - Interactive text elements

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