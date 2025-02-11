package com.trendflick.data.model

data class Author(
    val did: String,
    val handle: String,
    val displayName: String,
    val avatarUrl: String
) {
    // Add a toString() method to help with string conversions
    override fun toString(): String = "@$handle"
} 