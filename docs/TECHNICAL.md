# Technical Documentation

## Text Post Creation

### Overview
The text post creation system supports rich text features including user mentions and hashtags, fully compliant with the AT Protocol's facet system.

### Components

#### CreatePostScreen
- Handles user input and suggestion display
- Implements real-time mention and hashtag triggers
- Manages text input state and character limits
- Provides visual feedback for suggestions

#### CreatePostViewModel
- Manages post creation state
- Handles user and hashtag searches
- Processes text input for mentions and hashtags
- Manages suggestion lists
- Handles post creation through AT Protocol

### Data Flow

1. User Input
   - Text input is monitored for @ and # triggers
   - Triggers activate respective search functions
   - Results are displayed in suggestion list

2. Suggestion Selection
   - User selects mention or hashtag
   - Text is updated with selection
   - Suggestion list is cleared
   - Cursor position is maintained

3. Post Creation
   - Text is processed for facets
   - Mentions and hashtags are converted to AT Protocol format
   - Post is created with proper facets
   - Success/error states are handled

### AT Protocol Integration

#### Facets
- Mentions: `app.bsky.richtext.facet#mention`
- Tags: `app.bsky.richtext.facet#tag`
- ByteSlices: Used for proper text indexing
- Features: Mapped to appropriate AT Protocol types

#### Search APIs
- User Search: Uses `app.bsky.actor.search`
- Hashtag Search: Mock implementation (pending AT Protocol support)

### Error Handling
- Invalid mentions are caught and reported
- Network errors are handled gracefully
- Rate limiting is respected
- Session validation is performed

### Future Improvements
- Implement proper hashtag search when AT Protocol adds support
- Add link preview support
- Enhance suggestion UI
- Add image attachment support

## Implementation Details

### UserSearchResult
```kotlin
data class UserSearchResult(
    val did: String,
    val handle: String,
    val displayName: String?,
    val avatar: String? = null
)
```

### TrendingHashtag
```kotlin
data class TrendingHashtag(
    val tag: String,
    val count: Int
)
```

### CreatePostViewModel States
```kotlin
data class CreatePostUiState(
    val isLoading: Boolean = false,
    val isPostSuccessful: Boolean = false,
    val error: String? = null,
    val userSuggestions: List<UserSearchResult> = emptyList(),
    val hashtagSuggestions: List<TrendingHashtag> = emptyList()
)
```

## Testing

### Unit Tests
- ViewModel tests for suggestion handling
- Repository tests for AT Protocol integration
- Facet processing tests

### UI Tests
- Suggestion list display
- Mention/hashtag insertion
- Character limit handling
- Error state display

## Performance Considerations

- Debounced search queries
- Efficient suggestion list updates
- Proper state management
- Memory-efficient list handling 