package com.trendflick

import android.os.Bundle
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
import com.trendflick.ui.screens.upload.UploadScreen
import com.trendflick.ui.screens.profile.ProfileScreen
import com.trendflick.ui.screens.splash.SplashScreen
import com.trendflick.ui.screens.login.LoginScreen
import com.trendflick.ui.theme.TrendFlickTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            TrendFlickTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    Box(modifier = Modifier.fillMaxSize()) {
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
                                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                                    HomeScreen(navController = navController)
                                }
                            }
                            composable(Screen.Upload.route) {
                                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                                    UploadScreen()
                                }
                            }
                            composable(Screen.Profile.route) {
                                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                                    ProfileScreen(navController = navController)
                                }
                            }
                        }
                        
                        // Only show bottom navigation bar when not on splash or login screen
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                        if (currentRoute != Screen.Splash.route && currentRoute != Screen.Login.route) {
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