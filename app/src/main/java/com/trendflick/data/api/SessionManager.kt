package com.trendflick.data.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.trendflick.data.model.AtSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val TAG = "TF_Session"

    fun saveSession(session: AtSession) {
        Log.d(TAG, "💾 Saving session data")
        
        with(prefs.edit()) {
            putString(KEY_ACCESS_TOKEN, session.accessJwt)
            putString(KEY_REFRESH_TOKEN, session.refreshJwt)
            putString(KEY_DID, session.did)
            putString(KEY_HANDLE, session.handle)
            // Store expiration timestamp (24 hours from now as per AT Protocol docs)
            putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + (24 * 60 * 60 * 1000))
            apply()
        }
        
        Log.d(TAG, """
            ✅ Session saved:
            Handle: ${session.handle}
            DID: ${session.did}
            Expires: ${java.time.Instant.ofEpochMilli(System.currentTimeMillis() + (24 * 60 * 60 * 1000))}
        """.trimIndent())
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        
        return if (token != null && System.currentTimeMillis() < expiresAt) {
            Log.d(TAG, "✅ Valid access token found")
            token
        } else {
            Log.w(TAG, "⚠️ Access token expired or not found")
            null
        }
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)?.also { token ->
            Log.d(TAG, "✅ Refresh token found")
        } ?: run {
            Log.w(TAG, "⚠️ No refresh token found")
            null
        }
    }

    fun getDid(): String? {
        return prefs.getString(KEY_DID, null)?.also { did ->
            Log.d(TAG, "✅ DID found: $did")
        } ?: run {
            Log.w(TAG, "⚠️ No DID found")
            null
        }
    }

    fun getHandle(): String? {
        return prefs.getString(KEY_HANDLE, null)?.also { handle ->
            Log.d(TAG, "✅ Handle found: $handle")
        } ?: run {
            Log.w(TAG, "⚠️ No handle found")
            null
        }
    }

    fun hasValidSession(): Boolean {
        val accessToken = getAccessToken()
        val did = getDid()
        val handle = getHandle()
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        val isExpired = System.currentTimeMillis() >= expiresAt

        Log.d(TAG, """
            🔐 Session validation:
            Has token: ${accessToken != null}
            Has DID: ${did != null}
            Has handle: ${handle != null}
            Is expired: $isExpired
            Is valid: ${!isExpired && accessToken != null && did != null && handle != null}
        """.trimIndent())

        return !isExpired && accessToken != null && did != null && handle != null
    }

    fun clearSession() {
        Log.d(TAG, "🧹 Clearing session data")
        with(prefs.edit()) {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_DID)
            remove(KEY_HANDLE)
            remove(KEY_EXPIRES_AT)
            apply()
        }
        Log.d(TAG, "✅ Session data cleared")
    }

    companion object {
        private const val PREFS_NAME = "at_protocol_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_DID = "did"
        private const val KEY_HANDLE = "handle"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
} 