package com.trendflick.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Upload : Screen("upload")
} 