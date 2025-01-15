package com.trendflick.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.trendflick.data.db.Converters

@Entity(tableName = "videos")
@TypeConverters(Converters::class)
data class Video(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var userId: String = "",
    var username: String = "",
    var videoUrl: String = "",
    var thumbnailUrl: String = "",
    var title: String = "",
    var description: String = "",
    var likes: Int = 0,
    var comments: Int = 0,
    var shares: Int = 0,
    @TypeConverters(Converters::class)
    var hashtags: List<String> = emptyList()
) {
    @Ignore
    var relatedVideos: List<Video> = emptyList()
} 