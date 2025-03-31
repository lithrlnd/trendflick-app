package com.trendflick.data.repository

import com.trendflick.data.model.UserSuggestion
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * Repository for handling hashtag and user mention suggestions
 */
@Singleton
class HashtagRepository @Inject constructor() {

    private val TAG = "TF_HashtagRepository"
    
    // Mock data for hashtag suggestions
    private val popularHashtags = listOf(
        "trending", "viral", "fyp", "foryou", "trendflick",
        "bluesky", "atprotocol", "tech", "android", "developer",
        "coding", "music", "dance", "comedy", "food",
        "travel", "fitness", "fashion", "beauty", "art",
        "photography", "nature", "animals", "sports", "gaming"
    )
    
    // Mock data for user suggestions
    private val popularUsers = listOf(
        UserSuggestion("John Smith", "johnsmith", null),
        UserSuggestion("Tech Insider", "techinsider", null),
        UserSuggestion("Travel Guide", "travelguide", null),
        UserSuggestion("Fitness Coach", "fitnesscoach", null),
        UserSuggestion("Food Lover", "foodlover", null),
        UserSuggestion("Music Fan", "musicfan", null),
        UserSuggestion("Art Gallery", "artgallery", null),
        UserSuggestion("Gaming Pro", "gamingpro", null),
        UserSuggestion("Fashion Trends", "fashiontrends", null),
        UserSuggestion("Nature Explorer", "natureexplorer", null)
    )
    
    /**
     * Get hashtag suggestions based on query
     */
    suspend fun getHashtagSuggestions(query: String): List<String> {
        Log.d(TAG, "Getting hashtag suggestions for: $query")
        
        // Simulate network delay
        delay(300)
        
        // Filter hashtags based on query
        return popularHashtags
            .filter { it.contains(query, ignoreCase = true) }
            .take(5)
            .also { Log.d(TAG, "Returning ${it.size} hashtag suggestions") }
    }
    
    /**
     * Get user suggestions based on query
     */
    suspend fun getUserSuggestions(query: String): List<UserSuggestion> {
        Log.d(TAG, "Getting user suggestions for: $query")
        
        // Simulate network delay
        delay(300)
        
        // Filter users based on query
        return popularUsers
            .filter { 
                it.username.contains(query, ignoreCase = true) || 
                it.handle.contains(query, ignoreCase = true) 
            }
            .take(5)
            .also { Log.d(TAG, "Returning ${it.size} user suggestions") }
    }
    
    /**
     * Track hashtag usage for analytics
     */
    suspend fun trackHashtagUsage(hashtag: String) {
        Log.d(TAG, "Tracking hashtag usage: #$hashtag")
        
        // In a real app, this would send analytics data to a backend
        delay(100)
    }
    
    /**
     * Get trending hashtags
     */
    suspend fun getTrendingHashtags(): List<String> {
        Log.d(TAG, "Getting trending hashtags")
        
        // Simulate network delay
        delay(500)
        
        // Return a subset of popular hashtags as "trending"
        return popularHashtags
            .shuffled()
            .take(10)
            .also { Log.d(TAG, "Returning ${it.size} trending hashtags") }
    }
    
    /**
     * Get recently used hashtags
     */
    suspend fun getRecentHashtags(): List<String> {
        Log.d(TAG, "Getting recent hashtags")
        
        // Simulate network delay
        delay(300)
        
        // Return a subset of popular hashtags as "recent"
        return popularHashtags
            .shuffled()
            .take(5)
            .also { Log.d(TAG, "Returning ${it.size} recent hashtags") }
    }
    
    /**
     * Get frequently mentioned users
     */
    suspend fun getFrequentlyMentionedUsers(): List<UserSuggestion> {
        Log.d(TAG, "Getting frequently mentioned users")
        
        // Simulate network delay
        delay(300)
        
        // Return a subset of popular users as "frequently mentioned"
        return popularUsers
            .shuffled()
            .take(5)
            .also { Log.d(TAG, "Returning ${it.size} frequently mentioned users") }
    }
}
