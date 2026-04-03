package org.example

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray

object Utils {
    fun readJsonResource(resourceName: String): JsonArray? {
        return try {
            val jsonString = Utils::class.java.classLoader
                .getResourceAsStream(resourceName)
                ?.bufferedReader()
                ?.readText()
                ?: return null
            Json.parseToJsonElement(jsonString).jsonArray
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}