# Comments System Documentation

## Overview
The TrendFlick comments system provides a rich, interactive experience for viewing and filtering comments while maintaining optimal performance and state preservation.

## Features

### Author-Only Filtering
- Toggle between all comments and author-only responses
- Maintains separate scroll states for each view
- Smooth transitions between views
- Preserves user's reading position

### Technical Implementation

#### State Management
```kotlin
val showAuthorOnly by viewModel.showAuthorOnly.collectAsState()
val allCommentsState = rememberLazyListState()
val authorCommentsState = rememberLazyListState()
```

#### Filtering Logic
```kotlin
val filteredReplies = remember(thread, showAuthorOnly) {
    if (showAuthorOnly) {
        thread.replies?.filter { reply ->
            reply.post.author.did == thread.post.author.did
        }
    } else {
        thread.replies
    }
}
```

#### UI Components
- LazyColumn with dual state management
- Custom Switch component for filtering
- Optimized item rendering with keys
- Proper spacing and layout management

### Performance Considerations
1. State Preservation
   - Separate LazyListStates prevent unnecessary recomposition
   - Scroll position maintained across view changes
   - Efficient memory usage with remember

2. List Optimization
   - Stable keys for items
   - Proper list item recycling
   - Efficient filtering implementation

3. UI Responsiveness
   - Smooth animations
   - Immediate feedback on toggle
   - Proper state updates

## Integration with AT Protocol
- Follows AT Protocol comment structure
- Proper handling of author DIDs
- Maintains protocol compliance for replies
- Supports rich text in comments

## Usage Example
```kotlin
FilteredCommentsList(
    thread = threadPost,
    viewModel = homeViewModel,
    onProfileClick = { did -> handleProfileClick(did) }
)
```

## Best Practices
1. Always use keys for list items
2. Maintain separate scroll states
3. Use remember for expensive computations
4. Implement proper error handling
5. Follow Material 3 design guidelines

## Future Improvements
- [ ] Add comment threading depth indicator
- [ ] Implement comment sorting options
- [ ] Add comment search functionality
- [ ] Enhance rich text support
- [ ] Add comment analytics 