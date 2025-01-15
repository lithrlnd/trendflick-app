package com.trendflick.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.trendflick.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlueskyCredentialsManager @Inject constructor(
    @ApplicationContext private val context: Context
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
        prefs.edit()
            .putString(KEY_HANDLE, handle)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun getHandle(): String? = prefs.getString(KEY_HANDLE, BuildConfig.BLUESKY_HANDLE)
    
    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, BuildConfig.BLUESKY_APP_PASSWORD)
    
    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_HANDLE = "handle"
        private const val KEY_PASSWORD = "password"
    }
} 