package com.trendflick.data.repository

import com.trendflick.data.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor() : MessageRepository {
    override suspend fun getMessages(
        conversationDid: String,
        limit: Int,
        before: Instant?
    ): Result<List<Message>> {
        // Return empty list until Bluesky messaging API is available
        return Result.success(emptyList())
    }

    override suspend fun sendMessage(
        recipientDid: String,
        text: String,
        replyTo: String?,
        embed: com.trendflick.data.model.Embed?
    ): Result<Message> {
        // Return error until Bluesky messaging API is available
        return Result.failure(
            UnsupportedOperationException("Messaging is not yet available in Bluesky")
        )
    }

    override suspend fun deleteMessage(uri: String): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Messaging is not yet available in Bluesky")
        )
    }

    override suspend fun getConversations(
        limit: Int,
        before: Instant?
    ): Result<List<Conversation>> {
        // Return empty list until Bluesky messaging API is available
        return Result.success(emptyList())
    }

    override fun watchMessages(conversationDid: String): Flow<Message> {
        // Return empty flow until Bluesky messaging API is available
        return flowOf()
    }

    override fun watchConversations(): Flow<List<Conversation>> {
        // Return empty flow until Bluesky messaging API is available
        return flowOf(emptyList())
    }

    override suspend fun markAsRead(conversationDid: String, upTo: Instant): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Messaging is not yet available in Bluesky")
        )
    }
} 