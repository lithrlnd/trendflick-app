package com.trendflick.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Messages : Screen("messages")
    object CreatePost : Screen("create_post")
    object Upload : Screen("upload")
    object CreateFlick : Screen("create_flick")
    object Chat : Screen("chat")
    object Profile : Screen("profile")
    object AI : Screen("ai")
    object Settings : Screen("settings")
    object EditProfile : Screen("settings/edit_profile")
    object AppPassword : Screen("settings/app_password")
    object PrivacySettings : Screen("settings/privacy")
    object BlockedAccounts : Screen("settings/blocked")
    object VideoSettings : Screen("video_settings")
} 