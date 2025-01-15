package com.trendflick.data.db

import androidx.room.*
import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY id DESC")
    fun getAllVideos(): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun getVideoById(videoId: Int): Video?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: Video): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<Video>)

    @Update
    suspend fun updateVideo(video: Video)

    @Delete
    suspend fun deleteVideo(video: Video)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Query("SELECT * FROM videos WHERE userId = :userId")
    fun getVideosByUser(userId: String): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchVideos(query: String): Flow<List<Video>>
} 