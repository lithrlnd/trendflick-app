package com.trendflick.di

import android.content.Context
import android.content.SharedPreferences
import com.trendflick.data.auth.BlueskyCredentialsManager
import com.trendflick.data.api.SessionManager
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    // Keep only AT Protocol related providers
    // ... existing code ...

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        // Enable anonymous auth if no user is signed in
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously()
                    .addOnSuccessListener { result ->
                        Log.d("TF_Auth", """
                            ✅ Anonymous auth successful:
                            User ID: ${result.user?.uid}
                            Is New User: ${result.additionalUserInfo?.isNewUser}
                        """.trimIndent())
                    }
                    .addOnFailureListener { e ->
                        Log.e("TF_Auth", """
                            ❌ Anonymous auth failed:
                            Error: ${e.message}
                            Type: ${e.javaClass.name}
                        """.trimIndent())
                    }
            } catch (e: Exception) {
                Log.e("TF_Auth", """
                    ❌ Error during anonymous auth:
                    Error: ${e.message}
                    Stack: ${e.stackTraceToString()}
                """.trimIndent())
            }
        } else {
            Log.d("TF_Auth", """
                ✅ Using existing auth:
                User ID: ${auth.currentUser?.uid}
                Is Anonymous: ${auth.currentUser?.isAnonymous}
            """.trimIndent())
        }
        return auth
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideBlueskyCredentialsManager(
        @ApplicationContext context: Context,
        sessionManager: SessionManager,
        sharedPreferences: SharedPreferences
    ): BlueskyCredentialsManager {
        return BlueskyCredentialsManager(context, sessionManager, sharedPreferences)
    }
} 