package com.trendflick.data.repository

import android.net.Uri
import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    suspend fun uploadVideo(
        uri: Uri,
        title: String,
        description: String,
        visibility: String,
        tags: List<String>
    ): Result<String>
    
    suspend fun saveVideoMetadata(
        videoUrl: String,
        description: String,
        timestamp: String,
        did: String,
        handle: String,
        postToBlueSky: Boolean = false
    ): Video
    
    fun getVideoFeed(): Flow<List<Video>>
    
    suspend fun getVideos(): List<Video>
    
    suspend fun likeVideo(videoId: String)
    
    suspend fun unlikeVideo(videoId: String)
    
    suspend fun addComment(videoId: String, comment: String)
    
    suspend fun shareVideo(videoId: String)
    
    suspend fun getRelatedVideos(videoId: String): List<Video>
    
    suspend fun insertVideo(video: Video)

    suspend fun testVideoInFolder()

    suspend fun testFolderAccess()
} 