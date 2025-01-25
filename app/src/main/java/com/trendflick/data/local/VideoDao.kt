package com.trendflick.data.local

import androidx.room.*
import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY sortAt DESC")
    fun getAllVideos(): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE uri = :videoUri")
    suspend fun getVideoById(videoUri: String): Video?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: Video)

    @Update
    suspend fun updateVideo(video: Video)

    @Delete
    suspend fun deleteVideo(video: Video)

    @Query("UPDATE videos SET likes = likes + 1 WHERE uri = :videoUri")
    suspend fun incrementLikes(videoUri: String)

    @Query("UPDATE videos SET likes = likes - 1 WHERE uri = :videoUri")
    suspend fun decrementLikes(videoUri: String)

    @Query("UPDATE videos SET comments = comments + 1 WHERE uri = :videoUri")
    suspend fun incrementComments(videoUri: String)

    @Query("UPDATE videos SET shares = shares + 1 WHERE uri = :videoUri")
    suspend fun incrementShares(videoUri: String)

    @Query("SELECT * FROM videos WHERE did = :did ORDER BY sortAt DESC")
    fun getVideosByUser(did: String): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE uri != :videoUri ORDER BY sortAt DESC LIMIT 5")
    suspend fun getRelatedVideos(videoUri: String): List<Video>
} 