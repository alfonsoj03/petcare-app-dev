package com.example.mascotasapp.core

object JsonUtils {
    // naive JSON string escaper for simple demo
    fun q(v: String): String = buildString {
        append('"')
        v.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
        append('"')
    }
}
