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

### Rich Text Implementation
The comment system implements AT Protocol's rich text specification:

1. **Facet Processing**
   - UTF-8 byte-based indices for proper text handling
   - Support for @mentions, #hashtags, and URLs
   - Proper emoji and special character handling
   - Facet overlap prevention
   - Validation of facet ranges

2. **Comment Rendering**
   - Proper indentation for nested replies
   - OP badge for original poster
   - Timestamp formatting
   - Avatar and user info display
   - Rich text support in comments
   - Character limit enforcement

3. **State Management**
   - Author-only filter state
   - Comment visibility state
   - Reply input state
   - Loading and error states
   - Refresh functionality

4. **Performance Considerations**
   - Efficient list rendering with keys
   - Proper composition to prevent recomposition
   - State hoisting for better performance
   - Proper cleanup of resources
   - Efficient overlay handling

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