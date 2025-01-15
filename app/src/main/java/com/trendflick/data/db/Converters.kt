package com.trendflick.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject

@ProvidedTypeConverter
class Converters @Inject constructor(private val moshi: Moshi) {
    private val mapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        Any::class.java
    )
    private val mapAdapter = moshi.adapter<Map<String, Any>>(mapType)

    @TypeConverter
    fun mapToString(value: Map<String, Any>?): String {
        return value?.let { mapAdapter.toJson(it) } ?: ""
    }

    @TypeConverter
    fun stringToMap(value: String): Map<String, Any>? {
        return if (value.isNotEmpty()) mapAdapter.fromJson(value) else null
    }

    @TypeConverter
    fun listToString(value: List<String>?): String {
        val listType = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(listType)
        return value?.let { adapter.toJson(it) } ?: ""
    }

    @TypeConverter
    fun stringToList(value: String): List<String>? {
        val listType = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(listType)
        return if (value.isNotEmpty()) adapter.fromJson(value) else null
    }
} 