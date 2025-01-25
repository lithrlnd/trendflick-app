package com.trendflick.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.auth.BlueskyCredentialsManager
import com.trendflick.data.api.SessionManager
import com.trendflick.data.model.AtSession
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log
import com.trendflick.BuildConfig

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val atProtocolRepository: AtProtocolRepository,
    private val credentialsManager: BlueskyCredentialsManager,
    private val sessionManager: SessionManager,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val TAG = "TF_Auth"
    private val _loginState = MutableStateFlow<LoginState>(LoginState())
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState(isLoading = true)
                
                Log.d(TAG, "🔄 Starting login process via bsky.social Entryway")
                
                // Always use bsky.social as the entry point
                val fullHandle = "$username.bsky.social"
                Log.d(TAG, "🔐 Attempting login with handle: $fullHandle via Entryway")
                
                atProtocolRepository.createSession(fullHandle, password)
                    .onSuccess { session ->
                        Log.d(TAG, """
                            ✅ BlueSky session created via Entryway:
                            DID: ${session.did}
                            Handle: ${session.handle}
                            Access Token Valid: ${session.accessJwt.isNotEmpty()}
                            Refresh Token Present: ${session.refreshJwt.isNotEmpty()}
                        """.trimIndent())
                        
                        // First store the session which includes PDS info
                        sessionManager.saveSession(session)
                        Log.d(TAG, "✅ Session saved with PDS information")
                        
                        // Then store credentials securely
                        credentialsManager.saveCredentials(fullHandle, password)
                        Log.d(TAG, "✅ Credentials saved securely")
                        
                        // Firebase auth is secondary - only for app-specific features
                        try {
                            if (firebaseAuth.currentUser == null) {
                                firebaseAuth.signInAnonymously().await()
                                Log.d(TAG, "✅ Firebase anonymous auth completed (for app features)")
                            }
                        } catch (e: Exception) {
                            // Don't fail login if Firebase fails - it's secondary
                            Log.w(TAG, "⚠️ Firebase auth failed (non-critical): ${e.message}")
                        }
                        
                        // Update login state based on BlueSky session
                        _loginState.value = LoginState(isLoggedIn = true, session = session)
                        Log.d(TAG, "✅ Login completed - ready for PDS communication")
                    }
                    .onFailure { error ->
                        Log.e(TAG, """
                            ❌ BlueSky Entryway login failed:
                            Error: ${error.message}
                            Stack trace: ${error.stackTraceToString()}
                        """.trimIndent())
                        _loginState.value = LoginState(error = "Login failed: ${error.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ Critical login error:
                    Type: ${e.javaClass.name}
                    Message: ${e.message}
                    Stack trace: ${e.stackTraceToString()}
                """.trimIndent())
                _loginState.value = LoginState(error = "Login failed: ${e.message}")
            }
        }
    }

    init {
        // Check if we have a valid session
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Checking for existing session")
                
                if (sessionManager.hasValidSession()) {
                    val did = sessionManager.getDid()
                    val handle = sessionManager.getHandle()
                    val accessToken = sessionManager.getAccessToken()
                    val refreshToken = sessionManager.getRefreshToken()
                    
                    if (did != null && handle != null && accessToken != null) {
                        Log.d(TAG, """
                            ✅ Found valid session:
                            DID: $did
                            Handle: $handle
                            Has Refresh Token: ${refreshToken != null}
                        """.trimIndent())
                        
                        _loginState.value = LoginState(
                            isLoggedIn = true,
                            session = AtSession(
                                did = did,
                                handle = handle,
                                accessJwt = accessToken,
                                refreshJwt = refreshToken ?: "" // Use empty string if null
                            )
                        )
                    } else {
                        Log.w(TAG, "⚠️ Invalid session state - missing required fields")
                        sessionManager.clearSession()
                        _loginState.value = LoginState(isLoggedIn = false)
                    }
                } else if (credentialsManager.hasValidCredentials()) {
                    Log.d(TAG, "🔄 Found credentials, attempting to restore session")
                    
                    val (handle, password) = credentialsManager.getCredentials()
                    
                    if (handle != null && password != null) {
                        login(handle.removeSuffix(".bsky.social"), password)
                    } else {
                        Log.w(TAG, "⚠️ Invalid credentials found")
                        credentialsManager.clearCredentials()
                        _loginState.value = LoginState(isLoggedIn = false)
                    }
                } else {
                    Log.d(TAG, "ℹ️ No existing session or credentials found")
                    _loginState.value = LoginState(isLoggedIn = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ Error checking session:
                    Type: ${e.javaClass.name}
                    Message: ${e.message}
                    Stack trace: ${e.stackTraceToString()}
                """.trimIndent())
                _loginState.value = LoginState(isLoggedIn = false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Starting BlueSky AT Protocol logout process")
                
                // First invalidate the session with BlueSky
                val currentSession = _loginState.value.session
                if (currentSession != null) {
                    try {
                        atProtocolRepository.deleteSession(currentSession.refreshJwt)
                            .onSuccess {
                                Log.d(TAG, "✅ BlueSky session successfully invalidated")
                            }
                            .onFailure { error ->
                                Log.w(TAG, "⚠️ Failed to invalidate BlueSky session: ${error.message}")
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error invalidating BlueSky session: ${e.message}")
                    }
                }

                // Sign out from Firebase (if used)
                firebaseAuth.signOut()
                Log.d(TAG, "✅ Firebase sign out complete")
                
                // Clear all local data in order:
                // 1. Clear BlueSky credentials
                credentialsManager.clearCredentials()
                Log.d(TAG, "✅ BlueSky credentials cleared")
                
                // 2. Clear session data
                sessionManager.clearSession()
                Log.d(TAG, "✅ BlueSky session cleared")
                
                // 3. Clear any cached data
                atProtocolRepository.clearLikeCache()
                Log.d(TAG, "✅ Cache cleared")
                
                // Finally update the UI state
                _loginState.value = LoginState(isLoggedIn = false, session = null)
                
                Log.d(TAG, "✅ AT Protocol logout completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, """
                    ❌ AT Protocol logout error:
                    Type: ${e.javaClass.name}
                    Message: ${e.message}
                    Stack trace: ${e.stackTraceToString()}
                """.trimIndent())
                _loginState.value = LoginState(error = "Logout failed: ${e.message}")
            }
        }
    }
}

data class LoginState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val session: AtSession? = null,
    val error: String? = null
) 