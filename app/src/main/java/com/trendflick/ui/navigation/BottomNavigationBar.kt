package com.trendflick.ui.navigation

import android.os.Bundle
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.trendflick.ui.viewmodels.SharedViewModel

data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showBadge: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedFeed by sharedViewModel.selectedFeed.collectAsState()

    Box(modifier = modifier.fillMaxWidth()) {
        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            containerColor = Color.Black,
            contentColor = Color.White
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = currentRoute == Screen.Home.route,
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6B4EFF),
                    selectedTextColor = Color(0xFF6B4EFF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
            
            NavigationBarItem(
                icon = { Icon(Icons.Default.Message, contentDescription = "Messages") },
                label = { Text("Messages") },
                selected = currentRoute == Screen.Messages.route,
                onClick = {
                    navController.navigate(Screen.Messages.route) {
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6B4EFF),
                    selectedTextColor = Color(0xFF6B4EFF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                label = { Text("Search") },
                selected = false,
                onClick = {
                    // TODO: Implement search navigation
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6B4EFF),
                    selectedTextColor = Color(0xFF6B4EFF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI") },
                label = { Text("AI") },
                selected = currentRoute == Screen.AI.route,
                onClick = {
                    navController.navigate(Screen.AI.route) {
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6B4EFF),
                    selectedTextColor = Color(0xFF6B4EFF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = currentRoute == Screen.Profile.route,
                onClick = {
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6B4EFF),
                    selectedTextColor = Color(0xFF6B4EFF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
} 