# TrendFlick - BlueSky AT Protocol Client

A modern Android client for BlueSky social network, built with full compliance with the AT Protocol specifications.

## Features

- Full AT Protocol compliance
- Secure authentication with BlueSky
- Session management with JWT tokens
- Real-time updates via WebSocket
- Local data caching
- Modern Android architecture

## Technical Stack

- Kotlin 1.9.0
- Android SDK 34
- Gradle 8.2+
- Jetpack Compose for UI
- Hilt for dependency injection
- Retrofit for network calls
- Moshi for JSON parsing
- Room for local storage
- Coroutines for async operations
- WebSocket for real-time updates

## AT Protocol Compliance

### Authentication
- Handles in format: `username.bsky.social`
- DIDs in format: `did:plc:identifier`
- App Password format: `xxxx-xxxx-xxxx-xxxx`
- JWT token management for access and refresh

### API Endpoints
- Base URL: `https://bsky.social/xrpc/`
- Session creation: `com.atproto.server.createSession`
- WebSocket: `com.atproto.sync.subscribeRepos`

### Data Models
- Compliant session model with required fields
- Identity management with DID and handle
- Profile data handling
- Repository event handling

## Setup

1. Clone the repository
2. Create a `secrets.properties` file in the project root with:
   ```properties
   BSKY_API_KEY=your_api_key
   ```
3. Open in Android Studio
4. Build and run

## Authentication

To use TrendFlick, you need:
1. A BlueSky account
2. An App Password from BlueSky settings (https://bsky.app/settings/app-passwords)

### Getting an App Password:
1. Log in to BlueSky web interface
2. Go to Settings > App Passwords
3. Create a new app password
4. Copy the generated password (format: xxxx-xxxx-xxxx-xxxx)

## Configuration

The app uses the following endpoints:
- Authentication: `https://bsky.social/xrpc/`
- WebSocket: `wss://bsky.social/xrpc/com.atproto.sync.subscribeRepos`

## Troubleshooting

### HTTP 400 Errors
- Verify your handle includes `.bsky.social`
- Ensure app password is in correct format
- Check network connection

### HTTP 401 Errors
- Verify your app password is correct
- Generate a new app password if needed
- Check if your session has expired

### Common Issues
1. Invalid handle format
   - Solution: Use complete handle (e.g., `username.bsky.social`)
2. Invalid app password
   - Solution: Generate new app password from BlueSky settings
3. Network issues
   - Solution: Check internet connection and try again

## Development

### Testing
- Unit tests for repository operations
- Authentication flow tests
- WebSocket connection tests
- Mock API responses

### Architecture
- MVVM architecture
- Repository pattern
- Clean Architecture principles
- Dependency injection with Hilt

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## License

[Add your license here]

## AT Protocol Resources

- [AT Protocol Documentation](https://docs.bsky.app/)
- [BlueSky Developer Portal](https://bsky.app/developer)
- [AT Protocol Specifications](https://atproto.com/specs) 