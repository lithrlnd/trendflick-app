package com.trendflick.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.trendflick.ui.components.SettingsSection
import com.trendflick.ui.components.SettingsItem
import com.trendflick.ui.components.SettingsSwitch
import com.trendflick.ui.components.ColorSchemeSelector
import com.trendflick.ui.navigation.Screen
import com.trendflick.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Observe isLoggedOut state and navigate when true
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfilePicture(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    val user = uiState.user
                    if (user?.avatar != null) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Display Name and Handle
                Text(
                    text = uiState.user?.displayName ?: "Set Display Name",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "@${uiState.user?.handle ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider()

        // Profile Settings
        SettingsSection(title = "Profile") {
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Edit Profile",
                subtitle = "Change your name, bio, and other profile info",
                onClick = { navController.navigate("edit_profile") }
            )
            SettingsItem(
                icon = Icons.Default.Key,
                title = "App Password",
                subtitle = "Manage your BlueSky app password",
                onClick = { navController.navigate("app_password") }
            )
        }

        // Privacy Settings
        SettingsSection(title = "Privacy") {
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Privacy Settings",
                subtitle = "Control who can see your content",
                onClick = { navController.navigate("privacy_settings") }
            )
            SettingsItem(
                icon = Icons.Default.Block,
                title = "Blocked Accounts",
                subtitle = "Manage blocked accounts",
                onClick = { navController.navigate("blocked_accounts") }
            )
        }

        // Content Settings
        SettingsSection(title = "Content") {
            SettingsItem(
                icon = Icons.Default.VideoLibrary,
                title = "Video Quality",
                subtitle = "Adjust upload and playback quality",
                onClick = { navController.navigate("video_settings") }
            )
            SettingsItem(
                icon = Icons.Default.Storage,
                title = "Storage",
                subtitle = "Manage app storage and cache",
                onClick = { navController.navigate("storage_settings") }
            )
        }

        // Appearance Settings
        SettingsSection(title = "Appearance") {
            val isDark by ThemeManager.isDarkTheme.collectAsState()
            val primaryHue by ThemeManager.primaryHue.collectAsState()

            SettingsSwitch(
                title = "Dark Mode",
                subtitle = "Toggle dark/light theme",
                checked = isDark,
                onCheckedChange = { ThemeManager.updateThemeMode(it) },
                modifier = Modifier.fillMaxWidth()
            )

            ColorSchemeSelector(
                selectedHue = primaryHue,
                onHueChange = { ThemeManager.updatePrimaryHue(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }

        // Account Actions
        SettingsSection(title = "Account") {
            SettingsItem(
                icon = Icons.Default.Logout,
                title = "Logout",
                subtitle = "Sign out of your account",
                onClick = { showLogoutDialog = true },
                iconTint = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showLogoutDialog) {
        var isLoggingOut by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { 
                if (!isLoggingOut) showLogoutDialog = false 
            },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = {
                        isLoggingOut = true
                        scope.launch {
                            try {
                                viewModel.logout()
                                // Navigation is now handled by LaunchedEffect
                            } catch (e: Exception) {
                                Log.e("SettingsScreen", "Error during logout: ${e.message}")
                                isLoggingOut = false
                            }
                        }
                    }
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Logout")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 