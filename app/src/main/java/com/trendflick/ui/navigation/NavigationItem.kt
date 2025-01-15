package com.trendflick.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) 