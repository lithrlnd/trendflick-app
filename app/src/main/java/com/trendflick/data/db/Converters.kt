package com.trendflick.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trendflick.data.model.Video
import com.trendflick.data.api.Facet
import com.trendflick.data.api.FacetFeature
import java.time.Instant

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromVideoList(list: List<Video>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toVideoList(value: String?): List<Video> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<Video>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): String? {
        return instant?.toString()
    }

    @TypeConverter
    fun toInstant(value: String?): Instant? {
        return value?.let { Instant.parse(it) }
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value == null) return emptyMap()
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromFacetList(facets: List<Facet>?): String? {
        if (facets == null) return null
        return gson.toJson(facets)
    }

    @TypeConverter
    fun toFacetList(value: String?): List<Facet>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Facet>>() {}.type
        return gson.fromJson(value, listType)
    }
} 