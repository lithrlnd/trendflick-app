package com.trendflick.data

import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.util.UUID

object SampleDataProvider {
    private val _videos = MutableStateFlow<List<Video>>(listOf(
        Video(
            uri = "at://did:plc:sample1/app.bsky.feed.post/1",
            videoUrl = "https://example.com/video1.mp4",
            description = "Sample video 1",
            createdAt = Instant.now(),
            indexedAt = Instant.now(),
            sortAt = Instant.now(),
            did = "did:plc:sample1",
            handle = "user1.bsky.social",
            title = "Sample Video 1",
            thumbnailUrl = "https://example.com/thumb1.jpg",
            username = "user1",
            userId = "did:plc:sample1"
        ),
        Video(
            uri = "at://did:plc:sample2/app.bsky.feed.post/2",
            videoUrl = "https://example.com/video2.mp4",
            description = "Sample video 2",
            createdAt = Instant.now(),
            indexedAt = Instant.now(),
            sortAt = Instant.now(),
            did = "did:plc:sample2",
            handle = "user2.bsky.social",
            title = "Sample Video 2",
            thumbnailUrl = "https://example.com/thumb2.jpg",
            username = "user2",
            userId = "did:plc:sample2"
        ),
        Video(
            uri = "at://did:plc:sample3/app.bsky.feed.post/3",
            videoUrl = "https://example.com/video3.mp4",
            description = "Sample video 3",
            createdAt = Instant.now(),
            indexedAt = Instant.now(),
            sortAt = Instant.now(),
            did = "did:plc:sample3",
            handle = "user3.bsky.social",
            title = "Sample Video 3",
            thumbnailUrl = "https://example.com/thumb3.jpg",
            username = "user3",
            userId = "did:plc:sample3"
        )
    ))

    val videos: StateFlow<List<Video>> = _videos

    fun addVideo(video: Video) {
        _videos.value = _videos.value + video
    }

    fun updateVideos(videos: List<Video>) {
        _videos.value = videos
    }
} 