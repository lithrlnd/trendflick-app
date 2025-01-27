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
TrendFlick is a feature-rich BlueSky-compatible social media client that focuses on creating a seamless video and image sharing experience while strictly adhering to the AT Protocol standards. Built with modern Android development practices, it combines powerful media capabilities with AI-enhanced features.

## Key Features

### Core Social Features
1. **Authentication**
   - BlueSky account login/signup with secure credential management
   - Firebase Authentication integration with anonymous fallback
   - Robust session management
   - Handle resolution
   - Secure app password protection

2. **Feed Management**
   - Dynamic timeline viewing with pull-to-refresh
   - Rich post creation with media support
   - Support for BlueSky's feed algorithms
   - Custom feed filtering
   - Vertical and horizontal content paging

3. **Media Handling**
   - Advanced video upload and playback using Media3 (ExoPlayer)
   - Smart video caching system (90MB cache)
   - Image sharing (up to 4 images per post)
   - Efficient blob reference management
   - Media size limit compliance (1MB per image)
   - EXIF data stripping
   - CameraX integration for high-quality captures

4. **Social Interactions**
   - Likes and reposts with haptic feedback
   - Threading and replies
   - Quote posts
   - Rich user profiles with editing capabilities
   - Following/Follower management
   - Blocked accounts management

### Advanced Features
1. **AI Integration**
   - AI-powered content assistance
   - Smart content recommendations
   - OpenAI API integration

2. **Privacy & Security**
   - End-to-end encryption support
   - Secure credential storage
   - Privacy settings management
   - Blocked accounts handling
   - Firebase Crashlytics for stability

3. **User Experience**
   - Rich animations and transitions
   - Haptic feedback
   - Edge-to-edge design
   - Dark/Light theme support
   - Offline capability
   - Pull-to-refresh functionality
   - Splash screen with animations
   - Left side navigation drawer with categories:
     - Trending feed (🔥)
     - For You Page (FYP) (🎯)
     - Following feed (👥)
     - Popular feed (⭐)
   - Smooth animations with Material 3 design

4. **Messaging** (Coming Soon)
   - Direct messaging support
   - Real-time chat capabilities
   - Media sharing in messages

### Feed Management
- **Smart Feed Categories**:
  - FYP (For You Page) using BlueSky's "whats-hot" algorithm
  - Following feed with reverse-chronological timeline
  - Category-based discovery with hashtag filtering
  - Trending hashtags section

### Category Discovery
- **Curated Categories**:
  - Tech & AI (#tech, #ai, #coding, #startup, #innovation)
  - Entertainment (#movies, #tv, #streaming, #cinema)
  - Gaming (#gaming, #esports, #streamer, #gameplay)
  - Art & Design (#art, #design, #illustration, #creative)
  - Beauty (#beauty, #makeup, #skincare, #fashion)
  - Music (#music, #newmusic, #artist, #songs)
  - Food (#food, #cooking, #recipe, #foodie)
  - Fitness (#fitness, #health, #workout, #wellness)

### UI/UX Improvements
- Smooth category drawer with improved edge detection
- Dynamic feed title updates based on selection
- Hashtag previews in category items
- Seamless category switching with auto-closing drawer

## Technical Stack

### Frontend
1. **UI Framework**
   - Jetpack Compose with Material 3
   - Custom animations and transitions
   - Responsive layouts
   - Edge-to-edge design

2. **State Management**
   - MVVM architecture
   - Kotlin Flow for reactive programming
   - Jetpack Navigation Compose
   - ViewModel integration

### Backend Integration
1. **Firebase Services**
   - Authentication
   - Firestore for data persistence
   - Cloud Storage for media
   - Analytics for insights
   - Crashlytics for stability monitoring

2. **Networking**
   - Retrofit with OkHttp
   - Efficient API caching
   - Custom interceptors
   - Moshi and Gson for JSON handling

### Data Management
1. **Local Storage**
   - Room database
   - Secure credential storage
   - Efficient media caching
   - File provider implementation

### Development Features
1. **Build System**
   - Gradle with KTS
   - Multiple build variants
   - ProGuard optimization
   - Resource shrinking

2. **Testing**
   - Unit tests with JUnit and MockK
   - UI tests with Espresso
   - Integration tests
   - Truth assertions

3. **Code Quality**
   - Kotlin static analysis
   - Custom lint rules
   - Crash reporting
   - Performance monitoring

## Technical Requirements
1. **AT Protocol Compliance**
   - Follow all lexicon specifications
   - Proper blob handling
   - Correct JSON structures
   - Handle all required fields

2. **Performance Goals**
   - Fast feed loading with caching
   - Efficient media handling
   - Smooth scrolling and animations
   - Quick post creation
   - Optimized battery usage

3. **Platform Requirements**
   - Minimum SDK: 26 (Android 8.0)
   - Target SDK: 34 (Android 14)
   - Kotlin-first development
   - Jetpack Compose UI

## Success Metrics
1. **User Engagement**
   - Post creation success rate
   - Media upload success rate
   - Feed loading speed
   - Session duration
   - User retention

2. **Technical Performance**
   - Crash-free sessions
   - API call success rate
   - Media loading times
   - Memory usage
   - Battery efficiency

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