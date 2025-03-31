package com.trendflick.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for date and time formatting in the app
 */
object DateUtils {

    /**
     * Format timestamp for display in posts and comments
     * Returns relative time (e.g., "2m", "5h", "3d") for recent timestamps
     * Returns formatted date (e.g., "Mar 15") for older timestamps
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "just now"
            diff < 3600000 -> "${diff / 60000}m"
            diff < 86400000 -> "${diff / 3600000}h"
            diff < 604800000 -> "${diff / 86400000}d"
            else -> {
                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
    
    /**
     * Format timestamp for display in full date format
     * Returns formatted date and time (e.g., "Mar 15, 2025 at 2:30 PM")
     */
    fun formatFullDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Format count for display (e.g., 1.2K, 3.4M)
     */
    fun formatCount(count: Int): String {
        return when {
            count < 1000 -> count.toString()
            count < 1000000 -> String.format("%.1fK", count / 1000f).replace(".0K", "K")
            else -> String.format("%.1fM", count / 1000000f).replace(".0M", "M")
        }
    }
}
