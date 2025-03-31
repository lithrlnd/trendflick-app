package com.trendflick.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Following : Screen("following")
    object Search : Screen("search")
    object CreatePost : Screen("create_post")
    object CreateFlick : Screen("create_flick")
    object Chat : Screen("chat")
    object Profile : Screen("profile")
    object Upload : Screen("upload")
    object AI : Screen("ai")
    
    // Settings routes
    object Settings : Screen("settings")
    object EditProfile : Screen("edit_profile")
    object AppPassword : Screen("app_password")
    object PrivacySettings : Screen("privacy_settings")
    object BlockedAccounts : Screen("blocked_accounts")
    
    // Detail routes
    object VideoDetail : Screen("video/{videoId}") {
        fun createRoute(videoId: String) = "video/$videoId"
    }
    object ProfileDetail : Screen("profile/{did}") {
        fun createRoute(did: String) = "profile/$did"
    }
}
