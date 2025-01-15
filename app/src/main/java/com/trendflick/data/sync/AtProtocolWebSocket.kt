package com.trendflick.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AtProtocolWebSocket @Inject constructor(
    private val client: OkHttpClient
) {
    fun subscribeToRepos(): Flow<RepoEvent> = flow {
        val request = Request.Builder()
            .url("wss://bsky.social/xrpc/com.atproto.sync.subscribeRepos")
            .build()

        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Parse and emit repo events
            }
        }

        client.newWebSocket(request, listener)
    }
}

sealed class RepoEvent {
    data class Commit(
        val seq: Long,
        val repoHandle: String,
        val did: String,
        val records: List<Record>
    ) : RepoEvent()

    data class Handle(
        val seq: Long,
        val did: String,
        val handle: String
    ) : RepoEvent()

    data class Migrate(
        val seq: Long,
        val did: String,
        val migrateTo: String
    ) : RepoEvent()
}

data class Record(
    val uri: String,
    val cid: String,
    val action: String // create, update, delete
) 