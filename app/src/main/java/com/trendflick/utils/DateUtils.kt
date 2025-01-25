package com.trendflick.utils

import java.time.Instant
import java.time.temporal.ChronoUnit

object DateUtils {
    fun formatTimestamp(timestamp: String): String {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m"
            minutes < 1440 -> "${minutes / 60}h"
            minutes < 43200 -> "${minutes / 1440}d"
            else -> "${minutes / 43200}mo"
        }
    }
} 