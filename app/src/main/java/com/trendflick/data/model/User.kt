package com.trendflick.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.trendflick.data.db.Converters

@Entity(tableName = "users")
@TypeConverters(Converters::class)
data class User(
    @PrimaryKey
    val did: String,
    val handle: String,
    val displayName: String? = null,
    val description: String? = null,
    val avatar: String? = null,
    val accessJwt: String,
    val refreshJwt: String,
    val appPassword: String? = null,
    val preferences: Map<String, String> = emptyMap()
) 