package com.trendflick.utils

object FirebaseUtils {
    /**
     * Encodes an AT Protocol URI to be Firebase-safe
     * Replaces invalid characters with safe alternatives
     */
    fun encodeAtUriForFirebase(atUri: String): String {
        return atUri
            .replace("/", "_")
            .replace(".", "-")
            .replace(":", "--")
            .replace("#", "---")
            .replace("$", "----")
            .replace("[", "_lb_")
            .replace("]", "_rb_")
    }

    /**
     * Decodes a Firebase-safe path back to an AT Protocol URI
     */
    fun decodeFirebasePathToAtUri(path: String): String {
        return path
            .replace("----", "$")
            .replace("---", "#")
            .replace("--", ":")
            .replace("-", ".")
            .replace("_lb_", "[")
            .replace("_rb_", "]")
            .replace("_", "/")
    }
} 