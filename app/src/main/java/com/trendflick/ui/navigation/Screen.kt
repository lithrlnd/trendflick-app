package com.trendflick.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object WhatsHot : Screen("whats_hot")
    object Messages : Screen("messages")
    object Search : Screen("search")
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
    data class Custom(val id: String) : Screen("custom/$id")

    companion object {
        fun fromRoute(route: String?): Screen {
            return when(route?.substringBefore("?")) {
                Splash.route -> Splash
                Login.route -> Login
                Home.route -> Home
                WhatsHot.route -> WhatsHot
                Messages.route -> Messages
                Search.route -> Search
                CreatePost.route -> CreatePost
                Upload.route -> Upload
                CreateFlick.route -> CreateFlick
                Chat.route -> Chat
                Profile.route -> Profile
                AI.route -> AI
                Settings.route -> Settings
                EditProfile.route -> EditProfile
                AppPassword.route -> AppPassword
                PrivacySettings.route -> PrivacySettings
                BlockedAccounts.route -> BlockedAccounts
                VideoSettings.route -> VideoSettings
                null -> Home
                else -> throw IllegalArgumentException("Route $route is not recognized.")
            }
        }
    }
} 