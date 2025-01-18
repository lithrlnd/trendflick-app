package com.trendflick.data.local

import androidx.room.*
import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY id DESC")
    fun getAllVideos(): Flow<List<Video>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: Video)

    @Update
    suspend fun updateVideo(video: Video)

    @Delete
    suspend fun deleteVideo(video: Video)

    @Query("UPDATE videos SET likes = likes + 1 WHERE id = :videoId")
    suspend fun incrementLikes(videoId: Int)

    @Query("UPDATE videos SET likes = CASE WHEN likes > 0 THEN likes - 1 ELSE 0 END WHERE id = :videoId")
    suspend fun decrementLikes(videoId: Int)

    @Query("UPDATE videos SET commentCount = commentCount + 1 WHERE id = :videoId")
    suspend fun incrementComments(videoId: Int)

    @Query("UPDATE videos SET shares = shares + 1 WHERE id = :videoId")
    suspend fun incrementShares(videoId: Int)
} 