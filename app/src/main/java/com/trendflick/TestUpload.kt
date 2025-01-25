package com.trendflick

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object TestUpload {
    private const val TAG = "TF_Test"
    
    suspend fun testStorageAccess() {
        try {
            val storage = FirebaseStorage.getInstance()
            Log.d(TAG, "🔥 Testing with bucket: ${storage.app.options.storageBucket}")
            
            // Try to create a simple text file at root level
            val testRef = storage.reference.child("test.txt")
            val data = "Hello Firebase".toByteArray()
            
            testRef.putBytes(data).await()
            Log.d(TAG, "✅ Test upload successful!")
            
            // Try to read it back
            val url = testRef.downloadUrl.await()
            Log.d(TAG, "✅ Test download URL: $url")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Test failed: ${e.message}")
            Log.e(TAG, "Stack: ${e.stackTraceToString()}")
        }
    }
} 