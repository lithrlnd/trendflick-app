package com.trendflick.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.trendflick.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.trendflick.data.api.SessionManager
import android.content.SharedPreferences
import android.util.Log

@Singleton
class BlueskyCredentialsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val sharedPreferences: SharedPreferences
) {
    private val TAG = "TF_CredentialsManager"
    private val masterKeyAlias by lazy {
        try {
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        } catch (e: Exception) {
            Log.e("TF_Credentials", "❌ Failed to create master key: ${e.message}")
            null
        }
    }
    
    private val prefs by lazy {
        try {
            masterKeyAlias?.let { keyAlias ->
                EncryptedSharedPreferences.create(
                    "bluesky_credentials",
                    keyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } ?: sharedPreferences
        } catch (e: Exception) {
            Log.e("TF_Credentials", "❌ Failed to create encrypted prefs: ${e.message}")
            sharedPreferences
        }
    }

    fun saveCredentials(handle: String, password: String) {
        Log.d(TAG, "💾 Saving credentials for handle: $handle")
        try {
            // Try to save in encrypted storage first
            prefs.edit()
                .putString(KEY_HANDLE, handle)
                .putString(KEY_PASSWORD, password)
                .commit()
            
            // Clear any old unencrypted data
            if (prefs !== sharedPreferences) {
                sharedPreferences.edit()
                    .remove(KEY_HANDLE)
                    .remove(KEY_PASSWORD)
                    .commit()
            }
        } catch (e: Exception) {
            Log.e("TF_Credentials", "❌ Failed to save credentials: ${e.message}")
            // Fallback to unencrypted storage
            sharedPreferences.edit()
                .putString(KEY_HANDLE, handle)
                .putString(KEY_PASSWORD, password)
                .commit()
        }
    }

    fun getCredentials(): Pair<String?, String?> {
        val handle = getHandle()
        val password = getPassword()
        Log.d(TAG, """
            🔑 Getting credentials:
            Handle exists: ${!handle.isNullOrEmpty()}
            Password exists: ${!password.isNullOrEmpty()}
        """.trimIndent())
        return Pair(handle, password)
    }

    fun getHandle(): String? = try {
        val handle = prefs.getString(KEY_HANDLE, null) ?: sharedPreferences.getString(KEY_HANDLE, BuildConfig.BLUESKY_HANDLE)
        Log.d(TAG, "🔍 Retrieved handle: ${handle ?: "null"}")
        handle
    } catch (e: Exception) {
        Log.w("TF_Credentials", "⚠️ Could not get handle: ${e.message}")
        sharedPreferences.getString(KEY_HANDLE, BuildConfig.BLUESKY_HANDLE)
    }
    
    fun getPassword(): String? = try {
        val password = prefs.getString(KEY_PASSWORD, null) ?: sharedPreferences.getString(KEY_PASSWORD, BuildConfig.BLUESKY_APP_PASSWORD)
        Log.d(TAG, "🔍 Retrieved password: ${if (password != null) "****" else "null"}")
        password
    } catch (e: Exception) {
        Log.w("TF_Credentials", "⚠️ Could not get password: ${e.message}")
        sharedPreferences.getString(KEY_PASSWORD, BuildConfig.BLUESKY_APP_PASSWORD)
    }
    
    fun getDid(): String? = sessionManager.getDid()
    
    fun getRefreshToken(): String? = sessionManager.getRefreshToken()
    
    fun clearCredentials() {
        Log.d(TAG, "🧹 Clearing all credentials")
        
        try {
            // 1. Clear session data first
            sessionManager.clearSession()
            Log.d("TF_Credentials", "✅ Session data cleared")
            
            // 2. Clear both storage locations
            var clearedAny = false
            
            try {
                if (prefs !== sharedPreferences) {
                    prefs.edit().clear().commit()
                    clearedAny = true
                    Log.d("TF_Credentials", "✅ Encrypted storage cleared")
                }
            } catch (e: Exception) {
                Log.w("TF_Credentials", "⚠️ Failed to clear encrypted storage: ${e.message}")
            }
            
            try {
                sharedPreferences.edit()
                    .remove(KEY_HANDLE)
                    .remove(KEY_PASSWORD)
                    .commit()
                clearedAny = true
                Log.d("TF_Credentials", "✅ Unencrypted storage cleared")
            } catch (e: Exception) {
                Log.w("TF_Credentials", "⚠️ Failed to clear unencrypted storage: ${e.message}")
            }
            
            if (!clearedAny) {
                Log.e("TF_Credentials", "❌ Failed to clear any storage location")
            }
            
            // 3. Verify cleanup
            if (getHandle() != null || getPassword() != null) {
                Log.w("TF_Credentials", "⚠️ Credentials still present after clear")
                // One final attempt with both clear() and remove()
                try {
                    prefs.edit().clear().commit()
                    sharedPreferences.edit().clear().commit()
                    Log.d("TF_Credentials", "✅ Final cleanup completed")
                } catch (e: Exception) {
                    Log.e("TF_Credentials", "❌ Final cleanup failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("TF_Credentials", "❌ Error during cleanup: ${e.message}")
        }
    }

    fun hasValidCredentials(): Boolean {
        val (handle, password) = getCredentials()
        val hasCredentials = !handle.isNullOrEmpty() && !password.isNullOrEmpty()
        val hasSession = sessionManager.hasValidSession()
        
        Log.d(TAG, """
            🔐 Checking auth state:
            Handle present: ${!handle.isNullOrEmpty()}
            Password present: ${!password.isNullOrEmpty()}
            Session valid: $hasSession
            Overall valid: ${hasCredentials && hasSession}
        """.trimIndent())
        
        return hasCredentials && hasSession
    }

    fun saveDid(did: String) {
        Log.d(TAG, "💾 Saving DID: $did")
        prefs.edit().putString("did", did).apply()
    }

    companion object {
        private const val KEY_HANDLE = "handle"
        private const val KEY_PASSWORD = "password"
    }
} 