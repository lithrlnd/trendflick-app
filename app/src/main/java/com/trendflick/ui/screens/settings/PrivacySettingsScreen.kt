package com.trendflick.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.trendflick.ui.components.SettingsSwitch
import com.trendflick.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hideAdultContent by remember { mutableStateOf(uiState.user?.preferences?.get("hideAdultContent") as? Boolean ?: true) }
    var hideReplies by remember { mutableStateOf(uiState.user?.preferences?.get("hideReplies") as? Boolean ?: false) }
    var hideReposts by remember { mutableStateOf(uiState.user?.preferences?.get("hideReposts") as? Boolean ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Content Visibility",
                style = MaterialTheme.typography.titleMedium
            )

            SettingsSwitch(
                title = "Hide Adult Content",
                subtitle = "Filter out content marked as adult or sensitive",
                checked = hideAdultContent,
                onCheckedChange = { checked ->
                    hideAdultContent = checked
                    uiState.user?.let { user ->
                        val updatedPreferences = updatePrivacySettings(user.preferences ?: mapOf(), "hideAdultContent", checked.toString())
                        viewModel.updateUser(user.copy(preferences = updatedPreferences))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            SettingsSwitch(
                title = "Hide Replies",
                subtitle = "Only show original posts in your feed",
                checked = hideReplies,
                onCheckedChange = { checked ->
                    hideReplies = checked
                    uiState.user?.let { user ->
                        val updatedPreferences = updatePrivacySettings(user.preferences ?: mapOf(), "hideReplies", checked.toString())
                        viewModel.updateUser(user.copy(preferences = updatedPreferences))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            SettingsSwitch(
                title = "Hide Reposts",
                subtitle = "Only show original content in your feed",
                checked = hideReposts,
                onCheckedChange = { checked ->
                    hideReposts = checked
                    uiState.user?.let { user ->
                        val updatedPreferences = updatePrivacySettings(user.preferences ?: mapOf(), "hideReposts", checked.toString())
                        viewModel.updateUser(user.copy(preferences = updatedPreferences))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Account Privacy",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedButton(
                onClick = { navController.navigate(Screen.BlockedAccounts.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Blocked Accounts")
            }

            Text(
                text = "These settings help control what content you see and who can interact with your content. Changes may take a few minutes to take effect across all devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun updatePrivacySettings(
    currentSettings: Map<String, String>,
    key: String,
    value: String
): Map<String, String> {
    return currentSettings.toMutableMap().apply {
        put(key, value)
    }
} 