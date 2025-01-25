package com.trendflick.di

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.repository.VideoRepository
import com.trendflick.data.repository.VideoRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideContentResolver(
        @ApplicationContext context: Context
    ): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val storage = FirebaseStorage.getInstance()
        Log.d("TF_DI", """
            🔧 Providing Firebase Storage:
            - Bucket: ${storage.app.options.storageBucket}
            - Project ID: ${storage.app.options.projectId}
        """.trimIndent())
        return storage
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideVideoRepository(
        storage: FirebaseStorage,
        firestore: FirebaseFirestore,
        contentResolver: ContentResolver,
        @ApplicationContext context: Context,
        atProtocolRepository: AtProtocolRepository
    ): VideoRepository = VideoRepositoryImpl(storage, firestore, contentResolver, context, atProtocolRepository)
} 