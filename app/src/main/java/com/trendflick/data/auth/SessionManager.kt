package com.trendflick.data.auth

import android.content.SharedPreferences
import com.trendflick.data.model.AtSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_DID = "did"
        private const val KEY_HANDLE = "handle"
    }

    fun saveSession(session: AtSession) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessJwt)
            .putString(KEY_REFRESH_TOKEN, session.refreshJwt)
            .putString(KEY_DID, session.did)
            .putString(KEY_HANDLE, session.handle)
            .apply()
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_DID)
            .remove(KEY_HANDLE)
            .apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun getDid(): String? {
        return sharedPreferences.getString(KEY_DID, null)
    }

    fun getHandle(): String? {
        return sharedPreferences.getString(KEY_HANDLE, null)
    }

    fun hasValidSession(): Boolean {
        val accessToken = getAccessToken()
        val did = getDid()
        val handle = getHandle()
        return !accessToken.isNullOrEmpty() && !did.isNullOrEmpty() && !handle.isNullOrEmpty()
    }
} 