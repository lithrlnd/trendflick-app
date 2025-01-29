package com.trendflick.data.repository

import com.trendflick.data.model.Message
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface MessageRepository {
    // Get messages for a conversation
    suspend fun getMessages(
        conversationDid: String,
        limit: Int = 50,
        before: Instant? = null
    ): Result<List<Message>>
    
    // Send a new message
    suspend fun sendMessage(
        recipientDid: String,
        text: String,
        replyTo: String? = null,
        embed: com.trendflick.data.model.Embed? = null
    ): Result<Message>
    
    // Delete a message
    suspend fun deleteMessage(uri: String): Result<Unit>
    
    // Get all conversations
    suspend fun getConversations(
        limit: Int = 20,
        before: Instant? = null
    ): Result<List<Conversation>>
    
    // Watch for new messages in real-time
    fun watchMessages(conversationDid: String): Flow<Message>
    
    // Watch for new conversations in real-time
    fun watchConversations(): Flow<List<Conversation>>
    
    // Mark messages as read
    suspend fun markAsRead(conversationDid: String, upTo: Instant): Result<Unit>
}

data class Conversation(
    val did: String,          // Conversation participant's DID
    val handle: String,       // Participant's handle
    val displayName: String?, // Participant's display name
    val avatar: String?,      // Participant's avatar URL
    val lastMessage: Message?,// Last message in conversation
    val unreadCount: Int,     // Number of unread messages
    val updatedAt: Instant    // Last update timestamp
) 