package com.trendflick.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.trendflick.data.api.*
import com.trendflick.data.local.UserDao
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.repository.AtProtocolRepositoryImpl
import com.trendflick.data.auth.BlueskyCredentialsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.JsonDataException
import com.trendflick.data.api.SessionManager
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import android.util.Log
import android.content.SharedPreferences

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TAG = "AT Protocol"
    
    private const val BASE_URL = "https://bsky.social/"
    
    class FacetAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Facet? {
            try {
                var index: FacetIndex? = null
                var features: MutableList<FacetFeature> = mutableListOf()
                
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "index" -> {
                            reader.beginObject()
                            var start: Int? = null
                            var end: Int? = null
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "start", "byteStart" -> start = reader.nextInt()
                                    "end", "byteEnd" -> end = reader.nextInt()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            if (start != null && end != null) {
                                index = FacetIndex(start, end)
                            }
                        }
                        "features" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var type: String? = null
                                var uri: String? = null
                                var did: String? = null
                                var tag: String? = null
                                
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "\$type" -> type = reader.nextString()
                                        "uri" -> uri = reader.nextString()
                                        "did" -> did = reader.nextString()
                                        "tag" -> tag = reader.nextString()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                
                                when {
                                    type?.contains("mention") == true && did != null -> {
                                        features.add(MentionFeature(did = did))
                                    }
                                    type?.contains("link") == true && uri != null -> {
                                        features.add(LinkFeature(uri = uri))
                                    }
                                    type?.contains("tag") == true && tag != null -> {
                                        features.add(TagFeature(tag = tag))
                                    }
                                }
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                
                return if (index != null) {
                    Facet(index = index, features = features)
                } else {
                    null
                }
            } catch (e: Exception) {
                return null
            }
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Facet?) {
            if (value == null) {
                writer.nullValue()
                return
            }
            
            writer.beginObject()
            writer.name("index")
            writer.beginObject()
            writer.name("start").value(value.index.start)
            writer.name("end").value(value.index.end)
            writer.endObject()
            
            writer.name("features")
            writer.beginArray()
            value.features.forEach { feature ->
                writer.beginObject()
                writer.name("\$type").value(feature.type)
                when (feature) {
                    is MentionFeature -> writer.name("did").value(feature.did)
                    is LinkFeature -> writer.name("uri").value(feature.uri)
                    is TagFeature -> writer.name("tag").value(feature.tag)
                }
                writer.endObject()
            }
            writer.endArray()
            writer.endObject()
        }
    }
    
    class ExternalEmbedAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): ExternalEmbed {
            var uri: String? = null
            var title: String? = null
            var description: String? = null
            var thumbUrl: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "uri" -> uri = reader.nextString()
                    "title" -> title = reader.nextString()
                    "description" -> {
                        if (reader.peek() == JsonReader.Token.NULL) {
                            reader.nextNull<String>()
                            description = null
                        } else {
                            description = reader.nextString()
                        }
                    }
                    "thumb" -> {
                        if (reader.peek() == JsonReader.Token.STRING) {
                            thumbUrl = reader.nextString()
                        } else if (reader.peek() != JsonReader.Token.NULL) {
                            reader.skipValue()
                        } else {
                            reader.nextNull<String>()
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            if (uri == null) {
                throw JsonDataException("Required field 'uri' missing for ExternalEmbed")
            }

            return ExternalEmbed(
                uri = uri,
                title = title,
                description = description,
                thumbUrl = thumbUrl
            )
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: ExternalEmbed) {
            writer.beginObject()
            writer.name("uri").value(value.uri)
            writer.name("title").value(value.title)
            value.description?.let { writer.name("description").value(it) }
            value.thumbUrl?.let { writer.name("thumb").value(it) }
            writer.endObject()
        }
    }

    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        sessionManager: SessionManager
    ): AuthInterceptor {
        return AuthInterceptor(sessionManager).apply {
            setTokenType("Bearer")  // AT Protocol requires Bearer token type
        }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d("TF_Network", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(RecordJsonAdapter.FACTORY)
            .add(FacetFeatureJsonAdapter.FACTORY)
            .add(FeatureJsonAdapter.FACTORY)
            .add(FacetAdapter())
            .add(ExternalEmbedAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideAtProtocolService(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): AtProtocolService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AtProtocolService::class.java)
    }

    @Provides
    @Singleton
    fun provideAtProtocolRepository(
        service: AtProtocolService,
        userDao: UserDao,
        @ApplicationContext context: Context,
        sessionManager: SessionManager,
        credentialsManager: BlueskyCredentialsManager
    ): AtProtocolRepository {
        return AtProtocolRepositoryImpl(service, userDao, context, sessionManager, credentialsManager)
    }

    private fun createBlobRef(link: String?, mimeType: String?, size: Long?): BlobRef {
        return BlobRef(
            link = link,
            mimeType = mimeType,
            size = size
        )
    }

    private fun createExternalEmbed(
        uri: String,
        title: String?,
        description: String?,
        thumbUrl: String?
    ): ExternalEmbed {
        return ExternalEmbed(
            uri = uri,
            title = title,
            description = description,
            thumbUrl = thumbUrl
        )
    }
} 