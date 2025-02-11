package com.trendflick.data.repository

import com.trendflick.data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun getTrendingPosts(): List<Post>
    suspend fun getPostsByCategory(categoryId: String): List<Post>
    suspend fun likePost(postId: String): Boolean
    suspend fun unlikePost(postId: String): Boolean
    suspend fun repostPost(postId: String): Boolean
    suspend fun unrepostPost(postId: String): Boolean
    suspend fun getPostComments(postId: String): List<Post>
    suspend fun addComment(postId: String, comment: String): Post
} 