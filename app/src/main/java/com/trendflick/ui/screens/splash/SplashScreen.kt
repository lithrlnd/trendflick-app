package com.trendflick.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.trendflick.ui.navigation.Screen
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.trendflick.data.auth.BlueskyCredentialsManager

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    var startAnimation by remember { mutableStateOf(false) }
    var startTextAnimation by remember { mutableStateOf(false) }
    
    val scale = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val rotation = animateFloatAsState(
        targetValue = if (startAnimation) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    val textScale = animateFloatAsState(
        targetValue = if (startTextAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(500) // Delay before starting text animation
        startTextAnimation = true
        delay(1700) // Remaining time before navigation
        
        // Navigate to Login if no credentials, otherwise go to Home
        val destination = if (viewModel.hasCredentials()) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
        
        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated logo
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale.value)
                    .graphicsLayer { 
                        rotationZ = rotation.value
                    }
            ) {
                // Outer circle with gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                )
                
                // Inner circle with play icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(64.dp)
                    )
                }
            }
            
            // App name with animation
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "TrendFlick",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .scale(textScale.value)
                    .graphicsLayer {
                        alpha = textScale.value
                    }
            )
            
            // Tagline
            Text(
                text = "Share Your Story",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier = Modifier
                    .scale(textScale.value)
                    .graphicsLayer {
                        alpha = textScale.value
                    }
            )
        }
    }
} 