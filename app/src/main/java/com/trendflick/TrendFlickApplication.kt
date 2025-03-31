package com.trendflick

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@HiltAndroidApp
class TrendFlickApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("TF_STARTUP", """
            🚀 Application Starting:
            - Package: ${packageName}
            - Process: ${android.os.Process.myPid()}
            - Thread: ${Thread.currentThread().name}
        """.trimIndent())
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            Log.d("TF_App", "🔄 Starting Firebase initialization...")
            
            // Initialize Firebase with default config from google-services.json
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("TF_App", "✅ Firebase App initialized")
            }
            
            // Initialize Auth, Storage, and Firestore
            val auth = FirebaseAuth.getInstance()
            val storage = FirebaseStorage.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            
            // Enable Crashlytics and Analytics
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
            Firebase.analytics.setAnalyticsCollectionEnabled(true)
            
            Log.d("TF_App", """
                ✅ Firebase services initialized:
                📱 App ID: ${FirebaseApp.getInstance().options.applicationId}
                🔑 Project ID: ${FirebaseApp.getInstance().options.projectId}
                📦 Storage bucket: ${storage.app.options.storageBucket}
                👤 Auth initialized: ${auth != null}
                💾 Firestore initialized: ${firestore != null}
            """.trimIndent())
            
        } catch (e: Exception) {
            Log.e("TF_App", """
                ❌ Firebase initialization failed:
                - Error: ${e.message}
                - Type: ${e.javaClass.name}
                - Stack: ${e.stackTraceToString()}
            """.trimIndent())
        }
    }

    companion object {
        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        fun getVideoCache(context: Context): Cache {
            val cacheSize = 90 * 1024 * 1024L // 90MB
            val cacheDir = File(context.cacheDir, "video_cache")
            return SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(cacheSize),
                StandaloneDatabaseProvider(context)
            )
        }
    }
} 