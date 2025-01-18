package com.trendflick.data

import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SampleDataProvider {
    private val _videos = MutableStateFlow(mutableListOf(
        Video(
            id = 1,
            did = "did:plc:user1",
            handle = "bigbuckcreator.bsky.social",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
            thumbnailUrl = "https://storage.googleapis.com/exoplayer-test-media-0/images/test-media-1.jpg",
            title = "Big Buck Bunny",
            description = "A funny bunny's adventure",
            likes = 1200,
            commentCount = 350,
            shares = 280,
            hashtags = listOf("animation", "funny", "bunny", "3d")
        ),
        Video(
            id = 2,
            did = "did:plc:user2",
            handle = "wavecreator.bsky.social",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/test-mp4.mp4",
            thumbnailUrl = "https://storage.googleapis.com/exoplayer-test-media-0/images/test-media-2.jpg",
            title = "Colorful Waves",
            description = "Beautiful wave patterns in motion",
            likes = 890,
            commentCount = 230,
            shares = 150,
            hashtags = listOf("waves", "colors", "art", "motion")
        ),
        Video(
            id = 3,
            did = "did:plc:user3",
            handle = "framemaster.bsky.social",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/frames-960x540.mp4",
            thumbnailUrl = "https://storage.googleapis.com/exoplayer-test-media-0/images/test-media-3.jpg",
            title = "Frame by Frame",
            description = "A mesmerizing sequence of frames",
            likes = 750,
            commentCount = 180,
            shares = 120,
            hashtags = listOf("frames", "sequence", "tech", "art")
        )
    ))
    
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()
    private var nextId = 4

    fun addVideo(video: Video) {
        val newVideo = video.copy(id = nextId++).apply {
            relatedVideos = emptyList()
        }
        val currentList = _videos.value.toMutableList()
        currentList.add(0, newVideo) // Add to the beginning of the list
        updateVideos(currentList)
        updateVideoRelations()
    }

    fun updateVideos(newList: List<Video>) {
        _videos.value = newList.toMutableList()
        updateVideoRelations()
    }

    fun getRelatedVideos(video: Video): List<Video> {
        // Return videos that share at least one hashtag with the input video
        return _videos.value.filter { other ->
            other.id != video.id && 
            other.hashtags.any { it in video.hashtags }
        }
    }

    fun updateVideoRelations() {
        val currentList = _videos.value.map { video ->
            video.apply {
                relatedVideos = getRelatedVideos(video)
            }
        }
        _videos.value = currentList.toMutableList()
    }
} 