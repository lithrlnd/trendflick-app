package com.trendflick.data.api

import com.squareup.moshi.*
import java.lang.reflect.Type

class FeatureJsonAdapter(
    private val moshi: Moshi
) : JsonAdapter<AtProtocolService.Feature>() {
    
    private val options: JsonReader.Options = JsonReader.Options.of("\$type")
    
    // Adapters for each concrete type
    private val mentionAdapter by lazy { moshi.adapter(AtProtocolService.Feature.Mention::class.java) }
    private val linkAdapter by lazy { moshi.adapter(AtProtocolService.Feature.Link::class.java) }
    private val tagAdapter by lazy { moshi.adapter(AtProtocolService.Feature.Tag::class.java) }

    override fun fromJson(reader: JsonReader): AtProtocolService.Feature? {
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
            "app.bsky.richtext.facet#mention" -> mentionAdapter.fromJson(reader)
            "app.bsky.richtext.facet#link" -> linkAdapter.fromJson(reader)
            "app.bsky.richtext.facet#tag" -> tagAdapter.fromJson(reader)
            else -> throw JsonDataException("Unknown feature type: $type")
        }
        
        reader.endObject()
        return result
    }

    override fun toJson(writer: JsonWriter, value: AtProtocolService.Feature?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginObject()
        when (value) {
            is AtProtocolService.Feature.Mention -> {
                writer.name("\$type").value("app.bsky.richtext.facet#mention")
                writer.name("did").value(value.did)
            }
            is AtProtocolService.Feature.Link -> {
                writer.name("\$type").value("app.bsky.richtext.facet#link")
                writer.name("uri").value(value.uri)
            }
            is AtProtocolService.Feature.Tag -> {
                writer.name("\$type").value("app.bsky.richtext.facet#tag")
                writer.name("tag").value(value.tag)
            }
        }
        writer.endObject()
    }

    companion object {
        val FACTORY = object : JsonAdapter.Factory {
            override fun create(
                type: Type,
                annotations: Set<Annotation>,
                moshi: Moshi
            ): JsonAdapter<*>? {
                return if (type === AtProtocolService.Feature::class.java) {
                    FeatureJsonAdapter(moshi)
                } else {
                    null
                }
            }
        }
    }
} 