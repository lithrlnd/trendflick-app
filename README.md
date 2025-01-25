# TrendFlick App

A modern video sharing app that integrates with BlueSky's AT Protocol.

## Features

- Video upload and sharing
- Cross-posting to BlueSky (optional)
- Video speed adjustment
- Firebase Storage for video hosting
- AT Protocol integration for BlueSky posts

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