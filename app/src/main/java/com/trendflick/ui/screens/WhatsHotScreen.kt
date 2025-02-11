package com.trendflick.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trendflick.ui.components.CategoryFeedScreen
import com.trendflick.viewmodel.WhatsHotViewModel
import com.trendflick.ui.navigation.CustomCategory
import com.trendflick.ui.navigation.CategoryType
import com.trendflick.ui.navigation.EngagementType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp

@Composable
fun WhatsHotScreen(
    viewModel: WhatsHotViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val whatsHotCategory = CustomCategory(
        id = "whats-hot",
        icon = Icons.Default.TrendingUp,
        label = "What's Hot",
        type = CategoryType.APP_VIEW,
        description = "Popular content across the network",
        onClick = { /* Handle click */ },
        showTrending = true,
        showHashtags = true
    )

    CategoryFeedScreen(
        category = whatsHotCategory,
        posts = posts,
        onEngagement = { post, type -> 
            when (type) {
                EngagementType.LIKE -> viewModel.likePost(post)
                EngagementType.COMMENT -> viewModel.commentPost(post)
                EngagementType.SHARE -> viewModel.sharePost(post)
                EngagementType.REPOST -> viewModel.repostPost(post)
                EngagementType.SAVE -> viewModel.savePost(post)
            }
        },
        onHashtagClick = { tag -> viewModel.handleHashtagClick(tag) },
        modifier = modifier
    )
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
} 