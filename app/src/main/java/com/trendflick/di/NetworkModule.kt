package com.trendflick.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.trendflick.data.api.AtProtocolService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TAG = "AT Protocol"
    
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                
                // Don't modify the Content-Type if it's already set by Retrofit
                val newRequest = if (originalRequest.body?.contentType() == null) {
                    originalRequest.newBuilder()
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .build()
                } else {
                    originalRequest
                }

                System.out.println("AT Protocol Network - x-powered-by: ${newRequest.header("x-powered-by")}")
                System.out.println("AT Protocol Network - access-control-allow-origin: ${newRequest.header("access-control-allow-origin")}")
                System.out.println("AT Protocol Network - vary: ${newRequest.header("vary")}")
                System.out.println("AT Protocol Network - Content-Type: ${newRequest.body?.contentType()}")
                System.out.println("AT Protocol Network - Request URL: ${newRequest.url}")
                System.out.println("AT Protocol Network - Method: ${newRequest.method}")
                System.out.println("AT Protocol Network - Headers: ${newRequest.headers}")
                
                val response = chain.proceed(newRequest)
                
                if (!response.isSuccessful) {
                    val errorBody = response.peekBody(Long.MAX_VALUE).string()
                    System.err.println("AT Protocol - Error type: HttpException")
                    System.err.println("AT Protocol - Error message: HTTP ${response.code}")
                    System.err.println("AT Protocol - Error Body: $errorBody")
                    System.err.println("AT Protocol - Stack trace:")
                } else {
                    System.out.println("AT Protocol Network - <-- END HTTP (${response.body?.contentLength()}-byte body)")
                }
                
                response
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://bsky.social/xrpc/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
    }

    @Provides
    @Singleton
    fun provideAtProtocolService(retrofit: Retrofit): AtProtocolService {
        return retrofit.create(AtProtocolService::class.java)
    }
} 