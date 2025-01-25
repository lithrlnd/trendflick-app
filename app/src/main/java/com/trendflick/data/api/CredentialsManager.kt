package com.trendflick.data.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsManager @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val TAG = "TF_Credentials"

    fun saveCredentials(handle: String, password: String) {
        Log.d(TAG, "💾 Saving credentials for handle: $handle")
        prefs.edit()
            .putString(KEY_HANDLE, handle)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun getHandle(): String? {
        return prefs.getString(KEY_HANDLE, null)?.also { handle ->
            Log.d(TAG, "✅ Handle retrieved: $handle")
        } ?: run {
            Log.w(TAG, "⚠️ No handle found")
            null
        }
    }

    fun getPassword(): String? {
        return prefs.getString(KEY_PASSWORD, null)?.also {
            Log.d(TAG, "✅ Password retrieved")
        } ?: run {
            Log.w(TAG, "⚠️ No password found")
            null
        }
    }

    fun clearCredentials() {
        Log.d(TAG, "🗑️ Clearing credentials")
        prefs.edit().clear().apply()
    }

    fun hasValidCredentials(): Boolean {
        val hasHandle = !getHandle().isNullOrEmpty()
        val hasPassword = !getPassword().isNullOrEmpty()
        
        Log.d(TAG, """
            🔐 Credentials check:
            Has handle: $hasHandle
            Has password: $hasPassword
            Is valid: ${hasHandle && hasPassword}
        """.trimIndent())
        
        return hasHandle && hasPassword
    }

    companion object {
        private const val PREFS_NAME = "bluesky_credentials"
        private const val KEY_HANDLE = "handle"
        private const val KEY_PASSWORD = "password"
    }
} 