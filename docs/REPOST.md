# Repost Functionality Documentation

## Overview
The repost functionality in TrendFlick follows the AT Protocol's strong reference requirements, allowing users to repost content while maintaining proper content identifiers and references.

## Implementation Details

### Models
```kotlin
// Response model for repost status checks
data class GetRepostsResponse(
    val repostedBy: List<AtProfile>,
    val cursor: String?,
    val uri: String
)

// Record model for creating reposts
data class RepostRecord(
    val type: String = "app.bsky.feed.repost",
    val subject: PostReference,
    val createdAt: String
)

// Strong reference model
data class PostReference(
    val uri: String,
    val cid: String
)
```

### Key Features
1. **CID Validation**
   - Validates Content Identifier (CID) format
   - Ensures CID starts with "bafyrei" or "bafy"
   - Prevents invalid repost attempts

2. **Status Caching**
   - Caches repost status for 60 seconds
   - Reduces API calls for status checks
   - Improves UI responsiveness

3. **Error Handling**
   - Detailed error logging
   - Graceful failure recovery
   - User-friendly error messages

### API Flow
1. **Repost Creation**
   ```
   POST /xrpc/com.atproto.repo.createRecord
   {
     "repo": "$USER_DID",
     "collection": "app.bsky.feed.repost",
     "record": {
       "type": "app.bsky.feed.repost",
       "subject": {
         "uri": "$POST_URI",
         "cid": "$POST_CID"
       },
       "createdAt": "$TIMESTAMP"
     }
   }
   ```

2. **Repost Status Check**
   ```
   GET /xrpc/app.bsky.feed.getRepostedBy
   ?uri=$POST_URI
   ```

3. **Repost Deletion**
   ```
   POST /xrpc/com.atproto.repo.deleteRecord
   {
     "repo": "$USER_DID",
     "collection": "app.bsky.feed.repost",
     "rkey": "$REPOST_KEY"
   }
   ```

### Usage Example
```kotlin
// Create a repost
viewModel.repost(postUri)

// Check repost status
val isReposted = repository.isPostRepostedByUser(postUri)

// Remove repost
viewModel.repost(postUri) // Toggles the repost state
```

## Error Cases
1. Invalid CID format
2. Missing URI or CID
3. Network failures
4. Rate limiting
5. Duplicate repost attempts

## Performance Considerations
- Status caching reduces API load
- Immediate UI updates with optimistic updates
- Batch status checks for multiple posts
- Error state persistence

## Future Improvements
1. Offline repost queueing
2. Enhanced error recovery
3. Batch repost operations
4. Extended caching strategies
5. Real-time status updates 