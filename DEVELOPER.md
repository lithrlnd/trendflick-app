# TrendFlick Developer Documentation

## Overview
TrendFlick is an Android client for the BlueSky social network, built with full compliance with the AT Protocol specifications. This document provides technical details and implementation guidelines for developers.

## Architecture

### Tech Stack
- **Language**: Kotlin 1.9.0
- **Minimum SDK**: Android SDK 34
- **Build System**: Gradle 8.2+
- **UI Framework**: Jetpack Compose
- **Dependency Injection**: Hilt
- **Networking**: Retrofit
- **JSON Parsing**: Moshi
- **Local Storage**: Room
- **Async Operations**: Kotlin Coroutines
- **Real-time Updates**: WebSocket

### Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/trendflick/
│   │   │   ├── data/
│   │   │   │   ├── api/          # Network API interfaces
│   │   │   │   ├── local/        # Local database
│   │   │   │   ├── model/        # Data models
│   │   │   │   ├── repository/   # Data repositories
│   │   │   │   └── sync/         # WebSocket implementations
│   │   │   ├── di/              # Dependency injection modules
│   │   │   └── ui/
│   │   │       ├── components/   # Reusable UI components
│   │   │       ├── screens/      # App screens
│   │   │       └── theme/        # App theming
│   │   └── res/                 # Android resources
│   └── test/                    # Unit tests
```

## AT Protocol Integration

### Authentication
- **Endpoint**: `https://bsky.social/xrpc/`
- **Session Creation**: `com.atproto.server.createSession`
- **Format Requirements**:
  - Handle format: `username.bsky.social`
  - DID format: `did:plc:identifier`
  - App Password format: `xxxx-xxxx-xxxx-xxxx`

### Data Models
All data models follow AT Protocol specifications:

```kotlin
data class AtSession(
    @Json(name = "did") val did: String,
    @Json(name = "handle") val handle: String,
    @Json(name = "accessJwt") val accessJwt: String,
    @Json(name = "refreshJwt") val refreshJwt: String,
    @Json(name = "email") val email: String? = null
)
```

### WebSocket Integration
- **Endpoint**: `wss://bsky.social/xrpc/com.atproto.sync.subscribeRepos`
- **Purpose**: Real-time updates for repository events
- **Event Types**:
  - Commit (record updates)
  - Handle (identity changes)
  - Migrate (repository migrations)

## Key Components

### Camera Implementation
The camera functionality uses CameraX API with the following features:
- Video recording with quality selection
- Time limit options (15s, 60s, 180s, 600s)
- Segment recording
- Front/back camera switching
- Permission handling

```kotlin
// Initialize camera with quality settings
val qualitySelector = QualitySelector.fromOrderedList(
    listOf(Quality.HD, Quality.SD),
    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
)
```

### Video Upload
Video upload process follows AT Protocol's blob storage:
1. Convert video to appropriate format
2. Upload to PDS (Personal Data Server)
3. Create post with video reference
4. Handle response and user feedback

## Testing

### Unit Tests
- Repository tests for AT Protocol compliance
- Authentication flow validation
- Data model verification

Example test:
```kotlin
@Test
fun `createSession returns success with valid credentials`() = runTest {
    val expectedCredentials = mapOf(
        "identifier" to testHandle,
        "password" to "xxxx-xxxx-xxxx-xxxx"
    )
    // ... test implementation
}
```

### Required Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
```

## UI Components

### CameraPreview
A composable that handles video recording with the following features:
- Real-time preview
- Recording controls
- Time limit selection
- Progress indication
- Segment management

### CreativeCompass
Navigation component for:
- Starting recording
- Accessing gallery
- Managing recordings

## Error Handling

### Network Errors
- Authentication failures
- Connection issues
- API response errors

### Camera Errors
- Permission denials
- Hardware access issues
- Recording failures

## Best Practices

### AT Protocol Compliance
1. Always validate handle formats
2. Verify DID structures
3. Follow proper authentication flow
4. Handle repository events correctly

### Performance
1. Use appropriate video quality settings
2. Implement proper memory management
3. Handle background processes efficiently
4. Cache network responses

### Security
1. Secure storage of JWT tokens
2. Proper permission handling
3. Safe file operations
4. Secure network communications

## Contributing

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Document complex functions
- Add unit tests for new features

### Pull Request Process
1. Create feature branch
2. Update documentation
3. Add tests
4. Submit PR with description

## Troubleshooting

### Common Issues
1. Camera initialization failures
   - Check permissions
   - Verify device compatibility
   - Review logcat output

2. Upload failures
   - Verify network connection
   - Check file size limits
   - Validate AT Protocol compliance

3. Authentication issues
   - Verify handle format
   - Check app password format
   - Review token management

## Resources
- [AT Protocol Documentation](https://atproto.com/docs)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose) 