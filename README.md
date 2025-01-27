# TrendFlick App

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

## Contributing
Please read our DEVELOPER.md for detailed contribution guidelines.

## License
See LICENSE file for details.

## Current Status

### Video Upload Implementation
- Firebase Storage integration is set up
- Basic video upload functionality implemented
- Test functions created for debugging upload issues
- Currently investigating upload path issues

### Testing Progress
1. Successfully tested:
   - Firebase Storage connection
   - Firestore database connection
   - Test file upload to root directory

2. Current Investigation:
   - Video upload to `/videos` directory
   - File access and permissions
   - Storage path resolution

### Development Environment
- Android Studio
- Firebase Console for monitoring
- Database Inspector for verification

## Setup

### Environment Variables
For security, BlueSky credentials should be set as environment variables:
```bash
# Required for BlueSky integration
export BLUESKY_HANDLE=your.handle.bsky.social
export BLUESKY_APP_PASSWORD=xxxx-xxxx-xxxx-xxxx
```

These can also be set in your IDE's run configuration or your CI/CD pipeline.

1. Firebase Configuration:
   - Create a Firebase project
   - Enable Firebase Storage
   - Create a `/videos` directory in Firebase Storage
   - Set Storage Rules:
   ```javascript
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /videos/{videoId} {
         allow read, write: if true;  // Development only - update for production
       }
     }
   }
   ```

2. BlueSky Integration:
   - Requires BlueSky account for cross-posting
   - Uses AT Protocol for authentication
   - Posts include video links (videos hosted on TrendFlick)

## Development Notes

- Firebase Storage must have `/videos` directory created
- Videos are stored with format: `video_[timestamp]_[UUID].mp4`
- Maximum video size: 100MB
- BlueSky posts include "Coming soon to Android" notice

## Known Issues

1. Firebase Storage Setup:
   - Need to manually create `/videos` directory
   - Storage path must match rules configuration

2. AT Protocol Authentication:
   - Session management needs improvement
   - Better error handling for failed BlueSky auth

3. Video Processing:
   - Speed adjustment feature needs testing
   - Thumbnail generation not implemented

## Coming Soon

- [ ] Proper Firebase Storage directory structure
- [ ] Enhanced error handling
- [ ] Thumbnail support for videos
- [ ] User profile management
- [ ] Video feed implementation
- [ ] Public release on Play Store

## Additional Technical Notes
- The ViewModel uses Kotlin Flow for reactive state management
- Firebase operations are designed to be non-blocking
- All network calls include timeout handling
- State preservation across configuration changes is handled via SavedStateHandle 

## Recent Updates

### Authentication & Storage Setup (Latest)
- Implemented Firebase Authentication for secure video uploads
- Added Firebase Storage configuration with proper security rules
- Set up authentication checks before video uploads
- Maintained BlueSky AT Protocol authentication for social features
- Current Status:
  - ✅ BlueSky AT Protocol authentication working
  - ✅ Firebase Storage configured
  - 🔄 Working on Firebase Authentication for video uploads
  - 🔄 Testing video upload functionality

### Video Feed Implementation
- Added Firestore integration for video storage and retrieval
- Implemented timestamp handling in ISO 8601 format with UTC timezone
- Added detailed logging for debugging video feed issues
- Enhanced error handling for video document parsing
- Improved video metadata storage with consistent document IDs

### BlueSky Integration
- Updated AT Protocol post creation with proper embed structure
- Enhanced video post format on BlueSky with description and embedded video
- Added support for optional BlueSky posting
- Implemented proper URI formatting for cross-platform compatibility

### Firebase Configuration
- Set up Cloud Firestore with `videos` collection
- Implemented document structure with ISO timestamp format
- Added real-time video feed listener with proper ordering
- Enhanced video metadata storage and retrieval
- Added Firebase Storage rules for secure video uploads:
  ```javascript
  rules_version = '2';
  service firebase.storage {
    match /b/{bucket}/o {
      match /videos/{videoId} {
        allow read: if true;  // Public video access
        allow write: if request.auth != null;  // Authenticated uploads only
      }
    }
  }
  ```

### Technical Improvements
- Added comprehensive logging throughout the application
- Enhanced error handling and validation
- Improved timestamp parsing and storage
- Added document verification after saves 

## Setup Instructions

### Firebase Configuration
1. Storage Rules are configured for testing
2. Firestore Database is set up
3. Test video path: `/videos/TestVideo.mp4`

### Testing
Currently using two approaches for testing:
1. Manual testing through Android Studio debugger
2. Database Inspector for verifying data 