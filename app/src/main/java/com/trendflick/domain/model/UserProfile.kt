package com.trendflick.domain.model

data class UserProfile(
    val did: String,
    val handle: String,
    val displayName: String? = null,
    val avatar: String? = null,
    val description: String? = null
) 