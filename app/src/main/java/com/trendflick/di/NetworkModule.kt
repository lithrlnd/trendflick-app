package com.trendflick.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.trendflick.data.api.*
import com.trendflick.data.local.UserDao
import com.trendflick.data.repository.AtProtocolRepository
import com.trendflick.data.repository.AtProtocolRepositoryImpl
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

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TAG = "AT Protocol"
    
    private const val BASE_URL = "https://bsky.social/"
    
    class FacetAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Facet? {
            try {
                var index: TextRange? = null
                var features: MutableList<FacetFeature> = mutableListOf()
                
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "index" -> {
                            try {
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
                                    index = TextRange(start, end)
                                }
                            } catch (e: Exception) {
                                reader.skipValue()
                            }
                        }
                        "features" -> {
                            try {
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    reader.beginObject()
                                    var uri: String? = null
                                    var did: String? = null
                                    var tag: String? = null
                                    var type: String? = null
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
                                    features.add(FacetFeature(type, uri, did, tag))
                                }
                                reader.endArray()
                            } catch (e: Exception) {
                                reader.skipValue()
                            }
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
                feature.type?.let { writer.name("\$type").value(it) }
                feature.uri?.let { writer.name("uri").value(it) }
                feature.did?.let { writer.name("did").value(it) }
                feature.tag?.let { writer.name("tag").value(it) }
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
            var thumbBlob: BlobRef? = null

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
                            reader.beginObject()
                            var type: String? = null
                            var link: String? = null
                            var mimeType: String? = null
                            var size: Long? = null
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "\$type" -> type = reader.nextString()
                                    "\$link" -> link = reader.nextString()
                                    "mimeType" -> mimeType = reader.nextString()
                                    "size" -> size = reader.nextLong()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            thumbBlob = BlobRef(type, link, mimeType, size)
                        } else {
                            reader.nextNull<String>()
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            if (uri == null || title == null) {
                throw JsonDataException("Required fields missing for ExternalEmbed")
            }

            return ExternalEmbed(uri, title, description, thumbUrl, thumbBlob)
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: ExternalEmbed) {
            writer.beginObject()
            writer.name("uri").value(value.uri)
            writer.name("title").value(value.title)
            value.description?.let { writer.name("description").value(it) }
            value.thumbUrl?.let { writer.name("thumb").value(it) }
            value.thumbBlob?.let { blob ->
                writer.name("thumb")
                writer.beginObject()
                blob.type?.let { writer.name("\$type").value(it) }
                blob.link?.let { writer.name("\$link").value(it) }
                blob.mimeType?.let { writer.name("mimeType").value(it) }
                blob.size?.let { writer.name("size").value(it) }
                writer.endObject()
            }
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
    fun provideCredentialsManager(
        @ApplicationContext context: Context
    ): CredentialsManager {
        return CredentialsManager(context)
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
        credentialsManager: CredentialsManager
    ): AtProtocolRepository {
        return AtProtocolRepositoryImpl(service, userDao, context, sessionManager, credentialsManager)
    }
} 