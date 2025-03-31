package com.trendflick.ui.screens.flicks

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Wrapper component that handles orientation changes for the FlicksScreen
 * Automatically switches between portrait and landscape layouts
 */
@Composable
fun FlicksScreenWrapper(
    navController: NavController,
    viewModel: FlicksViewModel = hiltViewModel()
) {
    // Get current orientation
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Use appropriate screen based on orientation
    if (isLandscape) {
        LandscapeFlicksScreen(navController = navController, viewModel = viewModel)
    } else {
        FlicksScreen(navController = navController, viewModel = viewModel)
    }
}
