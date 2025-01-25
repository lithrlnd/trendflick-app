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
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val prefs = EncryptedSharedPreferences.create(
        "bluesky_credentials",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(handle: String, password: String) {
        Log.d("TF_Credentials", "💾 Saving credentials for handle: $handle")
        prefs.edit()
            .putString(KEY_HANDLE, handle)
            .putString(KEY_PASSWORD, password)
            .commit()
    }

    fun getCredentials(): Pair<String?, String?> {
        return try {
            val handle = getHandle()
            val password = getPassword()
            
            Log.d("TF_Credentials", "🔍 Retrieved credentials - Handle exists: ${!handle.isNullOrEmpty()}, Password exists: ${!password.isNullOrEmpty()}")
            
            if (handle.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.d("TF_Credentials", "⚠️ Incomplete credentials found - clearing")
                clearCredentials()
                Pair(null, null)
            } else {
                Pair(handle, password)
            }
        } catch (e: Exception) {
            Log.e("TF_Credentials", "❌ Error retrieving credentials: ${e.message}")
            clearCredentials()
            Pair(null, null)
        }
    }

    fun getHandle(): String? = try {
        prefs.getString(KEY_HANDLE, BuildConfig.BLUESKY_HANDLE)
    } catch (e: SecurityException) {
        Log.w("TF_Credentials", "⚠️ Could not decrypt handle: ${e.message}")
        sharedPreferences.getString(KEY_HANDLE, BuildConfig.BLUESKY_HANDLE)
    }
    
    fun getPassword(): String? = try {
        prefs.getString(KEY_PASSWORD, BuildConfig.BLUESKY_APP_PASSWORD)
    } catch (e: SecurityException) {
        Log.w("TF_Credentials", "⚠️ Could not decrypt password: ${e.message}")
        sharedPreferences.getString(KEY_PASSWORD, BuildConfig.BLUESKY_APP_PASSWORD)
    }
    
    fun getDid(): String? = sessionManager.getDid()
    
    fun getRefreshToken(): String? = sessionManager.getRefreshToken()
    
    fun clearCredentials() {
        Log.d("TF_Credentials", "🗑️ Starting BlueSky credentials cleanup")
        
        try {
            // 1. Clear session data first (this includes PDS info)
            sessionManager.clearSession()
            Log.d("TF_Credentials", "✅ Session data cleared")
            
            // 2. Clear shared preferences directly if encrypted prefs fail
            try {
                prefs.edit().clear().commit()
                Log.d("TF_Credentials", "✅ Encrypted credentials cleared")
            } catch (e: SecurityException) {
                Log.w("TF_Credentials", "⚠️ Could not clear encrypted prefs: ${e.message}")
                // Fallback to clearing regular shared preferences
                sharedPreferences.edit()
                    .remove(KEY_HANDLE)
                    .remove(KEY_PASSWORD)
                    .commit()
                Log.d("TF_Credentials", "✅ Fallback: credentials cleared from regular prefs")
            }
            
            // 3. Double check everything is cleared
            if (getHandle() != null || getPassword() != null) {
                Log.w("TF_Credentials", "⚠️ Credentials still present after clear, forcing removal")
                try {
                    // Force clear both storage locations
                    prefs.edit().clear().commit()
                    sharedPreferences.edit().clear().commit()
                } catch (e: Exception) {
                    Log.e("TF_Credentials", "❌ Final cleanup attempt failed: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e("TF_Credentials", """
                ❌ Error during credentials cleanup:
                Type: ${e.javaClass.name}
                Message: ${e.message}
            """.trimIndent())
            
            // Last resort: try to clear everything we can
            try {
                sessionManager.clearSession()
                sharedPreferences.edit().clear().commit()
                prefs.edit().clear().commit()
            } catch (e2: Exception) {
                Log.e("TF_Credentials", "❌ Emergency cleanup failed: ${e2.message}")
            }
        }
    }

    fun hasValidCredentials(): Boolean {
        try {
            val (handle, password) = getCredentials()
            val hasCredentials = !handle.isNullOrEmpty() && !password.isNullOrEmpty()
            
            // Check session state first
            val hasSession = sessionManager.hasValidSession()
            
            Log.d("TF_Credentials", """
                🔐 Checking auth state:
                Handle present: ${!handle.isNullOrEmpty()}
                Password present: ${!password.isNullOrEmpty()}
                Session valid: $hasSession
                Overall valid: ${hasCredentials && hasSession}
            """.trimIndent())
            
            return hasCredentials && hasSession
        } catch (e: SecurityException) {
            Log.e("TF_Credentials", "❌ Security error checking credentials: ${e.message}")
            // If we can't decrypt, consider credentials invalid
            clearCredentials() // Clean up any corrupted state
            return false
        } catch (e: Exception) {
            Log.e("TF_Credentials", "❌ Error checking credentials: ${e.message}")
            return false
        }
    }

    companion object {
        private const val KEY_HANDLE = "handle"
        private const val KEY_PASSWORD = "password"
    }
} 