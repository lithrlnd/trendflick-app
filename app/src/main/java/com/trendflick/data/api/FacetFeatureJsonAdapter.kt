package com.trendflick.data.api

import com.squareup.moshi.*
import java.lang.reflect.Type

class FacetFeatureJsonAdapter(
    private val moshi: Moshi
) : JsonAdapter<FacetFeature>() {
    
    private val options: JsonReader.Options = JsonReader.Options.of("\$type")
    
    // Adapters for each concrete type
    private val mentionAdapter by lazy { moshi.adapter(MentionFeature::class.java) }
    private val linkAdapter by lazy { moshi.adapter(LinkFeature::class.java) }
    private val tagAdapter by lazy { moshi.adapter(TagFeature::class.java) }

    override fun fromJson(reader: JsonReader): FacetFeature? {
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
            else -> throw JsonDataException("Unknown type: $type")
        }
        
        reader.endObject()
        return result
    }

    override fun toJson(writer: JsonWriter, value: FacetFeature?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        when (value) {
            is MentionFeature -> mentionAdapter.toJson(writer, value)
            is LinkFeature -> linkAdapter.toJson(writer, value)
            is TagFeature -> tagAdapter.toJson(writer, value)
            else -> throw JsonDataException("Unknown FacetFeature type")
        }
    }

    companion object {
        val FACTORY = object : JsonAdapter.Factory {
            override fun create(
                type: Type,
                annotations: Set<Annotation>,
                moshi: Moshi
            ): JsonAdapter<*>? {
                return if (type === FacetFeature::class.java) {
                    FacetFeatureJsonAdapter(moshi)
                } else {
                    null
                }
            }
        }
    }
} 
