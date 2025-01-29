package com.trendflick.data.model

data class UserSearchResult(
    val did: String,
    val handle: String,
    val displayName: String? = null,
    val avatar: String? = null
) 