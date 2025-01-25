package com.trendflick.ui.screens.ai

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    navController: NavController,
    viewModel: AIViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    var userInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.padding(bottom = 80.dp),  // Add padding for bottom nav
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Social Media Advisor",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear chat"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (isLoading) 16.dp else 0.dp),
                            placeholder = { Text("Ask about social media content...") },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (userInput.isNotBlank() && !isLoading) {
                                            scope.launch {
                                                viewModel.sendMessage(userInput)
                                                userInput = ""
                                                listState.animateScrollToItem(messages.size)
                                            }
                                        }
                                    },
                                    enabled = userInput.isNotBlank() && !isLoading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (userInput.isNotBlank() && !isLoading) {
                                        scope.launch {
                                            viewModel.sendMessage(userInput)
                                            userInput = ""
                                            listState.animateScrollToItem(messages.size)
                                        }
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isLoading
                        )
                        
                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = listState
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
            }
            
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isAssistant = message.role == ChatRole.Assistant
    val messageContent = message.content ?: return
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAssistant) Arrangement.Start else Arrangement.End
    ) {
        if (isAssistant) {
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Assistant,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = if (isAssistant) 4.dp else 16.dp,
                topEnd = if (isAssistant) 16.dp else 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (isAssistant) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp
        ) {
            Text(
                text = messageContent,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isAssistant) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        if (!isAssistant) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
} 