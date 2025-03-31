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

/**
 * Improved BottomNavigationBar with swipe tray functionality
 */
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
    
    // Define navigation items
    val navItems = listOf(
        NavItem(
            screen = Screen.Home,
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        NavItem(
            screen = Screen.Following,
            label = "Following",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People
        ),
        NavItem(
            screen = Screen.Search,
            label = "Search",
            selectedIcon = Icons.Filled.Search,
            unselectedIcon = Icons.Outlined.Search
        ),
        NavItem(
            screen = Screen.AI,
            label = "AI",
            selectedIcon = Icons.Filled.SmartToy,
            unselectedIcon = Icons.Outlined.SmartToy
        ),
        NavItem(
            screen = Screen.Profile,
            label = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // Main navigation bar
        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            containerColor = Color.Black,
            contentColor = Color.White,
            tonalElevation = 8.dp
        ) {
            navItems.forEach { item ->
                val isSelected = when {
                    item.screen == Screen.Home && currentRoute == Screen.Home.route -> true
                    item.screen == Screen.Following && currentRoute == Screen.Following.route -> true
                    item.screen == Screen.Search && currentRoute == Screen.Search.route -> true
                    item.screen == Screen.AI && currentRoute == Screen.AI.route -> true
                    item.screen == Screen.Profile && currentRoute == Screen.Profile.route -> true
                    else -> false
                }
                
                NavigationBarItem(
                    icon = { 
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    },
                    label = { Text(item.label) },
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.screen.route) {
                            navController.navigate(item.screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6B4EFF),
                        selectedTextColor = Color(0xFF6B4EFF),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Black
                    )
                )
            }
        }
        
        // Floating toggle button for Trends/Flicks
        if (currentRoute == Screen.Home.route) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-20).dp)
            ) {
                Card(
                    modifier = Modifier
                        .height(40.dp)
                        .width(160.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        // Trends button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selectedFeed == "Trends") Color(0xFF6B4EFF) else Color.Transparent
                                )
                                .clickable { sharedViewModel.updateSelectedFeed("Trends") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Trends",
                                color = if (selectedFeed == "Trends") Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Flicks button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selectedFeed == "Flicks") Color(0xFF6B4EFF) else Color.Transparent
                                )
                                .clickable { sharedViewModel.updateSelectedFeed("Flicks") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Flicks",
                                color = if (selectedFeed == "Flicks") Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showBadge: Boolean = false
)
