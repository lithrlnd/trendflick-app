package com.trendflick.data.repository

import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.Flow

interface BlueskyVideoRepository {
    suspend fun getVideos(): List<Video>
    suspend fun getVideoProcessingStatus(jobId: String): VideoProcessingStatus
    suspend fun getVideoBlob(blobId: String): ByteArray
    
    sealed class VideoProcessingStatus {
        data class Completed(val blobId: String) : VideoProcessingStatus()
        data class Processing(val progress: Int) : VideoProcessingStatus()
        data class Failed(val error: String) : VideoProcessingStatus()
        object Unknown : VideoProcessingStatus()
    }
} 