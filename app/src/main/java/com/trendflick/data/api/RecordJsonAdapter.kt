package com.trendflick.data.api

import com.squareup.moshi.*
import java.lang.reflect.Type

class RecordJsonAdapter(
    private val moshi: Moshi
) : JsonAdapter<AtProtocolService.Record>() {
    
    private val options: JsonReader.Options = JsonReader.Options.of("\$type")
    
    // Adapters for each concrete type
    private val postAdapter by lazy { moshi.adapter(AtProtocolService.PostRecord::class.java) }
    private val repostAdapter by lazy { moshi.adapter(AtProtocolService.RepostRecord::class.java) }

    override fun fromJson(reader: JsonReader): AtProtocolService.Record? {
        reader.beginObject()
        var type: String? = null
        
        // Find the $type field first
        while (reader.hasNext()) {
            when (val index = reader.selectName(options)) {
                0 -> type = reader.nextString()
                -1 -> {
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        
        // Reset reader to start
        reader.beginObject()
        
        // Create appropriate type based on $type
        val result = when (type) {
            "app.bsky.feed.post" -> postAdapter.fromJson(reader)
            "app.bsky.feed.repost" -> repostAdapter.fromJson(reader)
            else -> throw JsonDataException("Unknown record type: $type")
        }
        
        reader.endObject()
        return result
    }

    override fun toJson(writer: JsonWriter, value: AtProtocolService.Record?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        when (value) {
            is AtProtocolService.PostRecord -> postAdapter.toJson(writer, value)
            is AtProtocolService.RepostRecord -> repostAdapter.toJson(writer, value)
        }
    }

    companion object {
        val FACTORY = object : JsonAdapter.Factory {
            override fun create(
                type: Type,
                annotations: Set<Annotation>,
                moshi: Moshi
            ): JsonAdapter<*>? {
                return if (type === AtProtocolService.Record::class.java) {
                    RecordJsonAdapter(moshi)
                } else {
                    null
                }
            }
        }
    }
} 