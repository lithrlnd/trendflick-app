package com.trendflick

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.trendflick.ui.navigation.Screen
import com.trendflick.ui.navigation.BottomNavigationBar
import com.trendflick.ui.screens.home.HomeScreen
import com.trendflick.ui.screens.profile.ProfileScreen
import com.trendflick.ui.screens.splash.SplashScreen
import com.trendflick.ui.screens.login.LoginScreen
import com.trendflick.ui.screens.ai.AIScreen
import com.trendflick.ui.screens.messages.MessagesScreen
import com.trendflick.ui.theme.TrendFlickTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.trendflick.ui.screens.settings.*
import com.trendflick.ui.screens.create.CreatePostScreen
import com.trendflick.ui.screens.create.CreateFlickScreen
import androidx.core.view.WindowCompat
import com.trendflick.ui.screens.upload.UploadScreen
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.trendflick.ui.viewmodels.SharedViewModel
import com.trendflick.ui.components.CastButton
import com.trendflick.ui.screens.following.FollowingScreen

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure window for edge-to-edge and no title bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
        
        // Keep screen on while app is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            TrendFlickTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding() // Add system bars padding
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Splash.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Screen.Splash.route) {
                                SplashScreen(navController = navController)
                            }
                            composable(Screen.Login.route) {
                                LoginScreen(navController = navController)
                            }
                            composable(Screen.Home.route) {
                                val sharedViewModel: SharedViewModel = hiltViewModel()
                                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                                    HomeScreen(
                                        onNavigateToProfile = { did ->
                                            navController.navigate("profile/$did")
                                        },
                                        navController = navController,
                                        sharedViewModel = sharedViewModel
                                    )
                                }
                            }
                            composable(Screen.Messages.route) {
                                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                                    MessagesScreen(navController = navController)
                                }
                            }
                            composable(Screen.CreateFlick.route) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Black
                                ) {
                                    CreateFlickScreen(
                                        onNavigateBack = { navController.navigateUp() }
                                    )
                                }
                            }
                            composable(Screen.CreatePost.route) {
                                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                                    CreatePostScreen(navController = navController)
                                }
                            }
                            composable(Screen.Profile.route) {
                                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                                    ProfileScreen(navController = navController)
                                }
                            }
                            composable(Screen.Upload.route) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Black
                                ) {
                                    UploadScreen(navController = navController)
                                }
                            }
                            composable(Screen.AI.route) {
                                AIScreen(navController = navController)
                            }
                            composable(Screen.WhatsHot.route) {
                                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                                    FollowingScreen(
                                        onNavigateToProfile = { did: String ->
                                            navController.navigate("profile/$did")
                                        },
                                        onNavigateToCreatePost = {
                                            navController.navigate(Screen.CreatePost.route)
                                        },
                                        onCreatePost = {
                                            navController.navigate(Screen.CreatePost.route)
                                        }
                                    )
                                }
                            }
                            
                            // Settings Routes
                            composable(Screen.Settings.route) {
                                SettingsScreen(navController = navController)
                            }
                            composable(Screen.EditProfile.route) {
                                EditProfileScreen(navController = navController)
                            }
                            composable(Screen.AppPassword.route) {
                                AppPasswordScreen(navController = navController)
                            }
                            composable(Screen.PrivacySettings.route) {
                                PrivacySettingsScreen(navController = navController)
                            }
                            composable(Screen.BlockedAccounts.route) {
                                // TODO: Implement BlockedAccountsScreen
                            }
                        }
                        
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                        if (currentRoute != Screen.Splash.route && 
                            currentRoute != Screen.Login.route) {
                            
                            // Show bottom navigation bar on main screens
                            if (!currentRoute.toString().startsWith("settings")) {
                                BottomNavigationBar(
                                    navController = navController,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
} 