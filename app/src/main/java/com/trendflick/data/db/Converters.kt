package com.trendflick.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject

@ProvidedTypeConverter
class Converters @Inject constructor(private val moshi: Moshi) {
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    
    private val mapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        Any::class.java
    )
    private val mapAdapter = moshi.adapter<Map<String, Any>>(mapType)
    
    private val mapListType = Types.newParameterizedType(
        List::class.java,
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )
    private val mapListAdapter = moshi.adapter<List<Map<String, Any>>>(mapListType)

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return stringListAdapter.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return stringListAdapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromMap(value: Map<String, Any>): String {
        return mapAdapter.toJson(value)
    }

    @TypeConverter
    fun toMap(value: String): Map<String, Any> {
        return mapAdapter.fromJson(value) ?: emptyMap()
    }

    @TypeConverter
    fun fromMapList(value: List<Map<String, Any>>): String {
        return mapListAdapter.toJson(value)
    }

    @TypeConverter
    fun toMapList(value: String): List<Map<String, Any>> {
        return mapListAdapter.fromJson(value) ?: emptyList()
    }
} 