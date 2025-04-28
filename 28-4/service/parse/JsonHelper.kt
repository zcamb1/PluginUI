package com.example.rulemaker.service.parser

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Utility class for JSON operations using Gson auto-mapping
 */
object JsonHelper {
    val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Read and parse JSON file into an object of type T
     */
    inline fun <reified T> readJson(filePath: String): T {
        val json = File(filePath).readText(Charsets.UTF_8)
        return gson.fromJson(json, T::class.java)
    }

    /**
     * Read and parse JSON string into an object of type T
     */
    inline fun <reified T> fromJson(json: String): T {
        return gson.fromJson(json, T::class.java)
    }

    /**
     * Convert object to JSON string
     */
    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

    /**
     * Write object as JSON to a file
     */
    fun writeJson(filePath: String, obj: Any) {
        val json = gson.toJson(obj)
        File(filePath).writeText(json, Charsets.UTF_8)
    }
} 